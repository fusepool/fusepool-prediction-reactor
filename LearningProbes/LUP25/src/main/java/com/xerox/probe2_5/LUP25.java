package com.xerox.probe2_5;

import com.xerox.services.ClientEngine;
import com.xerox.services.HubEngine;
import com.xerox.services.LUPEngine;
import com.xerox.services.RestEngine;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.event.FilterTriple;
import org.apache.clerezza.rdf.core.event.GraphEvent;
import org.apache.clerezza.rdf.core.event.GraphListener;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the predictor engine service
 * @author aczerny
 */
@Component(immediate = true, metatype = true, inherit = true)
@Service(LUPEngine.class)
public class LUP25 implements LUPEngine
{
    
    /**
     * Using slf4j for logging
     */
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(LUP25.class);
    
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
    
    private class Listener2_5 implements GraphListener {

        public void graphChanged(List<GraphEvent> list) {
            log.info("graph changed !");
            for (GraphEvent e : list) {
                /**
                 * 1. Get the data source
                 * 2. Get every element of the body
                 */
                HashMap<String, String> params = new HashMap<String, String>();
                Iterator<Triple> itTriple = annostore.filter(e.getTriple().getSubject(),
                        new UriRef("http://www.w3.org/ns/oa#annotatedBy"),
                        null);
                while (itTriple.hasNext()) {
                    Triple newTriple = itTriple.next();
                    params.put("user", newTriple.getObject().toString());
                }
                Resource target = annostore.filter(e.getTriple().getSubject(),
                        new UriRef("http://www.w3.org/ns/oa#hasTarget"),
                        null).next().getObject();
                
                itTriple = annostore.filter((NonLiteral)target,
                        new UriRef("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                        null);
                while (itTriple.hasNext()) {
                    Triple newTriple = itTriple.next();
                    params.put("type", newTriple.getObject().toString());
                }
                
                Resource body = annostore.filter(e.getTriple().getSubject(),
                        new UriRef("http://www.w3.org/ns/oa#hasBody"),
                        null).next().getObject();
                
                itTriple = annostore.filter((NonLiteral)body,
                        new UriRef("http://fusepool.eu/ontologies/annostore#hasQuery"),
                        null);
                while (itTriple.hasNext()) {
                    Triple newTriple = itTriple.next();
                    params.put("query", newTriple.getObject().toString());
                }
                
                itTriple = annostore.filter((NonLiteral)body,
                        new UriRef("http://fusepool.eu/ontologies/annostore#wasClicked"),
                        null);
                while (itTriple.hasNext()) {
                    Triple newTriple = itTriple.next();
                    params.put("clicked", newTriple.getObject().toString());
                }
                updateModels(params);
            }
        }
        
    }
    
    private Listener2_5 listener2_5;
    private FilterTriple filter2_5;
    private Integer delay2_5;
    
    @Reference
    private ClientEngine openXeroxClient;
    
    private RestEngine clientPush;
    private RestEngine clientPull;
    
    @Reference
    private HubEngine predictionHub;
    
    @Activate
    public void activate() {
        log.info("Activation");
        // 1.) Accessing the AnnoStore
        tcManager.getMGraph(ANNOTATION_GRAPH_NAME);
        annostore = tcManager.getMGraph(ANNOTATION_GRAPH_NAME);
        // 2.) Instanciating any listener, filter, delay and web access needed
        this.listener2_5 = new LUP25.Listener2_5();
        this.filter2_5 = new FilterTriple(
                null ,
                null ,
                new UriRef("http://fusepool.eu/ontologies/annostore#RerankingAnnotation"));
        this.delay2_5 = 5000;
        this.clientPush = openXeroxClient.getPush();
        this.clientPull = openXeroxClient.getPull();
        this.predictionHub.register(this);
    }
    
    @Deactivate
    private void deactivate() {
        log.info("Deactivate");
        this.predictionHub.unregister(this);
    }

    public String getName() {
        return "LUP25";
    }
    public String getDescription() {
        return "Listen-Update-Predict module for T2.5 refinement task.";
    }

    public GraphListener getListener() {
        return this.listener2_5;
    }

    public FilterTriple getFilter() {
        return this.filter2_5;
    }

    public long getDelay() {
        return this.delay2_5;
    }

    public void updateModels(HashMap<String, String> params) {
        try {
            clientPush.doPost("/additem/", params);
        } catch (Exception ex) {
            Logger.getLogger(LUP25.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String predict(HashMap params) {
        try {
            String result = clientPull.doPost("/dopredictallsources/", params);
            return result;
        } catch (Exception ex) {
            Logger.getLogger(LUP25.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    /**
     * Nothing to do since the the model is not on the platform.
     */
    public void save() {}
    public void load() {}
}
