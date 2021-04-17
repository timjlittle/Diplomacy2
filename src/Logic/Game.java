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
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that implements the main game. Holds the lists of the various components
 * including players, regions, borders, units and borders
 * 
 * @author Tim Little
 */
public class Game {
    private Map <Integer, Player> allPlayers = new HashMap<>();
    private Map <String, Region> allRegions = new HashMap<>();
    private Map <Integer, Border> allBorders = new HashMap<>();
    private Map <Integer, Unit> allUnits = new HashMap<>();
    private Map <Integer, Order> allOrders = new HashMap<>();
    private Props props;
    private LinkedList <Border> coastsList = new LinkedList<>();
    
    
    public Game () throws DataAccessException {
              
        try {
            props = new Props ();

            //NB MUST be in this order as there are dependencies
            loadPlayers ();
            loadRegions ();
            loadBorders ();         
            loadUnits ();
            
            //This needs props to know the current turn
            loadCurrentOrders ();
            
            //Default units without an order to hold (getCurrent order does this)
            for (Map.Entry m: allUnits.entrySet()){
                Unit u = (Unit)m.getValue();
                
                u.getCurrentOrder();
            }
            
        } catch (IOException ex) {
            Logger.getLogger(Game.class.getName()).log(Level.SEVERE, null, ex);
            DataAccessException de = new DataAccessException ("Unable to find properties file", 1, ex.getMessage());
            
            throw de;
        }
        
    }
    
    private void loadPlayers () throws DataAccessException {
        DataAccessor db = new DataAccessor ();
        Record requestedFields = new Record();
        ArrayList <Record> returnedFields;

        requestedFields.addField("PlayerId", 0);
        requestedFields.addField("PlayerName", "");
        requestedFields.addField("Colour", "");
        
        returnedFields = db.readAllRecords("Player", requestedFields, null);

        if (db.getErrNo() == 0) {
            for (Record record: returnedFields){
                int playerId = record.getIntVal("PlayerId");
                String playerName = record.getStringVal("PlayerName");
                String colour = record.getStringVal("Colour");
                
                Player p = new Player (playerName, playerId, colour);
                
                allPlayers.put(playerId, p);
            }
            
        } else {
            DataAccessException ex = new DataAccessException ("loadPlayers: Error reading players", db.getErrNo(), db.getErrorMsg());
            
            throw ex;
        }


    }
    
    private void loadRegions () throws DataAccessException {
        DataAccessor db = new DataAccessor ();
        Record requestedFields = new Record();
        ArrayList <Record> returnedFields;
        
        requestedFields.addField("RegionCode", "");
        requestedFields.addField("RegionName", "");
        requestedFields.addField("SupplyCenter", false);
        requestedFields.addField("Owner", -1);
        requestedFields.addField("BounceTurn", -1);
        
        returnedFields = db.readAllRecords("Region", requestedFields, null);
        
        if (db.getErrNo() == 0) {
            for (Record record: returnedFields){
                String regionCode = record.getStringVal("RegionCode");
                String regionName = record.getStringVal("RegionName");
                boolean supplyCenter = record.getBoolVal("SupplyCenter");
                int ownerId = record.getIntVal("Owner");
                int standoffTurn = record.getIntVal("BounceTurn");
                
                if (standoffTurn != props.getTurn()){
                    standoffTurn = -1;
                }
                
                Region r = new Region (regionName, regionCode, supplyCenter, standoffTurn);
                
                allRegions.put(regionCode, r);
                
                //
                if (ownerId > 0 && supplyCenter) {
                    r.setOwnerId(ownerId);
                    Player p = allPlayers.get(ownerId);
                    p.addSupplyCenter(r);
                }
            }
        } else {
            DataAccessException ex = new DataAccessException ("loadRegions: Error reading regions", db.getErrNo(), db.getErrorMsg());
            
            throw ex;
        }
    }
    
