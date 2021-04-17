/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Logic;

import Data.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The instructions for an individual unitId.
 * 
 * @author Tim Little
 */
public class Order implements Comparable<Order> {
    public enum OrderType {HOLD, MOVE, SUPPORT, CONVOY, RETREAT, DISBAND};
    public enum ORDER_STATE {UNSEEN, PENDING, SUCCEEDED, FAILED, SEEN};
    private int orderId = -1;
    private OrderType command;
    private int destinationId;
    private int originId;
    boolean beingConvoyed;
    private ORDER_STATE state;
    private Unit unit;
    private int turn;
    private String destinationName;
    private String originName;
    private String unitType;
    private String unitLocationName;
    private Border dest = null;
    private Border origin = null;
    private int supportCount = 0;
    private boolean cut = false;
    private int errNo;
    private String errMsg;
    
    /**
     * Creates a new Order object by reading the details of an existing Order from disk
     * 
     * @param orderId 
     * @throws Data.DataAccessException 
     */
    public Order (int orderId) throws DataAccessException  {
        Record fields = new Record ();
        DataAccessor db = new DataAccessor ();
        int ret;
        
        fields.addField("Type", "");
        fields.addField("Origin", -1);
        fields.addField("Destination", -1);
        fields.addField("Round", -1);
        fields.addField("UnitId", -1);
        fields.addField("BeingConvoyed", false);
        fields.addField("State", 0);
        
        ret = db.readIndividualRecord(orderId, "Command", "OrderId", fields);
        
        if (ret == 0) {
            int intVal;
            String strVal;
            this.orderId = orderId;
            
            strVal = fields.getStringVal("Type");
            command = mapCommandTypeFromString (strVal);
            originId = fields.getIntVal("Origin");
            destinationId = fields.getIntVal("Destination");
            turn = fields.getIntVal("Round");
            beingConvoyed = fields.getBoolVal("BeingConvoyed");
            intVal = fields.getIntVal("State");
            state = mapIntToState(intVal);
            
            destinationName = getBorderName(destinationId);
            originName = getBorderName (originId);
            
        } else {
            Data.DataAccessException ex = new Data.DataAccessException ("Unable to read Order " + orderId, db.getErrNo(), db.getErrorMsg());
            
            throw (ex);
                        
        }
        
        
    }
    
    /**
     * Creates a new Order object but does not save to disk.
     * Call save() to ensure persistence
     * 
     * @param OrderId Unique identifier. pass -1 to indicate a new record.
     * @param command What the order is (Move. Hold etc)
     * @param destination Where the unitId should end up. NB for convoys this should be the destinationId of the unitId being convoyed
     * @param origin Where the unitId should end up. NB for convoys this should be the originId of the unitId being convoyed
     * @param beingConveyed whether or not the unitId is being convoyed
     * @param unit The unitId that owns this order
     * @param turn The game turn
     * 
     * 
     */
    public Order(int OrderId, OrderType command, Border destination, Border origin, boolean beingConveyed, Unit unit, int turn)  {
        this.orderId = OrderId;
        this.command = command;
        this.dest = destination;
        this.origin = origin;
        this.beingConvoyed = beingConveyed;
        this.state = ORDER_STATE.UNSEEN; 
        this.unit = unit;
        this.turn = turn;
        
        if (destination != null){
            destinationId = destination.getBorderId();
            destinationName = destination.getBorderName();
        }
        
        if (origin != null) {
            originName = origin.getBorderName();
            originId = origin.getBorderId();
        }
        
    }
    
    
    /**
     * Writes the data to disk.
     * 
     * @return true if successful.
     */
    public boolean save () {
        boolean success = false;
        
        Record fields = new Record ();
        DataAccessor db = new DataAccessor ();
        
        fields.addField("Type", getCommandTypeAsString ());
        fields.addField("Origin", originId);
        fields.addField("Destination", destinationId);
        fields.addField("Round", turn);
        fields.addField("UnitId", unit.getUnitId());
        fields.addField("BeingConvoyed", beingConvoyed);
        fields.addField("State", getStateToInt ());
        
        
        if (orderId == -1) {
            //If it is a new order insert into the table and get the new ID
            orderId = db.insertRecord("command", fields, true);
            
            if (orderId > 0) {
                success = true;
            }
            
        } else {
            //Otherwise update the existing record
            success = db.updateKeyRecord(orderId, "OrderId", "command", fields);
        }
        
        if (!success) {
            errNo = db.getErrNo();
            errMsg = db.getErrorMsg();
        }
        
        return success;
    }

    public int getOrderId() {
        return orderId;
    }

    public OrderType getCommand() {
        return command;
    }

    public int getDestinationId() {
        return destinationId;
    }

    public int getOriginId() {
        return originId;
    }

    public boolean isBeingConvoyed() {
        return beingConvoyed;
    }

    public ORDER_STATE getState() {
        return state;
    }

    public int getUnitId() {
        return unit.getUnitId();
    }

    public int getTurn() {
        return turn;
    }

    public void setState(ORDER_STATE state) {
        this.state = state;
    }

    public Border getDest() {
        return dest;
    }

