package com.xerox.probe5_5;

import com.xerox.services.LUPEngine;
import com.xerox.services.ClientEngine;
import com.xerox.services.HubEngine;
import com.xerox.services.RestEngine;
import java.util.ArrayList;
import java.util.List;

import java.util.HashMap;
import java.util.Iterator;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.LockableMGraph;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.event.FilterTriple;
import org.apache.clerezza.rdf.core.event.GraphEvent;
import org.apache.clerezza.rdf.core.event.GraphListener;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
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
public class LUP55 implements LUPEngine
{
    /**
     * Using slf4j for logging
     */
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(LUP55.class);
    /**
     * Accessing the TripleCollection Manager via the OSGi framework
     */
    @Reference
    private TcManager tcManager;
    @Reference(target = "(name=urn:x-localinstance:/fusepool/annotation.graph)")
    private LockableMGraph annoGraph; 
    @Reference(target = "(name=urn:x-localinstance:/content.graph)")
    private LockableMGraph ecsGraph; 
    
    @Reference
    private Serializer serializer;
    /**
     * Uri Reference to access the AnnoStore (Listener)
     */
    private UriRef ANNOTATION_GRAPH_NAME = new UriRef("urn:x-localinstance:/fusepool/annotation.graph");
    private UriRef CONTENT_GRAPH_NAME = new UriRef("urn:x-localinstance:/content.graph");
    private MGraph annostore;
    private MGraph contentstore;
    
//    private UriRef CONTENT_GRAPH_NAME = new UriRef("urn:x-localinstance:/fusepool/content.graph");
//    private MGraph annostore;
    
    private class Listener5_5 implements GraphListener {