    /**
     * Creates the list of all borders.
     * NB Assumes that the borders have been loaded.
     */
    private void loadBorders () throws DataAccessException {
        DataAccessor db = new DataAccessor ();
        Record requestedFields = new Record();
        ArrayList <Record> returnedFields;
        Record where = new Record();
        
        requestedFields.addField("BorderId", -1);
        requestedFields.addField("RegionCode", "");
        requestedFields.addField("Type", "");
        requestedFields.addField("BorderName", "");
        requestedFields.addField("mapX", 0);
        requestedFields.addField("mapY", 0);
        
        returnedFields = db.readAllRecords ("Border",requestedFields, null);
        
        if (db.getErrNo() == 0) {
            for (Record record : returnedFields) {
                int borderId = record.getIntVal("BorderId");
                String regionCode = record.getStringVal("RegionCode");
                String type = record.getStringVal("Type");
                String borderName = record.getStringVal("BorderName");
                int x = record.getIntVal("mapX");
                int y = record.getIntVal("mapY");
                
                Region owner = allRegions.get(regionCode);
                
                Border b = new Border (borderId, owner, Border.mapStringToType(type), borderName, x, y );
                allBorders.put(borderId, b);
                
                if (b.getType() == Border.BorderType.COAST) {
                    coastsList.add(b);
                }
                
            }
            
            Collections.sort(coastsList);
            
            //Create the list of neighbours for each border
            for (Map.Entry m : allBorders.entrySet()) {
                requestedFields.clear();
                requestedFields.addField ("Boarder2", 0);
                
                Border b1 = (Border)m.getValue();

                where.addField("Boarder1", b1.getBorderId());
                
                returnedFields = db.readAllRecords ("Adjacent",requestedFields, where);
                
                for (Record record : returnedFields){
                    int borderId = record.getIntVal("Boarder2");
                    
                    Border b2 = allBorders.get(borderId);
                    
                    b1.addNeighbour(b2);
                }
            }
        } else {
            DataAccessException ex = new DataAccessException ("loadBorders: Error reading borders", db.getErrNo(), db.getErrorMsg());
            
            throw ex;
        }
        
    }
    
    private void loadUnits () throws DataAccessException{
        DataAccessor db = new DataAccessor ();
        Record requestedFields = new Record();
        ArrayList <Record> returnedFields;
        
        requestedFields.addField("UnitId", 0);
        requestedFields.addField("UnitType", "");
        requestedFields.addField("PlayerId", 0);
        requestedFields.addField("Occupies", 0);
        
        returnedFields = db.readAllRecords ("Unit",requestedFields, null);
        
        if (db.getErrNo() == 0) {
            for (Record record : returnedFields) {
                int unitId = record.getIntVal("UnitId");
                String unitType = record.getStringVal("UnitType");
                int playerId = record.getIntVal("PlayerId");
                int occupies = record.getIntVal("Occupies");
                
                Border pos = allBorders.get(occupies);
                
                Unit u = new Unit (unitId, Unit.mapStringoUnitCode(unitType), pos, playerId, false);
                allUnits.put(unitId, u);
                
                Player p = allPlayers.get(playerId);
                
                p.addUnit(u);
                
            }
        } else {
            DataAccessException ex = new DataAccessException ("loadBorders: Error reading borders", db.getErrNo(), db.getErrorMsg());
            
            throw ex;
        }            
    }

