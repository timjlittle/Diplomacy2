/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Data;

/**
 *
 * @author timjl
 */
public class DataAccessException  extends Exception {
    int errNo;
    String extraInfo;
    
    public DataAccessException (String message, int pErrNo, String pExtraInfo) {
        super(message);
        
        errNo = pErrNo;
        extraInfo = pExtraInfo;
    }

    public int getErrNo() {
        return errNo;
    }

    public String getExtraInfo() {
        return extraInfo;
    }
    
    
    
}
