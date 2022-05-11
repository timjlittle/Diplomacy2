/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Logic;

import java.util.ArrayList;

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
        
        for (Unit u : units) {
            //reset the order
            if (u.getCurrentOrder().getState() == Order.ORDER_STATE.FAILED) {
                u.getCurrentOrder().setCommand(Order.OrderType.RETREAT);
                u.getCurrentOrder().setOrigin(u.getPosition());
                u.getCurrentOrder().setState(Order.ORDER_STATE.UNSEEN);
                //default to the first on the list of possible retreats
                u.getCurrentOrder().setDest(u.getPossibleRetreats().get(0));
                
                retreatList.add(u);
            }
            
            if (u.getCurrentOrder().getCommand() == Order.OrderType.RETREAT && 
                    u.getCurrentOrder().getState() == Order.ORDER_STATE.UNSEEN){
                retreatList.add(u);
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
        return playerName;
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
