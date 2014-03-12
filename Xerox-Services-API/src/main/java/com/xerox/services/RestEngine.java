package com.xerox.services;

import java.util.HashMap;

import org.codehaus.jettison.json.JSONObject;

/**
 *
 * @author aczerny
 */
public interface RestEngine {
    
    public String doGet(String service) throws Exception;
    
    public String doPost(String service, HashMap<String,String> params) throws Exception;
}
