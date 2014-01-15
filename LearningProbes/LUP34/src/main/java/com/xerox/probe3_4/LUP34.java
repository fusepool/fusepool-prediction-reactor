package com.xerox.probe3_4;

import com.xerox.predictionhub.PredictionHub;
import com.xerox.services.LUPEngine;
import com.xerox.services.ClientEngine;
import com.xerox.services.HubEngine;
import com.xerox.services.RestEngine;
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

import org.apache.clerezza.rdf.core.event.GraphEvent;
import org.apache.clerezza.rdf.core.event.GraphListener;

import org.codehaus.jettison.json.JSONObject;

import org.apache.clerezza.rdf.core.event.FilterTriple;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.LoggerFactory;

/**
 * The goal of this 3.4 LUP Module is to listen to label annotations and to ask
 * for proper services on OpenXerox
 */

/**
 *
 * @author aczerny
 */
@Component(metatype = true, immediate = true)
@Service
public class LUP34 implements LUPEngine
{
    
    /**
     * Using slf4j for logging
     */
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(LUP34.class);
    
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
    
    public String getName() {
        return "LUP34";
    }

    public String getDescription() {
        return "LUP module which provides a set of services for the Labelling T3.4 task.";
    }

    public String predict(HashMap<String, String> params) {
        log.error("Not implemented yet");
        return "Not implemented yet";
    }
    
    private class Listener3_4 implements GraphListener {

        public void graphChanged(List<GraphEvent> list) {
            log.info("graphChanged");
            for (GraphEvent e : list) {
                Iterator<Triple> itTriple = annostore.filter(e.getTriple().getSubject(), null, null);
                while (itTriple.hasNext()) {
                    Triple newTriple = itTriple.next();
                }
                Resource body = annostore.filter(e.getTriple().getSubject(),
                        new UriRef("http://fusepool.eu/ontologies/annostore#hasBody"),
                        null).next().getObject();
                
                itTriple = annostore.filter((NonLiteral)body, null, null);
                while (itTriple.hasNext()) {
                    Triple newTriple = itTriple.next();
                }
                itTriple = annostore.filter((NonLiteral)body, new UriRef("http://fusepool.eu/ontologies/annostore#hasNewLabel"), null);
                while (itTriple.hasNext()) {
                    Triple newTriple = itTriple.next();
                    Iterator<Triple> itLabel = annostore.filter((NonLiteral)(newTriple.getObject()),
                            new UriRef("http://www.w3.org/2011/content#chars"),
                            null);
                    while (itLabel.hasNext()) {
                        Triple newTriple2 = itLabel.next();
                    }
                }
                itTriple = annostore.filter((NonLiteral)body, new UriRef("http://fusepool.eu/ontologies/annostore#hasDeletedLabel"), null);
                while (itTriple.hasNext()) {
                    Triple newTriple = itTriple.next();
                    Iterator<Triple> itLabel = annostore.filter((NonLiteral)newTriple.getObject(),
                            new UriRef("http://www.w3.org/2011/content#chars"),
                            null);
                    while (itLabel.hasNext()) {
                        Triple newTriple2 = itLabel.next();
                    }
                }
                log.info("TODO: *******************************************************");
                log.info("TODO: GET THE CONTENT OF THE DOCUMENT");
                log.info("TODO:   For this : use the <content> ontology Reto showed me.");
                log.info("TODO: *******************************************************");
            }
            /**
             * Basically here we would need to BI things, and then update the models
             */
            //BI("This is a modification.");
            updateModels(null);
        }
        
    }
    
    private LUP34.Listener3_4 listener3_4;
    private FilterTriple filter3_4;
    private long delay3_4;
    
    @Reference
    private ClientEngine openXeroxClient;
    
    private RestEngine clientPush;
    private RestEngine clientPull;
    
    @Reference
    private PredictionHub predictionHub;
    
    @Activate
    public void activate() {
        log.info("Activation");
        // 1.) Accessing the AnnoStore
        tcManager.getMGraph(ANNOTATION_GRAPH_NAME);
        annostore = tcManager.getMGraph(ANNOTATION_GRAPH_NAME);
        // 2.) Instanciating any listener, filter, delay and web access needed
        this.listener3_4 = new LUP34.Listener3_4();
        this.filter3_4 = new FilterTriple(
                null ,
                null ,
                new UriRef("http://fusepool.eu/ontologies/annostore#LabellingAnnotation"));
        // this.filter3_4 = new FilterTriple(null, null, null);
        this.delay3_4 = 5000;
        this.clientPush = openXeroxClient.getPush();
        this.clientPull = openXeroxClient.getPull();
        this.predictionHub.register(this);
    }
    
    @Deactivate
    private void deactivate() {
        log.info("Deactivate");
        this.predictionHub.unregister(this);
    }
    
