/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Data;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

/**
 *  Class to be used to log the actions during the game.
 * @author timjl
 */
public class GameLogger {
    
    
        
    /**
     * Appends the specified text message to the log file preceded by a timestamp and followed by a newline
     * @param msg 
     */
    public void logMessage (String msg) {
        FileWriter logFile;
        
        try {
            String line = "";
            Props props = new Props();
            
            String loc = props.getLogFileLoc();
            
            logFile = new FileWriter(loc, true);
            
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();
            
            line = dtf.format(now) + ": " + msg + "\n";
            
           
            
            BufferedWriter writer = new BufferedWriter(logFile);
            
            writer.append(line);
            
            writer.close();
            logFile.close();
            
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(GameLogger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Truncates any existing logfile or creates a new one
     */
    public void resetLog () {
        FileWriter logFile;
        try {
            Props props = new Props();
            
            logFile = new FileWriter(props.getLogFileLoc());
            
            
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(GameLogger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
