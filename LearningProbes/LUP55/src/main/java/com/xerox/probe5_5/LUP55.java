package com.xerox.probe5_5;

import com.xerox.services.LUPEngine;
import com.xerox.services.ClientEngine;
import com.xerox.services.HubEngine;
import com.xerox.services.RestEngine;
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
                Resource annotation = annostore.filter(e.getTriple().getSubject(), null, null).next().getSubject();
                Resource target = annostore.filter((NonLiteral)annotation,
                        new UriRef("http://fusepool.eu/ontologies/annostore#hasTarget"),
                        null).next().getObject();
                Resource body = annostore.filter((NonLiteral)annotation,
                        new UriRef("http://fusepool.eu/ontologies/annostore#hasBody"),
                        null).next().getObject();
                /**
                 * Get user.
                 */
                Resource user = annostore.filter((NonLiteral)annotation,
                        new UriRef("http://www.w3.org/ns/oa#annotatedBy"),
                        null).next().getObject();
                String userString = user.toString().replace("<", "").replace(">", "");
                log.info("params.put(\"user\", "+userString+")");
//                params.put("user", userString);
                /**
                 * Get query.
                 */
                Resource query = annostore.filter((NonLiteral)body,
                        new UriRef("http://fusepool.eu/ontologies/annostore#hasQuery"),
                        null).next().getObject();
                String queryString = query.toString().replace("\"", "");
                log.info("params.put(\"query\", "+queryString+")");
//                params.put("query", queryString);
                /**
                 * Get predicate (check whether it is accepted or dismissed).
                 */
                Resource predicate = annostore.filter((NonLiteral)body,
                        new UriRef("http://fusepool.eu/ontologies/annostore#acceptedPredicate"),
                        null).next().getObject();
                if (predicate == null) {
                    predicate = annostore.filter((NonLiteral)body,
                            new UriRef("http://fusepool.eu/ontologies/annostore#dismissedPredicate"),
                            null).next().getObject();
                    log.info("params.put(\"accepted\", \"1\")");
//                    params.put("accepted", "1");
                } else {
                    log.info("params.put(\"accepted\", \"0\")");
//                    params.put("accepted", "0");
                }
                String predicateString = predicate.toString().replace("<", "").replace(">", "");
                log.info("params.put(\"predicate\", "+predicateString+")");
//                params.put("predicate", predicateString);
                /**
                 * Get document content.
                 */
                Iterator<Triple> itContent = contentstore.filter((NonLiteral)target,
                        new UriRef("http://rdfs.org/sioc/ns#content"),
                        null);
                while (itContent.hasNext()) {
                    String content = itContent.next().getObject().toString();
                    log.info("CONTENT FOUND:");
                    log.info(content);
//                    params.put("content", content);
                }
                /**
                 * Get document type.
                 */
                Iterator<Triple> itType = contentstore.filter((NonLiteral)target,
                        new UriRef("http://www.w3.org/2000/01/rdf-schema#type"),
                        null);
                while (itType.hasNext()) {
                    Triple typeTriple = itType.next();
                    log.info(typeTriple.getObject().toString());
                }
//                updateModels(params);
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
    
    @Activate
    private void activate() {
        /**
         * 1.) Fetch graphs
         * 2.) Create files
         * 3.) ETL
         */
        // 1.) Fetch graphs
        annostore = tcManager.getMGraph(ANNOTATION_GRAPH_NAME);
        // 2.) Instanciating any listener, filter, delay and web access needed
        this.listener5_5 = new LUP55.Listener5_5();
        this.filter5_5 = new FilterTriple(
                null ,
                null ,
                new UriRef("http://fusepool.eu/ontologies/annostore#AdaptiveLayoutAnnotation"));
        this.delay5_5 = 5000;
        this.clientPush = openXeroxClient.getPush();
        this.clientPull = openXeroxClient.getPull();
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
    }
    
    public String predict(HashMap<String, String> params) {
        try {
            String user = params.get("user");
            String document = params.get("document");
            String query = params.get("query");
            log.info("I got called by the Prediction Hub with parameters: " + user + ", " + document + ", " + query);
            return "[{\"text\": \"predicate-0\", \"accepted\": false}, {\"text\": \"predicate-1\", \"accepted\": true}, {\"text\": \"predicate-2\", \"accepted\": false}, {\"text\": \"predicate-3\", \"accepted\": true}]";
        } catch (Exception ex) {
            log.error("Error", ex);
            return "__error__";
        }
    }
}
