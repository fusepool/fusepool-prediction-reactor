package com.xerox.openxerox.client;

import com.xerox.services.RestEngine;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.codehaus.jettison.json.JSONObject;

import java.net.SocketAddress;
import java.net.Proxy;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import javax.net.ssl.HttpsURLConnection;
import org.apache.http.*;

/**
 * This singleton class is used by the probes mainly in order to update
 * prediction models on OpenXerox servers
 */

/**
 *
 * @author aczerny
 */
public final class OpenXerox4Push implements RestEngine {
    
    /**
     * Singleton class...
     */
    
    /**
     * Using slf4j for logging
     */
    private static final Logger log = LoggerFactory.getLogger(OpenXerox4Push.class);
    
    private OpenXerox4Push() {
        // Nichts !
    }
    private static OpenXerox4Push OpenXerox4Push_instance = new OpenXerox4Push();
    
    public static RestEngine getInstance() {
        if (OpenXerox4Push_instance == null) {
            OpenXerox4Push_instance = new OpenXerox4Push();
        }
        return (RestEngine) OpenXerox4Push_instance;
    }
    
    private String baseURL = "https://mtls.services.open.xerox.com";
    //private String baseURL = "http://spider-7:8000/";
    
    public String doGet(String service) throws Exception { // It will return JSON stuff later I think
        try {
            /**
             * These 2 lines are about the http proxy in XRCE, it should be
             * removed when deployed on the platform
             */
//            SocketAddress addr = new InetSocketAddress("cornillon.grenoble.xrce.xerox.com", 8000);
//            Proxy proxy = new Proxy(Proxy.Type.HTTP, addr);
            
            URL url = new URL(this.baseURL + service);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            log.info("doGet() about to send to " + baseURL + service + " :");
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            String result = "";
            while ((line = rd.readLine()) != null) {
               result += line;
            }
            rd.close();
            log.info("doGet() Status :");
            log.info(result);
            return result;
        } catch (Exception e) {
            log.error("OpenXerox4Push cannot access " + baseURL + service + ", " + e);
            return null;
        }
    }

    public String doPost(String service, HashMap<String, String> data) {
        try {
            
//            SocketAddress addr = new InetSocketAddress("cornillon.grenoble.xrce.xerox.com", 8000);
//            Proxy proxy = new Proxy(Proxy.Type.HTTP, addr);
            
            URL url = new URL(this.baseURL + service);
            final HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            
            //HttpURLConnection conn = (HttpURLConnection) siteUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", 
                   "application/x-www-form-urlencoded");
            conn.setUseCaches (true);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            
//            DataOutputStream out = new DataOutputStream(conn.getOutputStream());
            // --------------------
            //access the required files and do the required networking as priviledged
            DataOutputStream out = (DataOutputStream)AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    try {
                        return new DataOutputStream(conn.getOutputStream());
                    } catch (IOException e) {
                        log.error("OpenXerox4Push cannot access the service ", e);
                    }
                    return null;
                }
            });
            Set keys = data.keySet();
            Iterator keyIter = keys.iterator();
            String content = "";
            for(int i=0; keyIter.hasNext(); i++) {
                Object key = keyIter.next();
                if(i!=0) {
                    content += "&";
                }
                content += key + "=" + URLEncoder.encode(data.get(key), "UTF-8");
            }
            out.writeBytes(content);
            out.flush();
            out.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = "";
            String result = "";
            while((line=in.readLine())!=null) {
                log.info(line);
                result+=line+"\n";
            }
            in.close();
            return result;
        } catch (Exception ex) {
            log.error("OpenXerox4Push cannot access " + baseURL + service + ", " + ex);
            return null;
        }
    }
}