        public void graphChanged(List<GraphEvent> list) {
            log.info("graphChanged");
            for (GraphEvent e : list) {
                HashMap<String,String> params = new HashMap<String, String>();
                /**
                 * 3.) Get the document (bow)
                 * 1.) Get the user
                 * 2.) Get the query (bow)
                 * 4.) Get the docType
                 * 5.) Get the predicate
                 * 6.) Get dismissed/accepted boolean
                 */
                /**
                 * Get user.
                 */
                Resource user = annostore.filter(e.getTriple().getSubject(),
                        new UriRef("http://www.w3.org/ns/oa#annotatedBy"),
                        null).next().getObject();
                Iterator<Triple> itUser = annostore.filter(e.getTriple().getSubject(),
                        new UriRef("http://www.w3.org/ns/oa#annotatedBy"),
                        null);
                while (itUser.hasNext()) {
                    Triple userTriple = itUser.next();
                    String userString = userTriple.getObject().toString().replace("<", "").replace(">", "");
                    log.info("params.put(\"user\", "+userString+")");
                    params.put("user", userString);
                }
                /**
                 * Get query.
                 */
                Iterator<Triple> itQuery = annostore.filter(e.getTriple().getSubject(),
                        new UriRef("http://fusepool.eu/ontologies/annostore#hasQuery"),
                        null);
                while(itQuery.hasNext()) {
                    Triple queryTriple = itQuery.next();
                    String queryString = queryTriple.getObject().toString().replace("\"", "");
                    log.info("params.put(\"query\", "+queryString+")");
                    params.put("query", queryString);
                }
                /**
                 * Let's work on the body
                 */
                Iterator<Triple> itBody = annostore.filter(e.getTriple().getSubject(),
                        new UriRef("http://www.w3.org/ns/oa#hasBody"),
                        null);
                while (itBody.hasNext()){
                    Triple bodyTriple = itBody.next();
                    /**
                     * Get predicate (check whether it is accepted or dismissed).
                     */
                    String booldisplay = "0";
                    Iterator<Triple> itPredicate = annostore.filter((NonLiteral)bodyTriple.getObject(),
                            new UriRef("http://fusepool.eu/ontologies/annostore#acceptedPredicate"),
                            null);
                    while (itPredicate.hasNext()) {
                        Triple predicateTriple = itPredicate.next();
                        booldisplay = "1";
                        log.info("params.put(\"booldisplay\", "+booldisplay+")");
                        String predicateString = predicateTriple.getObject().toString().replace("<", "").replace(">", "");
                        log.info("params.put(\"predicate\", "+predicateString+")");
                        params.put("predicate", predicateString);
                    }
                    if (booldisplay.equals("0")) {
                        itPredicate = annostore.filter((NonLiteral)bodyTriple.getObject(),
                            new UriRef("http://fusepool.eu/ontologies/annostore#dismissedPredicate"),
                            null);
                        while (itPredicate.hasNext()) {
                            Triple predicateTriple = itPredicate.next();
                            log.info("params.put(\"booldisplay\", "+booldisplay+")");
                            String predicateString = predicateTriple.getObject().toString().replace("<", "").replace(">", "");
                            log.info("params.put(\"predicate\", "+predicateString+")");
                            params.put("predicate", predicateString);
                        }
                    }
                    params.put("booldisplay", booldisplay);
                }
                
                Iterator<Triple> itDocument = annostore.filter(e.getTriple().getSubject(),
                        new UriRef("http://www.w3.org/ns/oa#hasTarget"),
                        null);
                NonLiteral documentNonLiteral = null;
                while(itDocument.hasNext()) {
                    Triple documentTriple = itDocument.next();
                    /**
                     * Let's avoid DelayedNotificator Exceptions...
                     */
                    documentNonLiteral = (NonLiteral)documentTriple.getObject();
                }
                /**
                * Get document content.
                */
                Iterator<Triple> itContent = contentstore.filter(documentNonLiteral,
                        new UriRef("http://rdfs.org/sioc/ns#content"),
                        null);
                while (itContent.hasNext()) {
                    String content = itContent.next().getObject().toString().replace("\n", "");
                    log.info("CONTENT FOUND:");
                    log.info(content);
                     params.put("doc", content);
                }
               /**
                * Get document type.
                */
                Iterator<Triple> itType = contentstore.filter(documentNonLiteral,
                        new UriRef("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                        null);
                while (itType.hasNext()) {
                    Triple typeTriple = itType.next();
                    if (typeTriple.getObject().toString().equals("<http://www.patexpert.org/ontologies/pmo.owl#PatentPublication>")) {
                        log.info("I got a patent !");
                        params.put("doctype", typeTriple.getObject().toString().replace("<", "").replace(">", ""));
                    }
                    //<http://www.patexpert.org/ontologies/pmo.owl#PatentPublication>
                    /**
                     * TODO: here, something with "doctype".
                     */
                }
                updateModels(params);
            }
        }
    }
    
    private LUP55.Listener5_5 listener5_5;
    private FilterTriple filter5_5;
    private long delay5_5;
    
    @Reference
    private ClientEngine openXeroxClient;
    
    private RestEngine clientPush;
    private RestEngine clientPull;
    
    @Reference
    private HubEngine predictionHub;
    
    /**
     * HashMap containing a predicate linked to a score.
     */
    HashMap<String,Double> predicates = new HashMap<String,Double>();
    List<String> predicateList;
    
    
    @Activate
    private void activate() {
        /**
         * 1.) Fetch graphs
         * 2.) Create files
         * 3.) ETL
         */
        // 1.) Fetch graphs
        annostore = tcManager.getMGraph(ANNOTATION_GRAPH_NAME);
        contentstore = tcManager.getMGraph(CONTENT_GRAPH_NAME);
        // 2.) Instanciating any listener, filter, delay and web access needed
        this.listener5_5 = new LUP55.Listener5_5();
        this.filter5_5 = new FilterTriple(
                null ,
                null ,
                new UriRef("http://fusepool.eu/ontologies/annostore#AdaptiveLayoutAnnotation"));
        this.delay5_5 = 5000;
        this.clientPush = openXeroxClient.getPush();
        this.clientPull = openXeroxClient.getPull();
        this.predicateList = new ArrayList<String>();
        this.predictionHub.register(this);
    }
    
    @Deactivate
    private void deactivator() {
        this.predictionHub.unregister(this);
    }

    public String getName() {
        return "LUP55";
    }

    public String getDescription() {
        return "LUP module providing services for Adaptative Layout T5.5 task.";
    }
    
    public GraphListener getListener() {
        return this.listener5_5;
    }

    public FilterTriple getFilter() {
        return this.filter5_5;
    }

    public long getDelay() {
        return this.delay5_5;
    }
    
    /**
     * Nothing to do since the the model is not on the platform.
     */
    public void save() {}
    public void load() {}

    public void updateModels(HashMap<String, String> params) {
        log.info("updateModels()");
        try {
            log.info(this.clientPush.doPost("https://fpt55.services.open.xerox.com/additem/", params));
            if (!predicateList.contains(params.get("predicate"))) {
                predicateList.add(params.get("predicate"));
                this.clientPush.doGet("https://fpt55.services.open.xerox.com/dolearn/5/");
            }
        } catch (Exception ex) {
            log.error("error accessing fpt55.services.open.xerox.com/additem", ex);
        }
    }
    
    public String predict(HashMap<String, String> params) {
        try {
            HashMap<String, String> paramsTmp = new HashMap<String, String>();
            String user = params.get("user");
            String document = params.get("document");
            String query = params.get("query");
            paramsTmp.put("user", user);
            paramsTmp.put("query", query);
            
            Iterator<Triple> itContent = contentstore.filter(new UriRef(document),
                    new UriRef("http://rdfs.org/sioc/ns#content"),
                    null);
            String content = "";
            while (itContent.hasNext()) {
                Triple contentTriple = itContent.next();
                content = contentTriple.getObject().toString().replace("\n", "");
            }
            Iterator<Triple> itType = contentstore.filter(new UriRef(document),
                    new UriRef("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                    null);
            String docType = "";
            while (itType.hasNext()) {
                Triple typeTriple = itType.next();
                if (typeTriple.getObject().toString().equals("<http://www.patexpert.org/ontologies/pmo.owl#PatentPublication>")) {
                        log.info("I got a patent !");
                        docType = typeTriple.getObject().toString().replace("\n", "").replace("<","").replace(">","");
                    }
            }
            paramsTmp.put("doc", content);
            paramsTmp.put("doctype", docType);
            
            /**
             * Fetch the predicates.
             */
            predicates = new HashMap<String,Double>();
            Iterator<Triple> itPredicates = contentstore.filter(new UriRef(document),
                    null,
                    null);
            log.info("Predicate for document " + document + ":");
            while (itPredicates.hasNext()) {
                Triple predicateTriple = itPredicates.next();
                String newPredicate = predicateTriple.getPredicate().toString().replace("<","").replace(">", "");
                if (!predicates.containsKey(newPredicate)) {
                    if (!newPredicate.contains("urn:x-temp")) {
                        predicates.put(newPredicate, 1.0);
                        log.info("  - " + newPredicate);
                    }
                }
            }
            
            log.info("I got called by the Prediction Hub with parameters: " + user + ", " + document + ", " + query);
            String stringResult = this.clientPull.doPost("https://fpt55.services.open.xerox.com/dopredictallpredicate/", paramsTmp);
            log.info(stringResult);
            JSONObject jsonResult = new JSONObject(stringResult);
            if (!jsonResult.has("predicates")) {
                String error = jsonResult.getString("error");
                if (error.contains("learning")) {
                    log.info("LEARNING");
                    this.clientPush.doGet("https://fpt55.services.open.xerox.com/dolearn/5/");
                }
                String result = "";
                for (String predicate : predicates.keySet()) {
                    result += predicate + "__" + predicates.get(predicate) + "##";
                }
                if (result.length() > 2)
                    result = result.substring(0, result.length()-2);
                log.info(result);
                return result;
            }
            log.info("PREDICTING");
            jsonResult = (JSONObject) jsonResult.get("predicates");
            
            Iterator<String> it = jsonResult.keys();
            while (it.hasNext()) {
                String newPredicate = it.next();
                Double newConfidence = jsonResult.getDouble(newPredicate);
                predicates.put(newPredicate, newConfidence);
            }
            
            String result = "";
            for (String predicate : predicates.keySet()) {
                result += predicate + "__" + predicates.get(predicate) + "##";
            }
            if (result.length() > 2)
                result = result.substring(0, result.length()-2);
            
            log.info("Predicate list sent:");
            log.info(result);
            return result;
            
        } catch (Exception ex) {
            log.error("Error", ex);
            String result = "";
            for (String predicate : predicates.keySet()) {
                result += predicate + "__" + predicates.get(predicate) + "##";
            }
            if (result.length() > 2)
                result = result.substring(0, result.length()-2);
            
            return result;
        }
    }
}
