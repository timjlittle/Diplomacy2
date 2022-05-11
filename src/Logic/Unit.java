/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Logic;

import Data.*;
import java.io.IOException;
import java.util.LinkedList;
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
    private Border victorOrigin = null;
    
    
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

    public void delete () throws DataAccessException {
        currentOrder.delete();
        
        DataAccessor db = new DataAccessor ();
        
        if (!db.deleteRecord(unitId, "UnitId", "Unit") ) {
            DataAccessException ex = new DataAccessException (db.getErrorMsg(), db.getErrNo(), "Erro deleting unit " + unitId);
            
            throw ex;            
        }
        
        
        
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

    /**
     * 
     * @return The playerId of the country which owns this unit
     */
    public int getOwnerId() {
        return ownerId;
    }

    public boolean isDisbanded() {
        return disbanded;
    }

    public Border getVictorOrigin() {
        return victorOrigin;
    }

    public void setVictorOrigin(Border VictorOrigin) {
        this.victorOrigin = VictorOrigin;
    }

    
    public void setPosition(Border position) {
        this.position.setOccupyingUnit(null);

        this.position = position;
        position.setOccupyingUnit(this);
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
        
        if (victorOrigin == null) {
            fields.addField("victorOrigin", -1);
        } else {
            fields.addField("victorOrigin", victorOrigin.getBorderId());
        }
        
        
        
        //If the key is -1 then we need to insert and get the keyfield
        if (unitId == -1) {
            unitId = db.insertRecord("Unit", fields, true);
            
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
                
                currentOrder = new Order (-1, Order.OrderType.HOLD, this.getPosition(), this.getPosition(), false, this, props.getTurn(), 0);
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
    
    /**
     * Finds a list of the possible regions a unit can retreat to.
     * If non are found the list is initialised but empty
     * 
     * @return list of the possible borders the unit could retreat to.
     */
    public LinkedList<Border> getPossibleRetreats () {
        LinkedList<Border> ret = new LinkedList<>();
        
        LinkedList<Border> possibles = position.getNeighbours();
        
        for (Border b : possibles) {
            if (!b.getRegion().isOccupied() &&
                    b.getRegion().getRegionCode() != currentOrder.getRegionBeatenFrom() ) {
                
                //Make sure that the unit can retreat
                if (b.getType() == Border.BorderType.COAST ||
                    (b.getType() == Border.BorderType.LAND && unitType == Unit.UnitType.ARMY) ||
                     b.getType() == Border.BorderType.SEA && unitType == UnitType.FLEET)
                    
                    ret.add(b);
            }
        }
        
        return ret;
    }
    
}
