/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Data;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Intended to provide a set of source agnostic methods for interacting with 
 * stored data.
 * 
 * @author Tim Little 2021
 */
public class DataAccessor {
    static private final String JDBC_DRIVER = "org.sqlite.JDBC";
    private String errorMsg;
    private String sqlState;
    private int errNo;
    


    /**
     * 
     * @return most recent error message generated
     */
    public String getErrorMsg() {
        return errorMsg;
    }

    /**
     * 
     * @return Information regarding the SQL connection
     */
    public String getSqlState() {
        return sqlState;
    }

    /**
     * 
     * @return Most recent error number.
     */
    public int getErrNo() {
        return errNo;
    }
    
    //
    // Connects to the default datanase.
    //
    private Connection connect() throws SQLException, IOException {
        // SQLite connection string
        //String url = "jdbc:sqlite:C://sqlite/db/test.db";
        //At least start optimistic
        String dbLoc;
        Props props = new Props();
        
        errorMsg = "";
        sqlState = "";
        errNo = 0;
        
        dbLoc = props.getDatabaseDetails();
        
        Connection conn = null;
        try {
        conn = DriverManager.getConnection("jdbc:sqlite:" + dbLoc);
        Class.forName("org.sqlite.JDBC");
                
        conn = DriverManager.getConnection("jdbc:sqlite:" + dbLoc);
        
        conn.close();
        
        conn = DriverManager.getConnection("jdbc:sqlite:" + dbLoc);
        
        } catch (ClassNotFoundException ex) {
                Logger.getLogger(DataAccessor.class.getName()).log(Level.SEVERE, null, ex);
                errorMsg = ex.getLocalizedMessage();
                sqlState = "";
                errNo = 0;
            }   
        return conn;
    }
    
        private String readTableDef (String dbDefFile) throws FileNotFoundException, IOException {
        String sql = "";
        BufferedReader bufferedReader = new BufferedReader(
                      new FileReader(dbDefFile));
        
        while (bufferedReader.ready()){
            sql= sql + bufferedReader.readLine();
        }
        
        return sql;
    }
        
   /**
    * Creates a new database from a file definition.
    * 
    * @param dbDefFile Name of the file
    * @param dbLoc Folder location
    * 
    * @return True if successful. If false check the object's get error methods
    */
    public boolean newDbFromfile (String dbDefFile, 
                                  String dbLoc) {
        boolean success = true;
        String tableDef = "";
        
        //At least start optimistic
        errorMsg = "";
        sqlState = "";
        errNo = 0;
        
        try {
            tableDef = readTableDef (dbDefFile);
        } catch (IOException  e) {
            success = false;
            Logger.getLogger(DataAccessor.class.getName()).log(Level.SEVERE, null, e);
            errorMsg = e.getMessage();
            sqlState = "";
            errNo = 0;
        }
        
        if (success) {
            Connection conn = null;
            try {
                
                conn = connect ();
                
                if (conn != null) {
                    Statement stmt = conn.createStatement();

                    stmt.execute(tableDef);
                } else {
                    success = false;
                }
                
            } catch (SQLException e) {
            success = false;
            
            errorMsg = e.getMessage();
            sqlState = e.getSQLState();
            errNo = e.getErrorCode();
            Logger.getLogger(DataAccessor.class.getName()).log(Level.SEVERE, null, e);
            
        }   catch (IOException ex) {
                Logger.getLogger(DataAccessor.class.getName()).log(Level.SEVERE, null, ex);
                errorMsg = ex.getMessage();
                errNo = 1;
                
            }    finally {
                try {
                    if (conn != null) {
                        conn.close();
                    }
                } catch (SQLException ex) {
                    System.out.println(ex.getMessage());
                }
            }
        }
        
        return success;
    }

