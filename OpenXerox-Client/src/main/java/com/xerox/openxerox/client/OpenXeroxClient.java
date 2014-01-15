package com.xerox.openxerox.client;

import com.xerox.services.ClientEngine;
import com.xerox.services.RestEngine;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi component allowing to access Push&Pull services to OpenXerox
 * @author aczerny
 */
@Component(metatype = true, immediate = true)
@Service
public class OpenXeroxClient implements ClientEngine {
    
    /**
     * Using slf4j for logging
     */
    private static final Logger log = LoggerFactory.getLogger(OpenXeroxClient.class);
    
    public RestEngine getPull() {
        return OpenXerox4Pull.getInstance();
    }
    
    public RestEngine getPush() {
        return OpenXerox4Push.getInstance();
    }
    
    @Activate
    public void activator() {
        log.info("Started !");
    }
    
    @Deactivate
    public void deactivator() {
        log.info("Stopped !");
    }
}