    public void setDest(Border dest) {
        this.dest = dest;
        if (dest != null) {
            this.destinationId = dest.getBorderId();
            this.destinationName = dest.getBorderName();
        }
    }

    public Border getOrigin() {
        return origin;
    }

    public void setOrigin(Border origin) {
        this.origin = origin;
        if  (origin != null) {        
            this.originId = origin.getBorderId();
            this.originName = origin.getBorderName();
        }
    }

    public void setCommand(OrderType command) {
        this.command = command;
    }

    public void setBeingConvoyed(boolean beingConvoyed) {
        this.beingConvoyed = beingConvoyed;
    }

    public int getSupportCount() {
        return supportCount;
    }

    public void incrementSupportCount() {
        this.supportCount++;
    }
    
    public void resetSupportCount () {
        this.supportCount = 0;
    }

    public boolean isCut() {
        return cut;
    }

    public void setCut(boolean cut) {
        this.cut = cut;
    }

    public int getErrNo() {
        return errNo;
    }

    public void setErrNo(int errNo) {
        this.errNo = errNo;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }
    
    
    
    @Override
    public int compareTo(Order otherOrder) {
        return supportCount - otherOrder.getSupportCount();
    }
    
    /**
     * Maps the command type code to an English string
     * 
     * @return 
     */
    public String getCommandTypeAsString () {
        return getCommandTypeAsString (command);
    }
    
    public static String getCommandTypeAsString (Order.OrderType type) {
        String ret = "error";
        
        switch (type) {
            case HOLD:
                ret = "Hold";
                break;
                
            case MOVE:
                ret = "Move";
                break;
                
            case SUPPORT:
                ret = "Support";
                break;
                
            case CONVOY:
                ret = "Convoy";
                break;
                
            case RETREAT:
                ret = "Retreat";
                break;
                
            case DISBAND:
                ret = "Disband";
                break;
                           
        }
        
        return ret;
    }
    
    public static String [] getCommandTypes (Props.Phase phase) {
        String ret [] = null;
        
        switch (phase) {
            case ORDER:
                ret = new String []{"hold","move", "support", "convoy"};
                break;
                
            case RETREAT:
                ret = new String []{"move"};
                break;
                
            case BUILD:
                ret = new String []{"leave", "disband"};
                break;
                
        }
        
        
        return ret;
    }
    
    /**
     * 
     * @param val String version of the command (e.g. hold, move etc)
     * 
     * @return enum code for the command
     * 
     */
    public static OrderType mapCommandTypeFromString (String val) {
        OrderType ret = OrderType.HOLD;
        
        switch (val.toLowerCase()) {
            case "hold":
                ret = OrderType.HOLD;
                break;
                
            case "move":
                ret = OrderType.MOVE;
                break;
                
            case "support":
                ret = OrderType.SUPPORT;
                break;
                
            case "convoy":
                ret = OrderType.CONVOY;
                break;
                
            case "retreat":
                ret = OrderType.RETREAT;
                break;
                
            case "disband":
                ret = OrderType.DISBAND;
                break;
                           
        }
        
        return ret;
    }    
    
    
    private int getStateToInt () {
        int ret = -1;
        
        switch (state){
            case UNSEEN:
                ret = 0;
                break;

            case PENDING:
                ret = 1;
                break;

            case SUCCEEDED:
                ret = 2;
                break;

            case FAILED:
                ret = 3;
                break;
                
        }
        
        return ret;
    }
    
    private Order.ORDER_STATE mapIntToState (int val) {
        Order.ORDER_STATE ret = ORDER_STATE.UNSEEN;
        
        switch (val) {
            case 0:
                ret = ORDER_STATE.UNSEEN;
                break;
                
            case 1:
                ret = ORDER_STATE.PENDING;
                break;
                
            case 2:
                ret = ORDER_STATE.SUCCEEDED;
                break;
                
            case 3:
                ret = ORDER_STATE.FAILED;
                break;
        }
        
        return ret;
    }
    
    private String getBorderName (int borderId) throws DataAccessException {
        String borderName = "error";
        int ret;
        
        Record fields = new Record ();
        DataAccessor db = new DataAccessor ();
        
        fields.addField ("BorderName", "unknown");
        
        ret = db.readIndividualRecord(borderId, "border", "BorderId", fields);
        
        if (ret == 0) {
            borderName = fields.getStringVal("BorderName");
        } else {
            DataAccessException ex = new DataAccessException ("Unable to read border " + borderId, db.getErrNo(), db.getErrorMsg());
            
            Logger.getLogger(Order.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Unable to read border " + borderId);
            
            throw (ex);
        }
        
        return borderName;
    }
      
    
    @Override
    public String toString () {
        String ret;
        
        ret = unit.getTypeCode() + " " + unit.getPosition().getBorderName() + " " + getCommandTypeAsString ();
        
        switch (command) {
            case HOLD:
            case DISBAND:
                break;
                
            case MOVE:
            case RETREAT:
                ret += " to " + destinationName;
                break;
                
            case SUPPORT:
            case CONVOY:
                ret = originName + " to " + destinationName;
                break;               
        }
        
        return ret;
    }
    
}
