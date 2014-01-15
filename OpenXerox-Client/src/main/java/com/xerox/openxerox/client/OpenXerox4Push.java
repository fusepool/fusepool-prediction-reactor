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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
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
    

//    private String baseURL = "https://services.open.xerox.com/WebApp2.svc/MTLS";
    //private String baseURL = "https://mtls.services.open.xerox.com"; // New DNS entry
    private String baseURL = "http://spider-16:8080"; // Julien's machine
    
    private static final Logger log = LoggerFactory.getLogger(OpenXerox4Push.class);

    
    public String doGet(String service) throws Exception { // It will return JSON stuff later I think
        try {
            /**
             * These 2 lines are about the http proxy in XRCE, it should be
             * removed when deployed on the platform
             */
            SocketAddress addr = new InetSocketAddress("cornillon.grenoble.xrce.xerox.com", 8000);
            Proxy proxy = new Proxy(Proxy.Type.HTTP, addr);
            
            URL url = new URL(this.baseURL + service);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection(proxy);
            conn.setRequestMethod("GET");
            System.out.println("[OPENXEROX PUSH] doGet() about to send to " + baseURL + service + " :");
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            String result = "";
            while ((line = rd.readLine()) != null) {
               result += line;
            }
            rd.close();
            System.out.println("[OPENXEROX PUSH] doGet() Status :");
            System.out.println(result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String doPost(String service, HashMap<String, String> data) {
        try {
            
            SocketAddress addr = new InetSocketAddress("cornillon.grenoble.xrce.xerox.com", 8000);
            Proxy proxy = new Proxy(Proxy.Type.HTTP, addr);
            
            URL url = new URL(this.baseURL + service);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection(proxy);
            
            //HttpURLConnection conn = (HttpURLConnection) siteUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", 
                   "application/x-www-form-urlencoded");
            conn.setUseCaches (true);
            conn.setDoOutput(true);
            conn.setDoInput(true);

            DataOutputStream out = new DataOutputStream(conn.getOutputStream());

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
            //System.out.println(content);
            out.writeBytes(content);
            out.flush();
            out.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = "";
            String result = "";
            while((line=in.readLine())!=null) {
                System.out.println(line);
                result+=line+"\n";
            }
            in.close();
            return result;
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(OpenXerox4Push.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }
    
//    public String doPost(String service, HashMap<String,String> params) throws Exception { // It will return JSON stuff later I think
//        try {
//            /**
//             * These 2 lines are about the http proxy in XRCE, it should be
//             * removed when deployed on the platform
//             */
//            SocketAddress addr = new InetSocketAddress("cornillon.grenoble.xrce.xerox.com", 8000);
//            Proxy proxy = new Proxy(Proxy.Type.HTTP, addr);
//            
//            URL url = new URL(this.baseURL + service);
//            HttpURLConnection conn = (HttpURLConnection) url.openConnection(proxy);
//            
//            conn.setDoOutput(true);
//            conn.setDoInput(true);
//            conn.setInstanceFollowRedirects(false);
//            conn.setRequestMethod("POST");
//            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
//            conn.setRequestProperty("charset", "utf-8");
//            
//            String urlParameters = "";
//            Integer dirtyCounter = 0;
//            for (String key : params.keySet()) {
//                String value = params.get(key);
//                if (dirtyCounter == 0) {
//                    urlParameters = key + "=" + value;
//                } else {
//                    urlParameters = urlParameters + "&" + key + "=" + value;
//                }
//                dirtyCounter++;
//            }
//            System.out.println("[OPENXEROX PUSH] doPost() about to send to " + baseURL + service + " :");
//            for (String key : params.keySet()) {
//                System.out.println("[OPENXEROX PUSH] " + key + " = " + params.get(key));
//            }
//            
//            conn.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
//            DataOutputStream wr = new DataOutputStream(conn.getOutputStream ());
//            wr.writeBytes(urlParameters);
//            wr.flush();
//            wr.close();
//            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//            String line;
//            String result = "";
//            while ((line = rd.readLine()) != null) {
//               result += line;
//            }
//            System.out.println("[OPENXEROX PUSH] DoPost() Status :");
//            System.out.println(result);
//            rd.close();
//            return result;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
}
