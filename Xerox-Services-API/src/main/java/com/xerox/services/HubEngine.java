package com.xerox.services;

import java.util.HashMap;

/**
 *
 * @author aczerny
 */
public interface HubEngine {
    /**
     * We just test if Felix likes it more when services come from an interface
     * EDIT : It does.
     */
    void register(LUPEngine p);
    
    void unregister(LUPEngine p);
    
    String predict(String lupName, HashMap<String,String> params);
}
