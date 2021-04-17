/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Logic;

import Data.*;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that represents either an army or a fleet  on the board
 * 
 * @author Tim Little
 */
public class Unit {
    public enum UnitType {ARMY, FLEET};

    private int unitId;
    private UnitType unitType;
    private Border position;
    private int ownerId;
    private boolean disbanded;
    private Order currentOrder = null;
    private int errNo;
    private String errMsg;
    
    
    public Unit(int unitId, UnitType unitType, Border position, int ownerId, boolean disbanded) {
        this.unitId = unitId;
        this.unitType = unitType;
        this.position = position;
        this.ownerId = ownerId;
        this.disbanded = disbanded;
    }
    
    public Unit(UnitType unitType, Border position, int ownerId, boolean disbanded) {
        this.unitId = -1;
        this.unitType = unitType;
        this.position = position;
        this.ownerId = ownerId;
        this.disbanded = disbanded;
        
        
    }

    public int getUnitId() {
        return unitId;
    }

    public UnitType getUnitType() {
        return unitType;
    }

    public Border getPosition() {
        return position;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public boolean isDisbanded() {
        return disbanded;
    }

    public void setPosition(Border position) {
        this.position = position;
    }

    public void setDisbanded(boolean disbanded) {
        this.disbanded = disbanded;
    }
    
    public static UnitType mapStringoUnitCode (String code) {
        UnitType ret = UnitType.ARMY;
        
        switch (code.toUpperCase()) {
            case "A":
            case "ARMY":
                ret = UnitType.ARMY;
                break;
                
            case "F":
            case "FLEET":
                ret = UnitType.FLEET;
                break;
        }
        
        return ret;
    }
    
    public String getTypeCode() {
        String code;
        
        if (unitType == UnitType.ARMY){
            code = "A";
        } else {
            code = "F";
        }
        
        return code;
    }
    
    /**
     * Writes the current unit info to permanent storage. 
     * If it is a new record the unit id will be updated.
     * 
     * @return success or failure
     */
    public boolean save () {
        boolean success = false;
        Record fields = new Record();
        DataAccessor db = new DataAccessor();
                
        fields.addField("UnitType", getTypeCode());
        fields.addField("Occupies", position.getBorderId() );
        fields.addField("PlayerId", ownerId);
        
        
        //If the key is -1 then we need to insert and get the keyfield
        if (unitId == -1) {
            unitId = db.insertRecord("UnitId", fields, true);
            
            if (unitId >= 0) {
                success = true;
            }
            
        } else {
            //Otherwise update.
            success = db.updateKeyRecord(unitId, "UnitId", "Unit", fields);
        }

        if (!success) {
            errNo = db.getErrNo();
            errMsg = db.getErrorMsg();
        }
        
        
        return success;
    }
    
    /**
     *
     * @return String for displaying 
     */
    @Override
    public String toString() {
        String ret;
        
        ret = getTypeCode() + " " + position.getBorderName();
        
        return ret;
    }

    public Order getCurrentOrder() {
        if (currentOrder == null) {
            try {
                //Default to a hold order
                Props props = new Props ();
                
                currentOrder = new Order (-1, Order.OrderType.HOLD, this.getPosition(), this.getPosition(), false, this, props.getTurn());
                currentOrder.save();
                        } catch (IOException ex) {
                Logger.getLogger(Unit.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return currentOrder;
    }

    public void setCurrentOrder(Order currentOrder) {
        this.currentOrder = currentOrder;
    }

    public int getErrNo() {
        return errNo;
    }

    public String getErrMsg() {
        return errMsg;
    }
    
    
    
}
