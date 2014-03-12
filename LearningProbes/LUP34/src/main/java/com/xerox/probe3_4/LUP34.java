package com.xerox.probe3_4;

import com.xerox.services.LUPEngine;
import com.xerox.services.ClientEngine;
import com.xerox.services.HubEngine;
import com.xerox.services.RestEngine;
import java.security.AccessController;
import java.security.PrivilegedAction;
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
    private UriRef CONTENT_GRAPH_NAME = new UriRef("urn:x-localinstance:/content.graph");
    private MGraph annostore;
    private MGraph contentstore;
    
    public String getName() {
        return "LUP34";
    }

    public String getDescription() {
        return "LUP module which provides a set of services for the Labelling T3.4 task.";
    }

    private class Listener3_4 implements GraphListener {

        public void graphChanged(List<GraphEvent> list) {
            log.info("graphChanged");
            for (GraphEvent e : list) {
                HashMap<String,String> params = new HashMap<String, String>();
                Iterator<Triple> itTriple = annostore.filter(e.getTriple().getSubject(), null, null);
                while (itTriple.hasNext()) {
                    Triple newTriple = itTriple.next();
                }
                Resource target = annostore.filter(e.getTriple().getSubject(),
                        new UriRef("http://fusepool.eu/ontologies/annostore#hasTarget"),
                        null).next().getObject();
                
                params.put("docURI", target.toString());
                log.info("docURI: [" + target.toString() + "]");
                
                Resource body = annostore.filter(e.getTriple().getSubject(),
                        new UriRef("http://fusepool.eu/ontologies/annostore#hasBody"),
                        null).next().getObject();
                
//                itTriple = annostore.filter((NonLiteral)body, null, null);
//                while (itTriple.hasNext()) {
//                    Triple newTriple = itTriple.next();
//                }
                itTriple = annostore.filter((NonLiteral)body, new UriRef("http://fusepool.eu/ontologies/annostore#hasNewLabel"), null);
                while (itTriple.hasNext()) {
                    Triple newTriple = itTriple.next();
                    Iterator<Triple> itLabel = annostore.filter((NonLiteral)(newTriple.getObject()),
                            new UriRef("http://www.w3.org/2011/content#chars"),
                            null);
                    while (itLabel.hasNext()) {
                        Triple newTriple2 = itLabel.next();
                        log.info("new label: [" + newTriple2.getObject().toString() + "]");
                        params.put("newLabel", newTriple2.getObject().toString());
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
                        log.info("deleted label: [" + newTriple2.getObject().toString() + "]");
                        params.put("deletedLabel", newTriple2.getObject().toString());
                    }
                }
                itTriple = contentstore.filter((NonLiteral)target,
                        new UriRef("http://rdfs.org/sioc/ns#content"),
                        null);
                String content = itTriple.next().getObject().toString();
                log.info("CONTENT FOUND:");
                log.info(content);
                params.put("content", content);
                updateModels(params);
            }
        }
    }
    
    private LUP34.Listener3_4 listener3_4;
    private FilterTriple filter3_4;
    private long delay3_4;
    
    /**
     * Key: DocURI - Value: DocID, as returned by the OpenXerox service.
     */
    private HashMap<String, Integer> docIDIndex;
    
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
        annostore = tcManager.getMGraph(ANNOTATION_GRAPH_NAME);
        contentstore = tcManager.getMGraph(CONTENT_GRAPH_NAME);
        // 2.) Instanciating any listener, filter, delay and web access needed
        this.listener3_4 = new LUP34.Listener3_4();
        this.filter3_4 = new FilterTriple(
                null ,
                null ,
                new UriRef("http://fusepool.eu/ontologies/annostore#labellingAnnotation"));
        // this.filter3_4 = new FilterTriple(null, null, null);
        this.delay3_4 = 5000;
        this.docIDIndex = new HashMap<String, Integer>();
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
                    new UriRef("http://fusepool.eu/ontologies/annostore#labellingAnnotation"));
            // this.filter3_4 = new FilterTriple(null, null, null);

        }
        return this.filter3_4;
    }
    
    public long getDelay() {
        return this.delay3_4;
    }
    
    public void updateModels(HashMap<String, String> params) {
        try {
            HashMap<String, String> paramsTmp = new HashMap<String, String>();
            /**
             * 1.) Checking whether the document is already present on the OpenXerox DB
             * 2.) If not, we need to call /addtext/ first, then /addlabels/
             * 3.) If it is, just call /addlabels/
             */
            // 1.)
            if (!this.docIDIndex.containsKey(params.get("docURI"))) {
                log.info("Inserting new document to the model !");
                paramsTmp.put("checkboxclassify", " ");
                paramsTmp.put("text", params.get("content"));
                log.info("doPost /fusepool/addtext/");
                String addtextReturn = this.clientPush.doPost("/fusepool/addtext/", paramsTmp);
                /**
                 * We fetch the POST return and turn the JSON into a Integer Doc ID
                 * (to insert into the docIDIndex).
                 */
                JSONObject addtextReturnJSON = new JSONObject(addtextReturn);
                Integer newDocID = addtextReturnJSON.getInt("documentID");
                log.info("Document: [" + params.get("docURI") + "] has documentID: [" + newDocID + "]");
                this.docIDIndex.put(params.get("docURI"), newDocID);
            }
            // 2.)
            /**
             * First, we check if there are spaces; if so, we turn them into "-"
             */
            paramsTmp.clear();
            String newLabel = params.get("newLabel");
            newLabel = newLabel.replace('"', ' ');
            newLabel = newLabel.trim();
            newLabel = newLabel.replace(' ', '-');
            paramsTmp.put("labels", newLabel);
            paramsTmp.put("docID", this.docIDIndex.get(params.get("docURI")).toString());
            log.info("Adding labels: [" + newLabel + "] to documentID: [" + this.docIDIndex.get(params.get("docURI")) + "]");
            log.info("doPost /fusepool/addlabels/");
            this.clientPush.doPost("/fusepool/addlabels/", paramsTmp);
            log.info("docIDIndex contains:");
            for (String docURI : this.docIDIndex.keySet()) {
                log.info(docURI);
            }
        } catch (Exception ex) {
            log.error("Labelling prediction service unavailable");
        }
    }
    
    public String predict(HashMap<String, String> params) {
        try {
            /**
             * 1.) Fetch the content of the docURI object
             * 2.) Call /fusepool/dopredictlabels/
             * 3.) Parse the String returned to replace "-" with " " !
             */
            log.info("LUP34 PREDICT()");
            Iterator<Triple> itTriple = contentstore.filter(new UriRef(params.get("docURI")),
                            new UriRef("http://rdfs.org/sioc/ns#content"),
                            null);
            String content = itTriple.next().getObject().toString();
            // 1.) Get rid of the RDF/XML type
            if (content.contains("^^")) {
                content = content.substring(0, content.indexOf("^^"));
            }
            // 2.) Get rid of the guillemets
            content = content.replace('"', ' ');
            // 3.) Get rid of the line break
            content = content.replace("\n", " ");
            // 4.) Get rid of the tabs
            content = content.replace("\t", " ");
            // 5.) Trim
            content = content.trim();
            
            HashMap<String, String> paramsPrediction = new HashMap<String, String>();
            String result;
            paramsPrediction.put("text", content);
            // 2.) Parsing the JSON returned
            log.info("Content for docURI: " + params.get("docURI") + " : [" + content + "]");
            result = clientPull.doPost("/fusepool/dopredictlabels/", paramsPrediction);
            JSONObject jsonResult = new JSONObject(result);
            // 2. bis) TODO : Check if no label is returned !
            Iterator<String> it = jsonResult.keys();
            String labelList;
            String firstLabel = it.next().trim().replace('-', ' ');
            String firstConfidence = jsonResult.getString(firstLabel);
            /**
             * TO CHANGE THE FORMAT OF THE RETURNED STRING, PLEASE SWITCH COMMENTS
             * ON LINE (288,289) and (295,296)
             */
//            labelList = firstLabel;
             labelList = firstLabel + "__" + firstConfidence;
            while (it.hasNext()) {
                String newLabel = it.next();
                String newConfidence = jsonResult.getString(newLabel);
                newLabel = newLabel.trim();
                newLabel = newLabel.replace('-', ' ');
//                labelList = labelList+";"+newLabel;
                 labelList = labelList+"##"+ newLabel + "__" + newConfidence;
            }
            log.info("Label list sent:");
            log.info(labelList);
            return labelList;
        } catch (Exception ex) {
            log.error("Service /fusepool/dopredictlabels/ unavailable", ex);
        }
        return "__error__";
    }
    
    /**
     * We have to save/load the docID index
     */
    public void save() {}
    public void load() {}
    
}