    public GraphListener getListener() {
        if (this.listener3_4 == null) {
            this.listener3_4 = new LUP34.Listener3_4();
        }
        return this.listener3_4;
    }

    public FilterTriple getFilter() {
        if (this.filter3_4 == null) {
            /**
             * With this filter we are supposed to dump only one of the modifications
             * induced by the curl example request in the Github
             * (you can also find this request in the method
             * AnnotationStorePostRequestTest() in XRCEBundle class)
             */
            this.filter3_4 = new FilterTriple(
                    null ,
                    null ,
                    new UriRef("http://fusepool.eu/ontologies/annostore#LabellingAnnotation"));
            // this.filter3_4 = new FilterTriple(null, null, null);

        }
        return this.filter3_4;
    }
    
    public long getDelay() {
        return this.delay3_4;
    }

    public String predict(String param) {
        /**
         * 1. Parse the <param> string (just a single URI)
         * 2. Get the content of the text from the ECS
         * 3. Calls the proper openxerox service
         * 4. Parse the result as a JSON object
         * 5. Build and return the proper result (list of labels)
         */
        // 1. Ok... that is <param> actually
        // 2. I wait for Reto's answer
        String content = "";
        HashMap<String,String> serviceParam = new HashMap<String, String>();
        serviceParam.put("text", content);
        // 3.
        try {
            String jsonResult = this.clientPull.doPost("corpus_wikipedia/dopredictlabels/", serviceParam);
        } catch (Exception ex) {
            log.error("Labelling service currently unavailable");
        }
        // 4.
        
        return "bricks;manufacturing;industry";
    }
    
    public void updateModels(HashMap<String, String> params) {
        try {
            log.info("UpdateModels");
            /** TEST 3.4
             * 1.) POST : /corpus_wikipedia/addText/, param: text=""
             * 2.) POST : /corpus_wikipedia/addLabels/, param: docId="", Label=""
             * 3.) GET :  /learn/5
             * 4.) POST : /corpus_wikipedia/dopredictlabels/, param: text=""
             */
            //this.clientPush.doPost("/corpus_wikipedia/addtext/", "Document about manufacturing bricks in Toulouse");
//            HashMap<String,String> params = new HashMap<String, String>();
//            params.put("text", "Document about manufacturing bricks in Toulouse");
//            this.clientPush.doPost("/corpus_wikipedia/addtext/", params);
//            params = new HashMap<String, String>();
//            params.put("docID", "5");
//            params.put("labels", "djenty");
//            this.clientPush.doPost(("/corpus_wikipedia/addlabels/"), params);
            
            /** TEST 2.5
             * 1.) POST : /additem/, param: user, query, source, click (known user, known source)
             * 2.) POST : /additem/, param: user, query, source, click (known user, unknown source)
             * 3.) POST : /additem/, param: user, query, source, click (unknown user, known source)
             * 4.) POST : /additem/, param: user, query, source, click (unknown user, unknown source)
             * 5.) GET  : /dolearn/5/
             * 6.) GET  : /dump/
             * 7.) POST : /dopredictallsources/, param: user, query
             */
            
//            HashMap<String,String> params = new HashMap<String, String>();
//            params.put("user", "Adrian");
//            params.put("query", "Fusepool query one");
//            params.put("source", "source one");
//            params.put("click", "0");
//            this.clientPush.doPost("/additem/", params);
//            params = new HashMap<String, String>();
//            params.put("user", "FusepoolOtherOtherUser");
//            params.put("query", "Fusepool query two");
//            params.put("source", "patent");
//            params.put("click", "1");
//            this.clientPush.doPost("/additem/", params);
//            params = new HashMap<String, String>();
//            params.put("user", "FusepoolUser");
//            params.put("query", "Fusepool query three");
//            params.put("source", "source one");
//            params.put("click", "0");
//            this.clientPush.doPost("/additem/", params);
//            params = new HashMap<String, String>();
//            params.put("user", "FusepoolOtherUser");
//            params.put("query", "Fusepool query four");
//            params.put("source", "patents");
//            params.put("click", "1");
//            this.clientPush.doPost("/additem/", params);
//            
//            this.clientPush.doGet("/dolearn/5/");
//            
//            this.clientPush.doGet("/dump/");
//            
//            params = new HashMap<String, String>();
//            params.put("user", "FusepoolOtherUser");
//            params.put("query", "Fusepool query five");
//            this.clientPush.doPost("/dopredictallsources/", params);
        } catch (Exception ex) {
            log.error("Labelling prediction service unavailable");
        }
    }
    
    /**
     * Nothing to do since the the model is not on the platform.
     */
    public void save() {}
    public void load() {}
    
}
