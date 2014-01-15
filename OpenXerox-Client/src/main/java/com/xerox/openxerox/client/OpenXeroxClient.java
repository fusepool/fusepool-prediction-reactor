package com.xerox.openxerox.client;

import com.xerox.services.ClientEngine;
import com.xerox.services.RestEngine;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;

/**
 * OSGi component allowing to access Push&Pull services to OpenXerox
 * @author aczerny
 */
@Component(metatype = true, immediate = true)
@Service
public class OpenXeroxClient implements ClientEngine {
    
    public RestEngine getPull() {
        return OpenXerox4Pull.getInstance();
    }
    
    public RestEngine getPush() {
        return OpenXerox4Push.getInstance();
    }
    
    @Activate
    public void activator() {
        System.out.println("[OPENXEROX CLIENT] Started !");
    }
    
    @Deactivate
    public void deactivator() {
        System.out.println("[OPENXEROX CLIENT] Stopped !");
    }
}