    /**
     * Reads an individual record identified by the keyfield = dataId pair.
     * 
     * @param dataId unique identifier for the keyfield
     * @param tableName Name of the table
     * @param KeyName name of the keyfield
     * @param vals pre-populated Record object with the fields and data types expected to be returned. After calling the values for these fields will be populated.
     * @return error code (0 if successful)
     */
    public int readIndividualRecord (int dataId, 
                           String tableName, 
                           String KeyName, 
                           Record vals) {
        ResultSet rs = null;
        String sql;
        Record retData = new Record();
        
        //At least start optimistic
        errorMsg = "";
        sqlState = "";
        errNo = 0;
        
        ArrayList <String> fieldList = vals.getAllFieldNames();
        
        //Construct the select statement using the field names passed in
        sql = "SELECT ";
        
        boolean first = true;
        for (String field : fieldList) {
            if (first) {
                sql += " " + field;
                first = false;
            } else {
                sql += ", " + field;
            }
        }
        
        sql += " FROM " + tableName + " WHERE " + KeyName + " = " + dataId;
        Connection conn = null;
        try {

            conn = connect ();

            if (conn != null) {
                Statement stmt = conn.createStatement();

                rs = stmt.executeQuery(sql);
                
                //Copy the values out of the recordset and into the supplied list
                for (String field : fieldList) {
                    boolean boolVal;
                    int intVal;
                    String strVal;
                    
                    switch (vals.getFieldType(field)) {
                        case STRING:
                            strVal = rs.getString(field);
                            vals.addField(field, strVal);
                            break;
                            
                        case BOOLEAN:
                            boolVal = rs.getBoolean(field);
                            vals.addField(field, boolVal);
                            break;
                            
                        case INT:
                            intVal = rs.getInt(field);
                            vals.addField(field, intVal);
                            break;
                            
                    }
                }
                
                
            } 

        } catch (SQLException e) {
        
        errorMsg = e.getMessage();
        sqlState = e.getSQLState();
        errNo = e.getErrorCode();
        Logger.getLogger(DataAccessor.class.getName()).log(Level.SEVERE, null, e);

    }   catch (IOException ex) {
            Logger.getLogger(DataAccessor.class.getName()).log(Level.SEVERE, null, ex);
            errorMsg = ex.getMessage();
            errNo = 1;
            
        }    finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
        }

