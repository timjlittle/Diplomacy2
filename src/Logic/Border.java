/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Logic;

import java.util.LinkedList;

/**
 * This class represents a regions borders. A region can have one or more borders.
 * Borders are connected to one or more borders of other regions.
 * A border can connect to a border of the same region
 * @author timjl
 */
public class Border implements Comparable<Border> {
    private int borderId;
    private Region region;
    private BorderType type;
    private String borderName;
    private LinkedList <Border> neighbours = new LinkedList<>();
    int mapX;
    int mapY;
    private Unit occupyingUnit = null;
    
    public enum BorderType {LAND, SEA, COAST};

    /**
     * Basic constructor
     * 
     * @param borderId unique numeric identifies
     * @param region The region the border belongs to
     * @param type Land, Sea or Coast
     * @param borderName The display name of the border
     * @param x Coordinate for this border on the map
     * @param y Coordinate for this border on the map
     */
    public Border(int borderId, Region region, BorderType type, String borderName, int x, int y) {
        this.borderId = borderId;
        this.region = region;
        this.type = type;
        this.borderName = borderName;
        this.mapX = x;
        this.mapY = y;
       
    }
    
    public static BorderType mapStringToType (String bt) {
        BorderType ret = BorderType.LAND;
        
        switch (bt.toUpperCase()) {
            case "S":
            case "SEA":
                ret = BorderType.SEA;
                break;
                
            case "C":
            case "COAST":
                ret = BorderType.COAST;
                break;

            case "L":
            case "LAND":
                ret = BorderType.LAND;
                break;
                
        }
        
        return ret;
    }
    
    public void addNeighbour (Border border) {
        neighbours.add(border);
    }

    /**
     * 
     * @return The unique ID of the border
     */
    public int getBorderId() {
        return borderId;
    }

    /**
     * 
     * @return The region the border belongs to
     */
    public Region getRegion() {
        return region;
    }

    /**
     *  
     * @return Land, Sea or Coast
     */
    public BorderType getType() {
        return type;
    }

    /**
     * 
     * @return The display name of the border
     */
    public String getBorderName() {
        return borderName;
    }

    public LinkedList<Border> getNeighbours() {
        return neighbours;
    }

    public int getMapX() {
        return mapX;
    }

    public int getMapY() {
        return mapY;
    }

    public Unit getOccupyingUnit() {
        return occupyingUnit;
    }

    public void setOccupyingUnit(Unit occupyingUnit) {
        this.occupyingUnit = occupyingUnit;
        
    }
    
    public boolean isOccupied () {
        return occupyingUnit != null;
    }

    @Override
    public String toString() {
        return borderName;
    }    
    
    @Override
    public int compareTo(Border otherBorder) {
        return borderName.compareTo(otherBorder.getBorderName());
    }

    public boolean isNeighbour (Border neighbour) {
        boolean ret = neighbours.contains(neighbour) ;
        
        return ret;
    }    
}
