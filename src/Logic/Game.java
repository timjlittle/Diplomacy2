/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Logic;

import Data.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that implements the main game. Holds the lists of the various
 * components including players, regions, borders, units and borders
 *
 * @author Tim Little
 */
public class Game {

    private Map<Integer, Player> allPlayers = new HashMap<>();
    private Map<String, Region> allRegions = new HashMap<>();
    private Map<Integer, Border> allBorders = new HashMap<>();
    private Map<Integer, Unit> allUnits = new HashMap<>();
    private Map<Integer, Order> allOrders = new HashMap<>();
    private Props props;
    private LinkedList<Border> coastsList = new LinkedList<>();
    private String resolveLog;

    /**
     * Constructor. Reads the current game details from disk
     *
     * @throws DataAccessException
     */
    public Game() throws DataAccessException {

        resolveLog = "";

        loadGame();

    }

    private void loadGame() throws DataAccessException {
        try {
            props = new Props();

            //NB MUST be in this order as there are dependencies
            loadPlayers();
            loadRegions();
            loadBorders();
            loadUnits();

            //This needs props to know the current turn
            loadCurrentOrders();

            //Default units without an order to hold (getCurrent order does this)
            for (Map.Entry m : allUnits.entrySet()) {
                Unit u = (Unit) m.getValue();

                u.getCurrentOrder();
            }

        } catch (IOException ex) {
            Logger.getLogger(Game.class.getName()).log(Level.SEVERE, null, ex);
            DataAccessException de = new DataAccessException("Unable to find properties file", 1, ex.getMessage());

            throw de;
        }
    }

    /**
     * Reads the details of the players from disk
     *
     * @throws DataAccessException
     */
    private void loadPlayers() throws DataAccessException {
        DataAccessor db = new DataAccessor();
        Record requestedFields = new Record();
        ArrayList<Record> returnedFields;

        requestedFields.addField("PlayerId", 0);
        requestedFields.addField("PlayerName", "");
        requestedFields.addField("Colour", "");

        returnedFields = db.readAllRecords("Player", requestedFields, null);

        if (db.getErrNo() == 0) {
            for (Record record : returnedFields) {
                int playerId = record.getIntVal("PlayerId");
                String playerName = record.getStringVal("PlayerName");
                String colour = record.getStringVal("Colour");

                Player p = new Player(playerName, playerId, colour);

                Record homeRegionFields = new Record();
                Record selectedCountry = new Record();
                ArrayList<Record> homeRegions;

                homeRegionFields.addField("RegionCode", "");
                selectedCountry.addField("PlayerId", playerId);

                homeRegions = db.readAllRecords("HomeRegions", homeRegionFields, selectedCountry);

                for (Record regRec : homeRegions) {
                    String reg = regRec.getStringVal("RegionCode");
                    p.addHomeRegionCode(reg);
                }

                allPlayers.put(playerId, p);
            }

        } else {
            DataAccessException ex = new DataAccessException("loadPlayers: Error reading players", db.getErrNo(), db.getErrorMsg());

            throw ex;
        }
    }

    /**
     * loadRegions: data loader
     *
     * @throws DataAccessException
     */
    private void loadRegions() throws DataAccessException {
        DataAccessor db = new DataAccessor();
        Record requestedFields = new Record();
        ArrayList<Record> returnedFields;

        requestedFields.addField("RegionCode", "");
        requestedFields.addField("RegionName", "");
        requestedFields.addField("SupplyCenter", false);
        requestedFields.addField("CurOwner", -1);
        requestedFields.addField("BounceTurn", -1);
        requestedFields.addField("OriOwner", -1);

        returnedFields = db.readAllRecords("Region", requestedFields, null);

        if (db.getErrNo() == 0) {
            for (Record record : returnedFields) {
                String regionCode = record.getStringVal("RegionCode");
                String regionName = record.getStringVal("RegionName");
                boolean supplyCenter = record.getBoolVal("SupplyCenter");
                int ownerId = record.getIntVal("CurOwner");
                int standoffTurn = record.getIntVal("BounceTurn");
                int oriOwner = record.getIntVal("OriOwner");

                if (standoffTurn != props.getTurn()) {
                    standoffTurn = -1;
                }

                Region r = new Region(regionName, regionCode, supplyCenter, standoffTurn, oriOwner);

                allRegions.put(regionCode, r);

                //
                if (ownerId > 0 && supplyCenter) {
                    r.setOwnerId(ownerId);
                    Player p = allPlayers.get(ownerId);
                    p.addSupplyCenter(r);
                }
            }
        } else {
            DataAccessException ex = new DataAccessException("loadRegions: Error reading regions", db.getErrNo(), db.getErrorMsg());

            throw ex;
        }
    }

