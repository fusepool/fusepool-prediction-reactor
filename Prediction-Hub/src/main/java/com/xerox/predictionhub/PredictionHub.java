package com.xerox.predictionhub;

import com.xerox.services.HubEngine;
import com.xerox.services.LUPEngine;

import java.io.IOException;
import java.util.HashMap;

import javax.ws.rs.Path;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the predictor engine service
 * @author aczerny
 */
@Component(metatype = true, immediate = true)
@Service
public class PredictionHub implements HubEngine {

    /**
     * Using slf4j for logging
     */
    private static final Logger log = LoggerFactory.getLogger(PredictionHub.class);
    
    /**
     * Accessing the TripleCollection Manager via the OSGi framework
     */
    @Reference
    private TcManager tcManager;
    /**
     * Uri Reference to access the AnnoStore (Listener)
     */
    private UriRef ANNOTATION_GRAPH_NAME = new UriRef("urn:x-localinstance:/fusepool/annotation.graph");
    private MGraph annostore;
    
    /**
     * List of probes. Don't forget to insert any probes you need into this list
     * at activation !
     */
    private HashMap<String, LUPEngine> lupIndex;
    
    public String getAnnostore() {
        return ANNOTATION_GRAPH_NAME.toString();
    }
    
    public void register(LUPEngine p) {
        /**
         * 1) Check if the probe is not already registered
         * 2) Add it to the List
         * 3) Plug the Listener with the TripleFilter to the AS
         */
        String name = p.getName();
        if (lupIndex.containsKey(name)) {
            return;
        }
        lupIndex.put(name, p);
        annostore.addGraphListener(p.getListener(), p.getFilter(), p.getDelay());
        log.info("LUP Module " + p.getName() + " registered !");
        log.info("LUP registered :");
        for (String key : lupIndex.keySet()) {
            log.info("  - " + key);
        }
        log.info("Registering done");
    }
    
    public void unregister(LUPEngine p) {
        /**
         * We have to remove the listener from the Annostore,
         * then remove the LUPEngine from the HashMap
         */
        annostore.removeGraphListener(p.getListener());
        lupIndex.remove(p.getName());
        log.info("LUP " + p.getName() + " removed.");
    }
    
    @Activate
    private void activator() throws IOException {
        try {
            /**
             * Refactoring the project, bundle initialization :
             *  1) accessing AnnoStore
             *  2) creating new ProbesList
             *  3) that's it (everything is more OSGied now...)
             */
            
            // 1.) Accessing Annostore
            tcManager.getMGraph(ANNOTATION_GRAPH_NAME);
            annostore = tcManager.getMGraph(ANNOTATION_GRAPH_NAME);
            
            // 2.) Creating ProbesList
            lupIndex = new HashMap<String,LUPEngine>();
            
            // 3.) That's it
            log.info("Started !");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    @Deactivate
    private void deactivator() {
        // Removing every probes' listener from the annostore
        if (annostore != null) {
            for (LUPEngine p : lupIndex.values()) {
                annostore.removeGraphListener(p.getListener());
            }
        }
        log.info("Stopped !");
        /**
         * Note that we don't empty the probesList collection since a new ArrayList
         * instance will be created at (re-)activation. Garbage collector will
         * handle the memory leak we induce !
         */
    }

    public String predict(String lupName, HashMap<String,String> params) {
        /**
         * Routes the predict method through the probesList hashList
         */
        if (!lupIndex.containsKey(lupName)) {
            return "NO SUCH PREDICTOR";
        }
        return lupIndex.get(lupName).predict(params);
    }
}
