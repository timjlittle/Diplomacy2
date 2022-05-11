/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
//import javax.swing.JOptionPane;

/**
 *
 * @author timjl
 */
public class Props {
    private Properties props = new Properties();
    public static final String PROPS_FILE_LOC = "config.properties";
    private String databaseDetails = "game.db";
    private String logFileLoc = "gamelog.txt";
    private static final String DB_DETAILS_KEY = "DB_DETAILS_KEY";
    private static final String TURN_KEY = "TURN_KEY";
    private static final String PHASE_KEY = "PHASE_KEY";
    private static final String LOG_FILE_KEY = "LOG_FILE_LOC_KEY";
    private int turn;
    public enum Phase {ORDER, RETREAT, BUILD};
    private Phase phase;

    public Props () throws FileNotFoundException, IOException {
       File propsFile = new File (PROPS_FILE_LOC);
        
        if (propsFile.exists()){
        
            InputStream input = new FileInputStream(PROPS_FILE_LOC);

            props.load(input);
            
            turn = Integer.parseInt(props.getProperty(TURN_KEY, "1"));
            
        }
    }

    public String getLogFileLoc () {
        String tempLoc = (String) props.getProperty(DB_DETAILS_KEY, logFileLoc);
        int finalSlashLoc = tempLoc.lastIndexOf("\\");
        
        tempLoc = tempLoc + logFileLoc;
        
        return tempLoc;
    }
    
    /*
    public void setFileLoc (String logFileLocation) throws FileNotFoundException, IOException {
        props.put(LOG_FILE_KEY, logFileLocation);
        
        FileOutputStream output = new FileOutputStream(PROPS_FILE_LOC);
        props.store(output, null);
    }
    */
    public String getDatabaseDetails() {
        return (String) props.getProperty(DB_DETAILS_KEY, databaseDetails);
    }
    
    public Phase getPhase () {
            int phaseNum = Integer.parseInt(props.getProperty(PHASE_KEY, "1"));
            
            switch (phaseNum) {
                case 1:
                    phase = Phase.ORDER;
                break;
                
                case 2:
                    phase = Phase.RETREAT;
                break;
                
                case 3:
                    phase = Phase.BUILD;
                break;
                
                default:
                    phase = Phase.ORDER;
                break;
            }

          return phase;
    }

    public void setDatabaseDetails(String databaseDetails) throws FileNotFoundException, IOException {
        props.put(DB_DETAILS_KEY, databaseDetails);
        
        FileOutputStream output = new FileOutputStream(PROPS_FILE_LOC);
        props.store(output, null);
    }
    
    public int getTurn () {
        return turn;
    }
    
    public void nextTurn () throws FileNotFoundException, IOException {
        turn++;
        
        setTurn (turn);
    }

    public void setTurn (int newVal) throws FileNotFoundException, IOException {
        turn = newVal;
        props.put(TURN_KEY, "" + turn);
        
        FileOutputStream output = new FileOutputStream(PROPS_FILE_LOC);
        props.store(output, null);
    }

    public void setPhase (Phase newPhase) throws FileNotFoundException, IOException {
        phase = newPhase;
        String phaseVal = "1";
        
        switch (newPhase) {
            case ORDER:
                phaseVal = "1";
            break;
            
            case RETREAT:
                phaseVal = "2";
            break;
            
            case BUILD:
                phaseVal = "3";
            break;
        }
        
        props.put(PHASE_KEY, phaseVal);
        
        FileOutputStream output = new FileOutputStream(PROPS_FILE_LOC);
        props.store(output, null);
    }
    
}