    /**
     * Creates the list of all borders. NB Assumes that the borders have been
     * loaded.
     *
     * @throws DataAccessException
     */
    private void loadBorders() throws DataAccessException {
        DataAccessor db = new DataAccessor();
        Record requestedFields = new Record();
        ArrayList<Record> returnedFields;
        Record where = new Record();

        requestedFields.addField("BorderId", -1);
        requestedFields.addField("RegionCode", "");
        requestedFields.addField("Type", "");
        requestedFields.addField("BorderName", "");
        requestedFields.addField("mapX", 0);
        requestedFields.addField("mapY", 0);

        returnedFields = db.readAllRecords("Border", requestedFields, null);

        if (db.getErrNo() == 0) {
            for (Record record : returnedFields) {
                int borderId = record.getIntVal("BorderId");
                String regionCode = record.getStringVal("RegionCode");
                String type = record.getStringVal("Type");
                String borderName = record.getStringVal("BorderName");
                int x = record.getIntVal("mapX");
                int y = record.getIntVal("mapY");

                Region owner = allRegions.get(regionCode);

                Border b = new Border(borderId, owner, Border.mapStringToType(type), borderName, x, y);
                allBorders.put(borderId, b);
                owner.addBorder(b);

                if (b.getType() == Border.BorderType.COAST) {
                    coastsList.add(b);
                }

            }

            Collections.sort(coastsList);

            //Create the list of neighbours for each border
            for (Map.Entry m : allBorders.entrySet()) {
                requestedFields.clear();
                requestedFields.addField("Boarder2", 0);

                Border b1 = (Border) m.getValue();

                where.addField("Boarder1", b1.getBorderId());

                returnedFields = db.readAllRecords("Adjacent", requestedFields, where);

                for (Record record : returnedFields) {
                    int borderId = record.getIntVal("Boarder2");

                    Border b2 = allBorders.get(borderId);

                    b1.addNeighbour(b2);
                }
            }
        } else {
            DataAccessException ex = new DataAccessException("loadBorders: Error reading borders", db.getErrNo(), db.getErrorMsg());

            throw ex;
        }

    }

    /**
     * loadUnits: data loader
     *
     * @throws DataAccessException
     */
    private void loadUnits() throws DataAccessException {
        DataAccessor db = new DataAccessor();
        Record requestedFields = new Record();
        ArrayList<Record> returnedFields;

        requestedFields.addField("UnitId", 0);
        requestedFields.addField("UnitType", "");
        requestedFields.addField("PlayerId", 0);
        requestedFields.addField("Occupies", 0);
        requestedFields.addField("VictorOrigin", 0);

        returnedFields = db.readAllRecords("Unit", requestedFields, null);

        if (db.getErrNo() == 0) {
            for (Record record : returnedFields) {
                int unitId = record.getIntVal("UnitId");
                String unitType = record.getStringVal("UnitType");
                int playerId = record.getIntVal("PlayerId");
                int occupies = record.getIntVal("Occupies");
                Border beatenBy = null;

                int victorBorderId = record.getIntVal("VictorOrigin");

                if (victorBorderId != -1) {
                    beatenBy = allBorders.get(victorBorderId);
                }

                Border pos = allBorders.get(occupies);

                Unit u = new Unit(unitId, Unit.mapStringoUnitCode(unitType), pos, playerId, false, beatenBy);
                pos.setOccupyingUnit(u);

                if (victorBorderId > 0) {
                    Border victorBorder = allBorders.get(victorBorderId);

                    u.setVictorOrigin(victorBorder);
                }

                allUnits.put(unitId, u);

                Player p = allPlayers.get(playerId);

                p.addUnit(u);

            }
        } else {
            DataAccessException ex = new DataAccessException("loadBorders: Error reading borders", db.getErrNo(), db.getErrorMsg());

            throw ex;
        }
    }