    private void loadCurrentOrders () {
        DataAccessor db = new DataAccessor ();
        Record requestedFields = new Record();
        Record whereFields = new Record();
        ArrayList <Record> returnedFields;
        
        requestedFields.addField("OrderId", 0);
        requestedFields.addField("Type", "");
        requestedFields.addField("Origin", 0);
        requestedFields.addField("Destination", 0);
        requestedFields.addField("UnitId", 0);
        requestedFields.addField("BeingConvoyed", false);
        requestedFields.addField("State", 0);
        
        whereFields.addField("Round", props.getTurn());
        
        returnedFields = db.readAllRecords ("Command",requestedFields, whereFields);
        
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
                
                Order o = new Order (orderId, Order.mapCommandTypeFromString(type), dest, origin, beingConvoyed, unit, props.getTurn());
                unit.setCurrentOrder(o);
                allOrders.put(orderId, o);
            }
        }
        
    }
       
    public Map<Integer, Player> getAllPlayers() {
        return allPlayers;
    }

    public Map<String, Region> getAllRegions() {
        return allRegions;
    }

    public Map<Integer, Border> getAllBorders() {
        return allBorders;
    }

    public Map<Integer, Unit> getAllUnits() {
        return allUnits;
    }

    public Map<Integer, Order> getAllOrders() {
        return allOrders;
    }

    public LinkedList<Border> getCoastsList() {
        return coastsList;
    }
    
    
    
    public String getTitle () {
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
    
    public Props.Phase getGamePhase () {
        return props.getPhase();
    }
    /**
     * 
     * @param targetCode
     * @return 
     */
    private LinkedList<Order> findOrdersTargettingRegion (String targetCode) {
        LinkedList <Order> ret = new LinkedList<>();
        for (Map.Entry m : allOrders.entrySet()) {
            Order o = (Order) m.getValue();
            
            if (o.getDest().getRegion().getRegionCode().compareTo(targetCode) == 0 && o.getState() != Order.ORDER_STATE.FAILED) {
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
    private Order findAuxOrder (int fromId, int toId) {
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
     * 
     * @param order
     * @return 
     */
    private boolean checkForCuts (Order order){
        boolean cut = false;
        
        int unitId = order.getUnitId();
        Unit unit = allUnits.get(unitId);
        
        for (Order o : findOrdersTargettingRegion(order.getDest().getRegion().getRegionCode())){
            if (o.getCommand() == Order.OrderType.MOVE || o.getCommand() == Order.OrderType.CONVOY) {
                order.setState(Order.ORDER_STATE.SEEN);
                if (o.getState() != Order.ORDER_STATE.UNSEEN) {
                    if (!checkForCuts (o)) {
                        order.setCut(true);
                        cut = true;        
                    }
                }
            }
        }
        
        return cut;
    }
    
    private Order getOrderForRegion (String regionCode){
        Order ret = null;
        Unit unit;
        
        for (Map.Entry m : allUnits.entrySet()){
            unit = (Unit) m.getValue();
            if (regionCode.equalsIgnoreCase(unit.getPosition().getRegion().getRegionCode())) {
                ret = unit.getCurrentOrder();
                break;
            }
        }
        
        return ret;
    }
    
    /**
     * 
     */
    public void resolveAllOrders () throws DataAccessException {
        boolean cut = false;
        boolean pending = false;
        
        //Check for cuts and count supports.
        for (Map.Entry m : allOrders.entrySet()) {
            Order order = (Order)m.getValue();
            
            if (order.getCommand() == Order.OrderType.CONVOY || 
                order.getCommand() == Order.OrderType.SUPPORT){
                cut = checkForCuts (order);
            }

            if (!cut && order.getCommand() == Order.OrderType.SUPPORT ){
                Order supported = findAuxOrder (order.getDestinationId(), order.getOriginId());

                if (supported != null) {
                    supported.incrementSupportCount();
                }
            }
            
            if (cut && order.getCommand() == Order.OrderType.CONVOY){
                Order convoyed = findAuxOrder (order.getDestinationId(), order.getOriginId());

                if (convoyed != null) {
                    convoyed.setState(Order.ORDER_STATE.FAILED);
                }
            }
            
            order.setState(Order.ORDER_STATE.SEEN);
        }
        
        //Resolve conflicts
        do {
            pending = false;
            
            for (Map.Entry m : allOrders.entrySet()) {
                Order order = (Order)m.getValue();

                if ((order.getCommand() == Order.OrderType.MOVE ||
                    order.getCommand() == Order.OrderType.HOLD) && 
                    !(order.getState() == Order.ORDER_STATE.FAILED ||
                        (order.getState() == Order.ORDER_STATE.SUCCEEDED) )){
                    
                    //Find the list of orders that target the same region, but remove the curent order.
                    LinkedList<Order> conflicts = findOrdersTargettingRegion(order.getDest().getRegion().getRegionCode());
                    conflicts.remove(order);
                    
                    if (conflicts.isEmpty()){
                        //If the target is unwanted because of an unresolved order set to pending
                        String regionCode = order.getDest().getRegion().getRegionCode();
                        Order o = getOrderForRegion(regionCode);
                        boolean waiting = false;
                        
                        if (o != null) {
                            if ( o.getCommand() == Order.OrderType.MOVE && o.getState() != Order.ORDER_STATE.SUCCEEDED ) {
                                pending = true;
                                waiting = true;
                            }
                        }

                        if (!waiting){
                            //If no one else wants it it is yours
                            order.setState(Order.ORDER_STATE.SUCCEEDED);
                            int unitId = order.getUnitId();
                            
                            Unit unit = (Unit)allUnits.get(unitId);
                            
                            unit.setPosition(order.getDest());
                            if (!order.save()) {
                                DataAccessException ex = new DataAccessException ("loadBorders: Error saving order", order.getErrNo(), order.getErrMsg());

                                throw ex;                                
                            }
                            
                            if (!unit.save()) {
                                DataAccessException ex = new DataAccessException ("loadBorders: Error saving unit", order.getErrNo(), order.getErrMsg());

                                throw ex;                                                                
                            }
                        }
                        
                        
                    } else {
                        //Check for the highest number of supports
                        Collections.sort(conflicts, Collections.reverseOrder());
                        
                        Order otherOrder = conflicts.getFirst();
                        
                        //If this order has the most suppport it wins
                        if (order.getSupportCount() > otherOrder.getSupportCount()){
                            order.setState(Order.ORDER_STATE.SUCCEEDED);
                            int unitId = order.getUnitId();
                            
                            Unit unit = (Unit)allUnits.get(unitId);
                            
                            unit.setPosition(order.getDest());
                            unit.save();
                            order.save();
                            
                            //And fail the other orders
                            for (Order o : conflicts) {
                                if (o.getCommand() == Order.OrderType.HOLD) {
                                    o.setState(Order.ORDER_STATE.FAILED);
                                    o.save();
                                } else {
                                    o.setCommand(Order.OrderType.HOLD);
                                    o.resetSupportCount();
                                    o.save();
                                    pending = true;
                                }
                            }

                            
                        } else if (order.getSupportCount() < otherOrder.getSupportCount()) {
                           //This order lost so mark it as such
                           //If it is a hold mark it as failed, otherwise change to Hold and set the Pending flag
                           if (order.getCommand() == Order.OrderType.HOLD){
                               order.setState(Order.ORDER_STATE.FAILED);
                               order.save();
                           } else {
                               order.setCommand(Order.OrderType.HOLD);
                               order.resetSupportCount();
                               pending = true;
                           }
                           
                        } else {
                            //Standoff. Teat all orders as fails except a HOLD order (piece stays where it is). Mark the region as involved in a standoff unless there is a hold
                           if (order.getCommand() == Order.OrderType.HOLD){
                               order.setState(Order.ORDER_STATE.FAILED);
                           } else {
                               order.setCommand(Order.OrderType.HOLD);
                               order.resetSupportCount();
                               pending = true;
                           }

                           boolean standoff = true;
                           for (Order o : conflicts) {
                                if (o.getCommand() == Order.OrderType.HOLD){
                                    order.setState(Order.ORDER_STATE.SUCCEEDED);
                                    standoff = false;
                                } else {
                                    order.setCommand(Order.OrderType.HOLD);
                                    order.resetSupportCount();
                                    pending = true;
                                }                               
                           }
                           
                           if (standoff) {
                               Region standOffRegion = order.getDest().getRegion();
                               
                               standOffRegion.setStandoff();
                              
                           }
                            
                        }
                            
                        
                    }
                }
            }
        } while (pending);
        
    }
    
    /**
     * Returns the number of units requiring retreats
     * Looks for failed HOLD orders. 
     * This works because move orders that fail get changed to HOLD orders and get checked again.
     * 
     * 
     * @param player
     * @return 
     */
    
    public int countAllRetreats () {
        int retreatCount = 0;
        
        for (Map.Entry map : allOrders.entrySet()) {
            
            
        }
        
        return retreatCount;
    }
    
}
