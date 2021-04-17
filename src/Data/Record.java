/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Data;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * This class holds a list of fields
 * 
 * @author timjl
 */
public class Record {
    /**
     * The data type of the field
     */
    public enum FieldType {
        INT,
        BOOLEAN,
        STRING};
    
    private Dictionary fields = new Hashtable();
               
            
    private class Field {
        private String fieldName;
        FieldType type;
        private int intVal;
        private boolean boolVal;
        private String stringVal;
        
        public Field (String name, String val) {
            this.type = FieldType.STRING;
            this.fieldName = name;
            this.stringVal = val;
        }
        
        public Field (String name, int val) {
            this.type = FieldType.INT;
            this.fieldName = name;
            this.intVal = val;
        }
        
        public Field (String name, boolean val) {
            this.type = FieldType.BOOLEAN;
            this.fieldName = name;
            this.boolVal = val;
        }
        
    }
    
    /**
     * Overloaded method to add a field to the list fields in the record.
     * If the field exists it updates the value
     * 
     * @param name The field name
     * @param val The field value. 
     */
    public void addField (String name, String val) {
        name = name.toUpperCase();
        
        Field f = (Field)fields.get(name);
        
        if (f == null){
            fields.put(name, new Field (name, val));
        } else {
            f.type = FieldType.STRING;
            f.stringVal = val;
        }
    }
    
    /**
     *
     * @param name
     * @param val
     */
    public void addField (String name, int val) {
        name = name.toUpperCase();
        Field f = (Field)fields.get(name);
        
        if (f == null){
            fields.put(name, new Field (name, val));
        } else {
            f.type = FieldType.INT;
            f.intVal = val;
        }
    }
    
    /**
     *
     * @param name
     * @param val
     */
    public void addField (String name, boolean val) {
        name = name.toUpperCase();
        Field f = (Field)fields.get(name);
        
        if (f == null){
            fields.put(name, new Field (name, val));
        } else {
            f.type = FieldType.BOOLEAN;
            f.boolVal = val;
        }
    }

    /**
     * 
     * @param fieldName The name of the field to check
     * @return The type or null if the field does not exist
     */
    public FieldType getFieldType (String fieldName) {
        fieldName = fieldName.toUpperCase();
        Field f = (Field) fields.get(fieldName);
        
        if (f != null)
            return f.type;
        else
            return null;
    }
    
    /**
     * Gets the String value of the specified field
     * If the field does not exist or is not String returns null
     * 
     * @param fieldname
     * @return String value or null
     */
    public String getStringVal (String fieldname) {
        String ret = null;
        fieldname = fieldname.toUpperCase();
        Field f = (Field) fields.get(fieldname);
        
        if (f != null) {
            if (f.type == FieldType.STRING)
            ret = f.stringVal;
        }
        
        return ret;
    }
    
    public int getIntVal (String fieldname) {
        int ret = 0;
        fieldname = fieldname.toUpperCase();
        Field f = (Field) fields.get(fieldname);
        
        if (f != null) {
            if (f.type == FieldType.INT)
            ret = f.intVal;
        }
        
        return ret;
    }    
    
    public boolean getBoolVal (String fieldname) {
        fieldname = fieldname.toUpperCase();
        boolean ret = false;
        Field f = (Field) fields.get(fieldname);
        
        if (f != null) {
            if (f.type == FieldType.BOOLEAN)
            ret = f.boolVal;
        }
        
        return ret;
    }  
    
    /**
     * Gets a list of the field names in this record
     * @return Array list containing the field names
     */
    public ArrayList<String> getAllFieldNames () {
        ArrayList <String> ret = new ArrayList <>();
        
        for (Enumeration enm = fields.keys(); enm.hasMoreElements();){
            ret.add(enm.nextElement().toString());
        }
        
        return ret;
    }

    /**
     * Gets a list of the values in this record
     * @return Array list containing the String representations of the field values
     */
    public ArrayList<String> getAllFieldValues () {
        ArrayList <String> ret = new ArrayList <>();
        
        for (Enumeration enm = fields.elements(); enm.hasMoreElements();){
            ret.add(enm.nextElement().toString());
        }
        
        return ret;
    }
    
    public boolean isEmpty () {
        return fields.isEmpty();
    }
    
    public void clear () {
        fields = new Hashtable();
    }

}