    /**
     * loadCurrentOrders: data loader
     *
     */
    private void loadCurrentOrders() {
        DataAccessor db = new DataAccessor();
        Record requestedFields = new Record();
        Record whereFields = new Record();
        ArrayList<Record> returnedFields;

        requestedFields.addField("OrderId", 0);
        requestedFields.addField("Type", "");
        requestedFields.addField("Origin", 0);
        requestedFields.addField("Destination", 0);
        requestedFields.addField("UnitId", 0);
        requestedFields.addField("BeingConvoyed", false);
        requestedFields.addField("State", 0);

        whereFields.addField("Round", props.getTurn());
        whereFields.addField("State", 0);

        returnedFields = db.readAllRecords("Command", requestedFields, whereFields);

        if (db.getErrNo() == 0) {
            for (Record record : returnedFields) {
                int orderId = record.getIntVal("OrderId");
                String type = record.getStringVal("Type");
                int originId = record.getIntVal("Origin");
                int destinationId = record.getIntVal("Destination");
                int unitId = record.getIntVal("UnitId");
                boolean beingConvoyed = record.getBoolVal("BeingConvoyed");
                int state = record.getIntVal("State");

                Border origin = null;
                Border dest = null;
                Unit unit = allUnits.get(unitId);

                if (originId > 0) {
                    origin = allBorders.get(originId);

                }

                if (destinationId > 0) {
                    dest = allBorders.get(destinationId);
                }

                Order o = new Order(orderId, Order.mapCommandTypeFromString(type), dest, origin, beingConvoyed, unit, props.getTurn(), state);

                unit.setCurrentOrder(o);
                allOrders.put(orderId, o);
            }
        }

    }

    /**
     *
     * @return list of players
     */
    public Map<Integer, Player> getAllPlayers() {
        return allPlayers;
    }

    /**
     *
     * @return list of regions
     */
    public Map<String, Region> getAllRegions() {
        return allRegions;
    }

    /**
     * list of borders
     *
     * @return
     */
    public Map<Integer, Border> getAllBorders() {
        return allBorders;
    }

    /**
     * list of units
     *
     * @return
     */
    public Map<Integer, Unit> getAllUnits() {
        return allUnits;
    }

    /**
     * List of orders
     *
     * @return
     */
    public Map<Integer, Order> getAllOrders() {
        return allOrders;
    }

    /**
     * list of coasts
     *
     * @return
     */
    public LinkedList<Border> getCoastsList() {
        return coastsList;
    }

    /**
     * Construct the form title
     *
     * @return
     */
    public String getTitle() {
        String ret = "";
        String season = "Spring";
        int year;
        String phase = "";

        switch (props.getTurn() % 3) {
            case 1:
                season = "Spring";
                break;
            case 2:
                season = "Fall";
                break;

            case 0:
                season = "Winter";
                break;

        }

        year = 1901 + props.getTurn() / 3;

        switch (props.getPhase()) {
            case ORDER:
                phase = "create orders phase";
                break;

            case RETREAT:
                phase = "retreat phase";
                break;

            case BUILD:
                phase = "build phase";
                break;
        }

        ret = season + " " + year + " " + phase;
        return ret;
    }

    public Props.Phase getGamePhase() {
        return props.getPhase();
    }

    /**
     *
     * @param targetCode
     * @return
     */
    private LinkedList<Order> findOrdersTargettingRegion(String targetCode) {
        LinkedList<Order> ret = new LinkedList<>();
        for (Map.Entry m : allOrders.entrySet()) {
            Order o = (Order) m.getValue();

            String regCode = o.getDest().getRegion().getRegionCode();

            if (regCode.compareTo(targetCode) == 0 && o.getState() != Order.ORDER_STATE.FAILED) {
                ret.add(o);
            }
        }

        return ret;
    }

    /**
     *
     * @param fromId
     * @param toId
     * @return
     */
    private Order findAuxOrder(int fromId, int toId) {
        Order ret = null;

        for (Map.Entry m : allOrders.entrySet()) {
            Order o = (Order) m.getValue();

            if (o.getDestinationId() == toId && o.getOriginId() == fromId) {
                ret = o;
                break;
            }
        }

        return ret;
    }

    /**
     * Checks that a particular cut or convoy order has been cut.
     *
     * @param order
     * @return
     */
    private boolean checkForCuts(Order order) {
        boolean cut = false;

        int unitId = order.getUnitId();
        Unit unit = allUnits.get(unitId);

        //Ignore orders that can't be cut
        if (order.getCommand() == Order.OrderType.CONVOY || order.getCommand() == Order.OrderType.SUPPORT) {

            //for (Order o : findOrdersTargettingRegion(order.getDest().getRegion().getRegionCode())){
            for (Order o : findOrdersTargettingRegion(unit.getPosition().getRegion().getRegionCode())) {

                //Ignore the original order and any orders supporting/convoying this order
                if (o.getOrderId() != order.getOrderId() && o.getState() != Order.ORDER_STATE.FAILED) {
                    //check that the cutting order hasn't been cut.
                    if (!checkForCuts(o)) {
                        cut = true;
                        //order.setState(Order.ORDER_STATE.FAILED);
                    }

                }
            }
        }

        return cut;
    }

