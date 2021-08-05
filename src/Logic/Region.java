/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Logic;

import Data.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a region on the board (e.g. London or Mid Atlantic)
 * A region will have one or more borders. The borders determine the neighbours
 * 
 * @author timjl
 */
public class Region {
    private ArrayList <Border> borders = new ArrayList<>();
    private final String regionName;
    private final String regionCode;
    private final boolean supplyCenter;
    private int oriOwner;
    private int ownerId;
    private int standoff = -1;

    /**
     * Region constructor
     * 
     * @param regionName e.g. long name, North Atlantic Ocean
     * @param regionCode e.g. Unique identifier NAO
     * @param supplyCenter whether or not this is a supply center
     * @param standoffTurn -1 if not the current turn
     */
    public Region(String regionName, String regionCode, boolean supplyCenter, int standoffTurn, int oriOwner) {
        this.regionName = regionName;
        this.regionCode = regionCode;
        this.supplyCenter = supplyCenter;
        this.ownerId = 0;
        this.standoff = standoffTurn;
        this.oriOwner = oriOwner;
    }
    
    /**
     * Add the borders to the list of borders.
     * A region must have at least one border, but may have more (e.g. Spain or StPetersburg)
     * 
     * @param border 
     */
    public void addBorder (Border border) {
        borders.add(border);
    }

    public String getRegionName() {
        return regionName;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public boolean isSupplyCenter() {
        return supplyCenter;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) throws DataAccessException {
        this.ownerId = ownerId;
        save();
    }

    public ArrayList<Border> getBorders() {
        return borders;
    }

    public int getStandoff() {
        return standoff;
    }

    public void setStandoff() throws DataAccessException {
        try {
            Props props = new Props ();
            this.standoff = props.getTurn();
        
            save ();
            
        } catch (IOException e) {
            Logger.getLogger(Region.class.getName()).log(Level.SEVERE, null, e);
            Data.DataAccessException ex = new Data.DataAccessException ("File error updating region " + regionCode, -1,e.getMessage());
            
            throw (ex);            
        }
        
        
    }
  
    public boolean isOccupied () {
        boolean occupied = false;
        
        for (Border b : borders) {
            if (b.isOccupied()) {
                occupied = true;
                break;
            }
        }
        
        return occupied;
    }
    
    @Override
    public String toString () {
        return regionName;
    }
    
    private void save () throws DataAccessException {
        
        //Save this to disk
        DataAccessor db = new DataAccessor ();
        Record fields = new Record ();
        Record where = new Record();

        //Only Owner and standoff turn turn can change
        fields.addField("CurOwner", ownerId);
        fields.addField("BounceTurn", standoff);

        where.addField("RegionName", regionName);

        if (!db.updateRecord("Region", fields, where)){
            Data.DataAccessException ex = new Data.DataAccessException ("Database error " + regionCode, db.getErrNo(),db.getErrorMsg());
            Logger.getLogger(Region.class.getName()).log(Level.SEVERE, null, ex);
            
            throw (ex);   
        }    
    }
    
    public void resetOwner () throws DataAccessException {
        ownerId = oriOwner;
        
        save();
    }
    
}