        return errNo;
    }
    
    /**
     * Inserts a new record into a table
     * @param tableName Table to insert into
     * @param vals Record containing the fields to insert
     * @param hasKey True if the table will generate a key value
     * @return if haskey is true the value of the new key, otherwise -1
     */
    public int insertRecord (String tableName, 
                             Record vals, 
                             boolean hasKey) {
        int key = -1;
        String sql;
        Connection conn = null;
        String val;
        
        //At least start optimistic
        errorMsg = "";
        sqlState = "";
        errNo = 0;
        
        sql = "INSERT INTO " + tableName + "(";
        
        ArrayList<String> fieldList = vals.getAllFieldNames();
        boolean first = true;
        
        //Add the comma delimited list of field names
        for (String field : fieldList){
            if (first) {
                first = false;
                sql += " " + field;
            } else {
                sql += ", " + field;
            }
        }
        
        sql = sql + ") VALUES (";
        
        //Now add the comma separated values
        first = true;
        for (String field : fieldList){
            int intVal;
            boolean boolVal;
            String strVal;
            
            if (first) {
                
                first = false;
            } else {
                sql = sql + ", ";
            }
            
            switch (vals.getFieldType(field)) {
            
                case STRING:
                    strVal = vals.getStringVal(field);
                    //escape dangerous strings
                    //val = val.replaceAll("\\", "\\\\" );
                    strVal = strVal.replace("\"", "\\\"" );
                    strVal = strVal.replace("\'", "\\\'" );
                    strVal = "'" + strVal + "'";


                    sql = sql + strVal;
                break;
                
                case INT:
                    intVal = vals.getIntVal(field);
                    sql = sql + intVal; 
                    break;
                    
                //With boolean convert to 0 and -1
                case BOOLEAN:
                    boolVal = vals.getBoolVal(field);
                    
                    if (boolVal) {
                        sql += "-1";
                    } else {
                        sql += "0";
                    }
            }
            
            
            
        }
        
        sql = sql + " )";
        try {

            conn = connect ();

            if (conn != null) {
                Statement stmt = conn.createStatement();

                stmt.execute(sql);
                
                //if there isn't a key indicate success by setting key to 0
                if (!hasKey)
                    key = 0;
                else {
                    //Otherwise select the key value
                    sql = "select last_insert_rowid();";
                    
                    ResultSet rs    = stmt.executeQuery(sql);
                    
                    key = rs.getInt("last_insert_rowid()");
                }
            } 

        } catch (SQLException e) {
        
        errorMsg = e.getMessage();
        sqlState = e.getSQLState();
        errNo = e.getErrorCode();
        Logger.getLogger(DataAccessor.class.getName()).log(Level.SEVERE, null, e);

    }   catch (IOException ex) {
            Logger.getLogger(DataAccessor.class.getName()).log(Level.SEVERE, null, ex);
            errorMsg = ex.getMessage();
            errNo = 1;
            
        }    finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
        }
        
        return key;
    }
    
    /**
     * Update an individual record identified by it's key field
     * @param key value of the key
     * @param keyField 
     * @param tableName
     * @param vals
     * @return 
     */
    public boolean updateKeyRecord (int key, 
                                    String keyField, 
                                    String tableName, 
                                    Record vals) {
        boolean success = true;
        String sql;
        Connection conn = null;
        
        
        //At least start optimistic
        errorMsg = "";
        sqlState = "";
        errNo = 0;
        
        sql = "UPDATE " + tableName + " SET ";
        
        ArrayList <String> fieldList = vals.getAllFieldNames();
        boolean first = true;
        
        for (String field : fieldList) {

            if (first) {
                first = false;
            } else {
                sql += ", ";
            }
                        
            sql += field + " = ";
        
            switch (vals.getFieldType(field)) {
                case STRING:
                    String strVal = vals.getStringVal(field);
                    //escape dangerous strings
                    //val = val.replaceAll("\\", "\\\\" );
                    strVal = strVal.replace("\"", "\\\"" );
                    strVal = strVal.replace("\'", "\\\'" );
                    strVal = "'" + strVal + "'";

                    sql += strVal;
                    break;
                    
                case INT:
                    int intVal = vals.getIntVal(field);
                    sql += "" + intVal;
                    break;
                    
                case BOOLEAN:
                    boolean boolVal = vals.getBoolVal(field);
                    
                    
                    if (boolVal) {
                        sql += "-1";
                    } else {
                        sql += "0";
                    }                    
                    break;
                    
            }
        }
        
        sql = sql + " WHERE " + keyField + " = " + key;
        try {

            conn = connect ();

            if (conn != null) {
                Statement stmt = conn.createStatement();

                stmt.execute(sql);
                
            } 

        } catch (SQLException e) {
        
        success = false;
        errorMsg = e.getMessage();
        sqlState = e.getSQLState();
        errNo = e.getErrorCode();
        Logger.getLogger(DataAccessor.class.getName()).log(Level.SEVERE, null, e);

    }   catch (IOException ex) {
            Logger.getLogger(DataAccessor.class.getName()).log(Level.SEVERE, null, ex);
            errorMsg = ex.getMessage();
            errNo = 1;
            
        }    finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
                
            }
        }
        
        
        return success;
    }

    /**
     * Update an individual record identified by it's key field
     * @param tableName
     * @param vals
     * @param where
     *
     * @return 
     */
    public boolean updateRecord ( String tableName, 
                                  Record vals,
                                  Record where) {
        boolean success = true;
        String sql;
        Connection conn = null;
        
        
        //At least start optimistic
        errorMsg = "";
        sqlState = "";
        errNo = 0;
        
        sql = "UPDATE " + tableName + " SET ";
        
        ArrayList <String> fieldList = vals.getAllFieldNames();
        boolean first = true;
        
        for (String field : fieldList) {

            if (first) {
                first = false;
            } else {
                sql += ", ";
            }
                        
            sql += field + " = ";
        
            switch (vals.getFieldType(field)) {
                case STRING:
                    String strVal = vals.getStringVal(field);
                    //escape dangerous strings
                    //val = val.replaceAll("\\", "\\\\" );
                    strVal = strVal.replace("\"", "\\\"" );
                    strVal = strVal.replace("\'", "\\\'" );
                    strVal = "'" + strVal + "'";

                    sql += strVal;
                    break;
                    
                case INT:
                    int intVal = vals.getIntVal(field);
                    sql += "" + intVal;
                    break;
                    
                case BOOLEAN:
                    boolean boolVal = vals.getBoolVal(field);
                    
                    
                    if (boolVal) {
                        sql += "-1";
                    } else {
                        sql += "0";
                    }                    
                    break;
                    
            }
        }
        
        sql = sql + " WHERE ";
        first = true;
        
        ArrayList <String> whereFields = where.getAllFieldNames();
        for (String field : whereFields){
           if (!first) {
               sql += "AND ";
           } else {
               first = false;
           }
           
           sql += field + " = ";
           
           switch (where.getFieldType(field)){
                case STRING:
                    String strVal = where.getStringVal(field);
                    //escape dangerous strings
                    //val = val.replaceAll("\\", "\\\\" );
                    strVal = strVal.replace("\"", "\\\"" );
                    strVal = strVal.replace("\'", "\\\'" );
                    strVal = "'" + strVal + "'";

                    sql += strVal;
                    break;
                    
                case INT:
                    int intVal = where.getIntVal(field);
                    sql += "" + intVal;
                    break;
                    
                case BOOLEAN:
                    boolean boolVal = where.getBoolVal(field);
                    
                    
                    if (boolVal) {
                        sql += "-1";
                    } else {
                        sql += "0";
                    }                    
                    break;
                    
            }
               
        }
           
        
        
        try {

            conn = connect ();

            if (conn != null) {
                Statement stmt = conn.createStatement();

                stmt.execute(sql);
                
            } 

        } catch (SQLException e) {
        
        success = false;
        errorMsg = e.getMessage();
        sqlState = e.getSQLState();
        errNo = e.getErrorCode();
        Logger.getLogger(DataAccessor.class.getName()).log(Level.SEVERE, null, e);

    }   catch (IOException ex) {
            Logger.getLogger(DataAccessor.class.getName()).log(Level.SEVERE, null, ex);
            errorMsg = ex.getMessage();
            errNo = 1;
            
        }    finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
                
            }
        }
        
        
        return success;
    }
    
    /**
     * Deletes the specified record from the specified table
     * 
     * @param key Key value
     * @param keyField key field name
     * @param tableName Name of the table to delete from
     * 
     * @return true if successful
     */
    public boolean deleteRecord (int key, 
                                 String keyField, 
                                 String tableName) {
        boolean success = true;
        String sql;
        Connection conn = null;
        
        //At least start optimistic
        errorMsg = "";
        sqlState = "";
        errNo = 0;
        
        sql = "DELETE FROM " + tableName + " WHERE " + keyField + " = " + key;
        
        try {

            conn = connect ();

            if (conn != null) {
                Statement stmt = conn.createStatement();

                stmt.execute(sql);
                
            } 

        } catch (SQLException e) {
        
        success = false;
        errorMsg = e.getMessage();
        sqlState = e.getSQLState();
        errNo = e.getErrorCode();
        Logger.getLogger(DataAccessor.class.getName()).log(Level.SEVERE, null, e);

    }   catch (IOException ex) {
            Logger.getLogger(DataAccessor.class.getName()).log(Level.SEVERE, null, ex);
            errorMsg = ex.getMessage();
            errNo = 1;
            
        }    finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
                
            }
        }
        
        
        return success;
    }
    
    /**
     * Reads all the records from a table and returns the requested fields in a 
     * list of records (empty if no values found)
     * 
     * @param tableName Table to read from
     * @param fieldsRequested which fields to select
     * @param whereValue List of any fields with their values used to select the records. 
     *        If more than one field is specified they are connected with logical ANDs
     * @return an arraylist of values. Empty if no records found.
     * 
     */
    public ArrayList <Record> readAllRecords ( String tableName, 
                                        Record fieldsRequested, 
                                        Record whereValue) {
        
        ArrayList<Record> ret = new ArrayList <>();
        
        //Build the SQL select query
        String sql = "SELECT ";
        
        ArrayList<String> fieldList = fieldsRequested.getAllFieldNames();
        
        //Get the list of requested fields
        boolean first = true;
        for (String field : fieldList) {
            if (!first) {
                sql += ", ";
            } else {
                first = false;
            }
            
            sql += field;
        }
        
        sql += " FROM " + tableName;
        
        if (whereValue != null) {
            if (!whereValue.isEmpty()) {
                sql += " WHERE ";

                fieldList = whereValue.getAllFieldNames();

                first = true;
                for (String field : fieldList) {
                    if (!first) {
                        sql += " AND ";
                    } else {
                        first = false;
                    }

                    sql += field + " = ";

                    switch (whereValue.getFieldType(field)) {
                        case STRING:
                            String strVal = whereValue.getStringVal(field);
                            //escape dangerous strings
                            //val = val.replaceAll("\\", "\\\\" );
                            strVal = strVal.replace("\"", "\\\"" );
                            strVal = strVal.replace("\'", "\\\'" );
                            strVal = "'" + strVal + "'";
                            sql += strVal;
                            break;

                        case INT:
                            sql += whereValue.getIntVal(field);
                            break;

                        case BOOLEAN:
                            if (whereValue.getBoolVal(field)) {
                                sql += " -1 ";
                            } else {
                                sql += " 0 ";
                            }
                            break;
                    }
                }
            }
        }
        
        Connection conn = null;
        try {

            
            conn = connect();

            if (conn != null) {
                Statement stmt = conn.createStatement();

                ResultSet rs = stmt.executeQuery(sql);
                
                fieldList = fieldsRequested.getAllFieldNames();
                //rs.first();
                if (rs.isBeforeFirst()){
                    rs.next();
                }
                
                while (!rs.isAfterLast()) {
                    Record vals = new Record();

                    //Copy the values out of the recordset and into the supplied list
                    for (String field : fieldList) {
                        boolean boolVal;
                        int intVal;
                        String strVal;

                        switch (fieldsRequested.getFieldType(field)) {
                            case STRING:
                                strVal = rs.getString(field);
                                vals.addField(field, strVal);
                                break;

                            case BOOLEAN:
                                boolVal = rs.getBoolean(field);
                                vals.addField(field, boolVal);
                                break;

                            case INT:
                                intVal = rs.getInt(field);
                                vals.addField(field, intVal);
                                break;

                        }
                    }

                    ret.add(vals);
                    rs.next();
                }
            } 

        } catch (SQLException e) {
        
        errorMsg = e.getMessage();
        sqlState = e.getSQLState();
        errNo = e.getErrorCode();
        Logger.getLogger(DataAccessor.class.getName()).log(Level.SEVERE, null, e);

    }   catch (IOException ex) {
            Logger.getLogger(DataAccessor.class.getName()).log(Level.SEVERE, null, ex);
            errorMsg = ex.getMessage();
            errNo = 1;
            
        }    finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
        }

        
        return ret;
    }
    
}