    private void calculateSupport() {
        LinkedList<Order> moveList = new LinkedList<>();
        LinkedList<Order> supportList = new LinkedList<>();
        GameLogger g = new GameLogger();

        g.logMessage("Counting support");

        //Collect all the uncut support orders and all the move orders
        for (Map.Entry m : allOrders.entrySet()) {
            Order order = (Order) m.getValue();

            if (order.getCommand() == Order.OrderType.SUPPORT) {
                if (!checkForCuts(order)) {
                    supportList.add(order);
                } else {
                    g.logMessage(order.toString() + " cut (reset to HOLD");
                    order.setCommand(Order.OrderType.HOLD);
                    Unit unit = allUnits.get(order.getUnitId());

                    order.setDest(unit.getPosition());
                    order.setOrigin(unit.getPosition());

                }
            }

            if (order.getCommand() == Order.OrderType.MOVE) {
                moveList.add(order);
            }

        }

        g.logMessage("Finised checking supports for cuts");
        g.logMessage("Counting supports");

        for (Order move : moveList) {
            for (Order support : supportList) {
                if (support.getDestinationId() == move.getDestinationId() && support.getOriginId() == move.getOriginId()) {
                    move.incrementSupportCount();
                    g.logMessage(support.toString() + " support added");
                }
            }
        }
        g.logMessage("Finished counting support, setting support orders to HOLD");

        for (Order support : supportList) {
            support.setCommand(Order.OrderType.HOLD);
            Unit unit = allUnits.get(support.getUnitId());

            support.setDest(unit.getPosition());
            support.setOrigin(unit.getPosition());
        }
    }

    /**
     *
     * @param regionCode
     * @return The order for the specified region
     */
    private Order getOrderForRegion(String regionCode) {
        Order ret = null;
        Unit unit;

        for (Map.Entry m : allUnits.entrySet()) {
            unit = (Unit) m.getValue();
            if (regionCode.equalsIgnoreCase(unit.getPosition().getRegion().getRegionCode())) {
                ret = unit.getCurrentOrder();
                break;
            }
        }

        return ret;
    }

    /**
     * Ensure all units have at least a HOLD order
     */
    private void createMissingOrders() {

        for (Map.Entry m : allUnits.entrySet()) {
            Unit unit = (Unit) m.getValue();

            Order order = unit.getCurrentOrder();

            if (order == null) {
                order = new Order(-1, Order.OrderType.HOLD, unit.getPosition(), unit.getPosition(), false, unit, props.getTurn(), 0);
                unit.setCurrentOrder(order);
                allOrders.put(order.getOrderId(), order);
            }

            if (order.getTurn() != props.getTurn()) {
                order.setCommand(Order.OrderType.HOLD);
                order.setDest(unit.getPosition());
                order.setOrigin(unit.getPosition());
                order.setState(Order.ORDER_STATE.UNSEEN);
                order.setTurn(props.getTurn());
            }

            order.resetSupportCount();
        }

    }

