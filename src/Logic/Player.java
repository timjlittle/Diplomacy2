/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Logic;

import Data.Props;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class representing a country / player (e.g. Russia or Turkey)
 * @author timjl
 */
public class Player {
    private String playerName;
    private int playerId;
    private String colour;
    
    private ArrayList <Unit> units = new ArrayList<>();
    private ArrayList <Region> supplyCenters = new ArrayList <>();
    private ArrayList <String> homeRegionCodes = new ArrayList <>();

    public Player(String playerName, int playerId, String colour) {
        this.playerName = playerName;
        this.playerId = playerId;
        this.colour = colour;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getPlayerId() {
        return playerId;
    }

    public String getColour() {
        return colour;
    }

    public ArrayList<Unit> getUnits() {
        return units;
    }

    public ArrayList<Region> getSupplyCenters() {
        return supplyCenters;
    }
    
    public void addUnit (Unit unit) {
        units.add(unit);
    }
    
    public void addSupplyCenter (Region region) {
        supplyCenters.add(region);
    }
    
    public void addHomeRegionCode (String regionCode) {
        homeRegionCodes.add(regionCode);
    }
    
    public boolean isHomeRegion (String regionCode) {
        boolean found = false;
        int counter = 0;
        
        while (!found && counter < homeRegionCodes.size()){
            found = regionCode.compareTo(homeRegionCodes.get(counter)) == 0;
            counter++;
        }
                
        return found;
    }
    
    /**
     * 
     * @return The list of units requiring retreat orders.
     */
    public ArrayList<Unit> getRetreatUnits () {
        ArrayList<Unit> retreatList = new ArrayList<>();
        
        for (Unit candidate : units) {
            //reset the order
            if (candidate.getCurrentOrder().getState() == Order.ORDER_STATE.FAILED) {
                candidate.getCurrentOrder().setCommand(Order.OrderType.RETREAT);
                candidate.getCurrentOrder().setOrigin(candidate.getPosition());
                candidate.getCurrentOrder().setState(Order.ORDER_STATE.UNSEEN);
                //default to the first on the list of possible retreats
                candidate.getCurrentOrder().setDest(candidate.getPossibleRetreats().get(0));
                
                retreatList.add(candidate);
            }
            
            if (candidate.getCurrentOrder().getCommand() == Order.OrderType.RETREAT && 
                    candidate.getCurrentOrder().getState() == Order.ORDER_STATE.UNSEEN){
                
                if (!retreatList.contains(candidate)){
                    retreatList.add(candidate);
                }
            }
        }
        
        return retreatList;
    }
    
    public void removeSupplyCenter (Region region) {
        if (supplyCenters.contains(region)) {
            supplyCenters.remove(region);
        }
    }
    
    @Override
    public String toString () {
        Props props;
        String ret = playerName;
        
        try {
            props = new Props();
            
            if (props.getPhase() == Props.Phase.BUILD) {
                ret += " (" + getBuildCount() + ")";
            }
            
        } catch (IOException ex) {
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return ret;
    }
    
    /**
     * Calculates the difference between the number of units 
     * and the number of supply centers this player owns
     * 
     * @return Number of builds/disbands needed. Could be negative
     */
    public int getBuildCount () {
       int diff;
       
       diff = supplyCenters.size() - units.size();
       
       return diff;
    }
    
}
