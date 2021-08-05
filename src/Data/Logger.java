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
 *
 * @author timjl
 */
public class Logger {
    FileWriter logFile;
    
    
    public Logger () {
        
        try {
            Props props = new Props();
            
            logFile = new FileWriter(props.getLogFileLoc(), true);
            
            
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(Logger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void logMessage (String msg) {
        try {
            String line = "";
            
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();
            
            line = dtf.format(now) + ": " + msg + "\n";
            
            BufferedWriter writer = new BufferedWriter(logFile);
            
            writer.append(line);
            
            writer.close();
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(Logger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