    /**
     * As it says on the tin, checks move orders that are being convoyed. If
     * there is a continuous list of convoy orders from the origin to the
     * destination the order remains, otherwise it is changed to a hold.
     *
     * All convoy orders changed to HOLD at the end
     */
    private void checkConvoys() {
        LinkedList<Order> convoyList = new LinkedList<>();
        LinkedList<Order> beingConvoyedList = new LinkedList<>();
        GameLogger g = new GameLogger();

        //Check for relevant orders
        for (Map.Entry m : allOrders.entrySet()) {
            Order order = (Order) m.getValue();

            //      Put all convoy orders into a list     
            if (order.getCommand() == Order.OrderType.CONVOY) {
                // If a convoy order is cut change it to a HOLD order
                if (checkForCuts(order)) {
                    g.logMessage(order.toString() + " cut (reset to HOLD");
                    order.convertToHold();
                } else {
                    convoyList.add(order);
                }
            }

            //  Put all move orders that are being convoyed into a list
            if (order.getCommand() == Order.OrderType.MOVE && order.beingConvoyed) {
                // If a move order is cut change it to a HOLD order
                if (checkForCuts(order)) {
                    g.logMessage(order.toString() + " cut (reset to HOLD)");
                    order.convertToHold();
                } else {
                    beingConvoyedList.add(order);
                }
            }
        }

        //  While convoy list is not empty
        //  Get the next move order and set as current order
        for (Order moveOrder : beingConvoyedList) {
            boolean stillLooking = true;

            //  Note final destination
            Border dest = moveOrder.getDest();
            Border curPos = moveOrder.getOrigin();

            //Do until order not found or current convey order is adjacent to the destination
            do {
                // Find convoy order for current move order that is adjacent to current convoy order
                Iterator search = convoyList.iterator();
                boolean foundNext = false;
                Order tempOrder;
                while (search.hasNext() && !foundNext && stillLooking) {
                    tempOrder = (Order) search.next();

                    //Make sure the order applies to the current move
                    if (tempOrder.getDestinationId() == moveOrder.getDestinationId() && tempOrder.getOriginId() == moveOrder.getOriginId()) {
                        if (curPos.isNeighbour(tempOrder.getUnitPos())) {
                            foundNext = true;

                            //set as current convoy
                            curPos = tempOrder.getUnitPos();

                            //Check if reached the end
                            if (curPos.isNeighbour(dest)) {
                                stillLooking = false;
                            }
                        }
                    }
                }

                //If we didn't find a complete route change the MOVE order to HOLD
                if (!foundNext) {
                    stillLooking = false;
                    g.logMessage(moveOrder.toString() + " convoy failed, converted to HOLD");
                    moveOrder.convertToHold();

                }

            } while (stillLooking);

        } //      End while

        //Change all convoy orders to hold orders
        for (Order convoyOrder : convoyList) {
            convoyOrder.convertToHold();
        }

    }

    /**
     * Goes through the current order list calculating the final positions for
     * each piece.
     *
     * @throws Data.DataAccessException
     */
    public void resolveAllOrders() throws DataAccessException {
        LinkedList<Order> unresolved = new LinkedList<>();
        Order curOrder;
        GameLogger g = new GameLogger();

        try {
            g.logMessage("");
            String logStr = "Resolving all orders for " + getTitle();
            g.logMessage(logStr);
            int len = logStr.length();
            logStr = "";
            for (int counter = 0; counter < len; counter++) {
                logStr += "=";
            }
            g.logMessage(logStr);

            //Make sure every unit has an order
            createMissingOrders();

            g.logMessage("Current Orders:");

            //Identify cut support and convoys orders and set them to HOLD orders.
            checkConvoys();

            //Count non failed support orders for all Move and Hold orders.
            //Change remaining support orders to holds
            calculateSupport();

            //Copy all orders to unresolved orders list.
            for (Map.Entry m : allOrders.entrySet()) {
                Order o = (Order) m.getValue();

                if (o.getState() != Order.ORDER_STATE.FAILED && o.getState() != Order.ORDER_STATE.SUCCEEDED) {
                    Unit u = o.getOrigin().getOccupyingUnit();
                    //Unresolved so reset the victor identifier
                    if (u != null) {
                        u.setVictorOrigin(null);
                        u.save();
                    }
                    unresolved.add(o);
                    g.logMessage(o.toString());
                }
            }

            //Sort unresolved list by destination then support count
            unresolved.sort(null);

            Iterator mainIterator = unresolved.iterator();

            //While the unresolved list isn’t empty:
            while (!unresolved.isEmpty()) {

                boolean skip = false;

                //    Get the next Order.
                if (mainIterator != null && mainIterator.hasNext()) {
                    curOrder = (Order) mainIterator.next();
                } else {
                    mainIterator = unresolved.iterator();
                    curOrder = (Order) mainIterator.next();
                }

                Region dest = curOrder.getDestRegion();
                if (curOrder.getCommand() != Order.OrderType.HOLD) {
                    //    If there is a unit occupying the destination and that unit has a MOVE order

                    if (dest.isOccupied()) {
                        Order destOrder = getOrderForRegion(dest.getRegionCode());
                        if (destOrder != null) { //Should never be null but sometime seems to happen
                            //        Leave the order as pending
                            if (destOrder.getCommand() == Order.OrderType.MOVE && destOrder.getDest().getBorderId() != curOrder.getUnitPos().getBorderId()) {
                                skip = true;
                                g.logMessage(curOrder.toString() + " pending ...");
                            }
                        } else {
                            g.logMessage("Occuped = true but order = null; " + curOrder.toString());
                        }

                    }
                }

                if (!skip) {
                    //find orders targeting the destination
                    LinkedList<Order> sharedDestOrders = findOrdersTargettingRegion(dest.getRegionCode());

                    //    If there is only this one
                    if (sharedDestOrders.size() == 1) {
                        //       mark the order as successful and update the unit’s position etc.
                        int curUnitId = curOrder.getUnitId();
                        Unit curUnit = allUnits.get(curUnitId);
                        g.logMessage(curOrder.toString() + " succeeded");
                        curUnit.setPosition(curOrder.getDest());
                        curUnit.save();
                        curOrder.setState(Order.ORDER_STATE.SUCCEEDED);
                        curOrder.save();
                        //       Remove order from unresolved list
                        unresolved.remove(curOrder);
                        mainIterator = unresolved.iterator();

                    } else {

                        sharedDestOrders.sort(null);

                        Iterator destIterator = sharedDestOrders.iterator();
                        Order top = (Order) destIterator.next();
                        Order next = (Order) destIterator.next();
                        g.logMessage(top.toString() + ": Support = " + top.getSupportCount());
                        g.logMessage(next.toString() + ": Support = " + next.getSupportCount());

                        if (top.getSupportCount() > next.getSupportCount()) {
                            //            Mark highest as successful and update unit details
                            //            Mark highest as successful and update unit details
                            Unit winner = (Unit) allUnits.get(top.getUnitId());
                            top.setState(Order.ORDER_STATE.SUCCEEDED);
                            top.save();

                            //            Remove highest from unresolved list
                            unresolved.remove(top);
                            g.logMessage(top.toString() + " succeeded");
                            mainIterator = unresolved.iterator();
                            winner.setPosition(top.getDest());
                            winner.save();

                            do {
                                if (next.getCommand() == Order.OrderType.MOVE) {
                                    g.logMessage(next.toString() + " failed, changed to HOLD");
                                    next.setCommand(Order.OrderType.HOLD);
                                    next.setDest(next.getUnitPos());
                                    next.save();
                                } else {
                                    //Mark as failed and make sure it can't retreat to the space left empty by the winner.
                                    next.setState(Order.ORDER_STATE.FAILED);
                                    Unit losingUnit = next.getOrigin().getOccupyingUnit();
                                    losingUnit.setVictorOrigin(top.getOrigin());
                                    losingUnit.save();
                                    next.setRegionBeatenFrom(top.getOrigin().getRegion().getRegionCode());
                                    unresolved.remove(next);
                                    mainIterator = unresolved.iterator();
                                    g.logMessage(next.toString() + " failed, needs to retreat");
                                    next.save();
                                }

                                if (destIterator.hasNext()) {
                                    next = (Order) destIterator.next();
                                }
                            } while (destIterator.hasNext());

                        } else {
                            //More than one has the same amount of support
                            LinkedList<Order> standoffs = new LinkedList<>();

                            next.getDestRegion().setStandoff();

                            standoffs.add(next);
                            standoffs.add(top);

                            do {
                                if (destIterator.hasNext()) {
                                    next = (Order) destIterator.next();

                                    if (next.getSupportCount() == top.getSupportCount()) {
                                        standoffs.add(next);
                                    }
                                }
                            } while (next.getSupportCount() == top.getSupportCount() && destIterator.hasNext());

                            Iterator matchesIterator = standoffs.iterator();

                            while (matchesIterator.hasNext()) {
                                Order match = (Order) matchesIterator.next();

                                if (match.getCommand() == Order.OrderType.HOLD) {
                                    match.setState(Order.ORDER_STATE.SUCCEEDED);
                                    unresolved.remove(match);
                                    match.save();
                                    g.logMessage(match.toString() + " succeeded");
                                    mainIterator = unresolved.iterator();
                                } else {
                                    g.logMessage(match.toString() + " failed, changed to HOLD");
                                    match.setCommand(Order.OrderType.HOLD);
                                    match.setDest(match.getUnitPos());
                                    match.save();
                                }

                            }

                            if (next.getSupportCount() != top.getSupportCount()) {
                                g.logMessage(next.toString() + " failed");
                                next.setState(Order.ORDER_STATE.FAILED);
                                next.save();
                                unresolved.remove(next);
                                mainIterator = unresolved.iterator();
                            }

                            while (destIterator.hasNext()) {
                                next = (Order) destIterator.next();
                                g.logMessage(next.toString() + " failed");
                                next.setState(Order.ORDER_STATE.FAILED);
                                next.save();
                                unresolved.remove(next);
                                mainIterator = unresolved.iterator();
                            }

                        }
                    }
                    //End while
                }
            }
        } catch (Exception e) {
            g.logMessage("Error: " + e.getMessage());
            throw e;
        }

        g.logMessage("Order resolution completed");
        g.logMessage("");

    }

    /**
     * Calculates which units need a retreat order
     *
     * @return LinkedList containing the units where their current order failed.
     */
    public LinkedList<Unit> getRetreatList() throws DataAccessException {
        LinkedList<Unit> retreatList = new LinkedList<>();
        LinkedList<Unit> disbandList = new LinkedList<>();
        GameLogger gameLog = new GameLogger();

        for (Map.Entry m : allUnits.entrySet()) {
            Unit currentUnit = (Unit) m.getValue();

            if ((currentUnit.getCurrentOrder().getState() == Order.ORDER_STATE.FAILED)
                    || ((currentUnit.getCurrentOrder().getCommand() == Order.OrderType.RETREAT) && (currentUnit.getCurrentOrder().getState() != Order.ORDER_STATE.SUCCEEDED))) {
                LinkedList<Border> retreats = currentUnit.getPossibleRetreats();

                if (retreats.isEmpty()) {
                    //If it has nowhere to go it will need to be disbanded
                    disbandList.add(currentUnit);
                    
                } else {
                    //Otherwise add it to the retreat list
                    retreatList.add(currentUnit);
                }
            }
        }
        
        gameLog.logMessage (retreatList.size() + " units need to retreat");
        
        Iterator disbandIterator = disbandList.iterator();
        while (disbandIterator.hasNext()){
            Unit disband = (Unit)disbandIterator.next();
            
            gameLog.logMessage(disband.toString() + " DISBANDED (no retreats available)");
            
            allOrders.remove(disband.getCurrentOrder().getOrderId());

            disband.delete();
            allUnits.remove(disband.getUnitId());
        }

        return retreatList;
    }

    /**
     * Calculates the next game phase. May Update both phase and turn
     *
     * @throws DataAccessException
     * @throws IOException
     */
    public void nextPhase() throws DataAccessException, IOException {

        //loadGame ();
        if (getRetreatList().isEmpty()) {
            if (props.getPhase() != Props.Phase.BUILD || !isBuildNeeded()) {

                //No retreeats so go straight to the next turn
                props.nextTurn();
                if (props.getTurn() % 3 == 0) {
                    changeSupplyPointOwnership();
                    //winter so build
                    if (isBuildNeeded()) {
                        props.setPhase(Props.Phase.BUILD);
                    } else {
                        //Skip build phase and go straight to 
                        props.nextTurn();
                        props.setPhase(Props.Phase.ORDER);
                    }
                } else {
                    //Go straight to build
                    props.setPhase(Props.Phase.ORDER);
                }
            }
        } else {
            props.setPhase(Props.Phase.RETREAT);
        }

    }

    /**
     * Sets all unit's orders to be HOLD
     *
     * @throws DataAccessException
     */
    public void setStartOfOrderPhase() throws DataAccessException {
        //Reset all units current orders to be holds for the current turn
        for (Map.Entry map : allUnits.entrySet()) {
            Unit unit = (Unit) map.getValue();

            unit.setVictorOrigin(null);

            Order order = unit.getCurrentOrder();

            order.setCommand(Order.OrderType.HOLD);
            order.setState(Order.ORDER_STATE.UNSEEN);
            order.setTurn(props.getTurn());
            order.setBeingConvoyed(false);
            order.setCut(false);
            order.setOrigin(unit.getPosition());
            order.setDest(unit.getPosition());

            if (!order.save()) {
                DataAccessException ex = new DataAccessException(order.getErrMsg(), order.getErrNo(), "Error saving order id " + order.getOrderId());

                throw ex;
            }
        }
    }

    public void restartGame() throws DataAccessException {
        DataAccessor db = new DataAccessor();
        Record fieldList = new Record();

        GameLogger logfile = new GameLogger();
        logfile.resetLog();

        logfile.logMessage("Game restarted");

        try {
            props.setTurn(1);
            props.setPhase(Props.Phase.ORDER);
            //logfile.logMessage("Phase set to 1");
        } catch (IOException ex) {
            Logger.getLogger(Game.class.getName()).log(Level.SEVERE, null, ex);
            DataAccessException e = new DataAccessException(ex.getMessage(), -1, "IO error resetting properties");
            throw e;
        }

        //Delete all existing units and orders
        //       for (Map.Entry map : allUnits.entrySet()) {
        //           Unit unit = (Unit) map.getValue();
        //           Order order = unit.getCurrentOrder();
//
//            order.delete();
//            unit.delete();
//            logfile.logMessage("Units and orders deleted");
//        }
        db.clearTable("Command");
        db.clearTable("unit");
        logfile.logMessage("Units and orders deleted");

        for (Map.Entry map : allPlayers.entrySet()) {
            Player p = (Player) map.getValue();

            p.getUnits().clear();
        }

        //Reset the supply centers to their original owners.
        for (Map.Entry map : allRegions.entrySet()) {
            Region sc = (Region) map.getValue();
            if (sc.isSupplyCenter()) {
                sc.resetOwner();
            }

        }

        allUnits.clear();
        allOrders.clear();

        //Read the start positions
        fieldList.addField("PlayerId", 0);
        fieldList.addField("RegionCode", "");
        fieldList.addField("StartPiece", "");
        fieldList.addField("StartBorder", 0);

        ArrayList<Record> results = db.readAllRecords("HomeRegions", fieldList, null);

        //For each start position create a unit and a hold order
        for (Record fields : results) {
            int playerId = fields.getIntVal("PlayerId");
            int borderId = fields.getIntVal("StartBorder");
            String typeString = fields.getStringVal("StartPiece");

            Border pos = allBorders.get(borderId);
            Player player = allPlayers.get(playerId);

            Unit newUnit = new Unit(Unit.mapStringoUnitCode(typeString), pos, playerId, false);

            if (!newUnit.save()) {
                DataAccessException ex = new DataAccessException(newUnit.getErrMsg(), newUnit.getErrNo(), "Error creating new unit");
                throw ex;
            }

            player.addUnit(newUnit);
            allUnits.put(newUnit.getUnitId(), newUnit);

            Order newOrder = new Order(-1, Order.OrderType.HOLD, pos, pos, false, newUnit, 1, 0);

            if (!newOrder.save()) {
                DataAccessException ex = new DataAccessException(newUnit.getErrMsg(), newUnit.getErrNo(), "Error creating new order");
                throw ex;
            }

            newUnit.setCurrentOrder(newOrder);
            allOrders.put(newOrder.getOrderId(), newOrder);
        }

        logfile.logMessage("Game reset completed");
    }

    /**
     * Looks at each unit in turn. If they are in a supply centre the ownership
     * of the supply centre is changed to the owner of the unit.
     *
     * @throws Data.DataAccessException
     */
    public void changeSupplyPointOwnership() throws DataAccessException {
        GameLogger logfile = new GameLogger();

        logfile.logMessage("Checking for changes of ownership ...");

        for (Map.Entry m : allUnits.entrySet()) {
            Unit u = (Unit) m.getValue();

            Region r = u.getPosition().getRegion();

            if (r.isSupplyCenter()) {

                int currOwner = r.getOwnerId();
                int newOwner = u.getOwnerId();

                if (currOwner != newOwner) {

                    Player p1 = allPlayers.get(currOwner);
                    Player p2 = allPlayers.get(newOwner);

                    logfile.logMessage(r.getRegionName() + " changed from " + p1.getPlayerName() + " to " + p2.getPlayerName());

                    p1.removeSupplyCenter(r);
                    p2.addSupplyCenter(r);

                    try {
                        r.setOwnerId(newOwner);
                    } catch (DataAccessException e) {
                        Logger.getLogger(Region.class.getName()).log(Level.SEVERE, null, e);
                        Data.DataAccessException ex = new Data.DataAccessException("Error updating supply center owner " + u.toString(), e.getErrNo(), e.getMessage());

                        throw (ex);
                    }
                }
            }
        }
        logfile.logMessage("Cownership checking finished.");
    }

    /**
     *
     * @return True if any players need to build or disband
     */
    public boolean isBuildNeeded() {
        boolean buildNeeded = false;

        for (Map.Entry m : allPlayers.entrySet()) {
            Player p = (Player) m.getValue();

            if (p.getBuildCount() != 0) {
                buildNeeded = true;
            }
        }

        return buildNeeded;
    }

    public Player getWinner() {
        Player ret = null;

        for (Map.Entry m : allPlayers.entrySet()) {

            Player p = (Player) m.getValue();

            if (p.getSupplyCenters().size() >= 34) {
                ret = p;
            }
        }

        return ret;
    }

}
