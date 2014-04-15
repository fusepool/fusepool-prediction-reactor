package com.xerox.probe4_5;

import com.xerox.services.ClientEngine;
import com.xerox.services.HubEngine;
import com.xerox.services.LUPEngine;
import com.xerox.services.RestEngine;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import eu.fusepool.ecs.ontologies.ECS;
import java.util.ArrayList;
import org.apache.clerezza.rdf.core.BNode;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.event.FilterTriple;
import org.apache.clerezza.rdf.core.event.GraphEvent;
import org.apache.clerezza.rdf.core.event.GraphListener;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.clerezza.rdf.utils.RdfList;
import org.apache.clerezza.rdf.utils.graphnodeprovider.GraphNodeProvider;
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
public class LUP45 implements LUPEngine
{
    /**
     * Using slf4j for logging
     */
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(LUP45.class);
    /**
     * Accessing the TripleCollection Manager via the OSGi framework
     */
    @Reference
    private TcManager tcManager;
    
    @Reference
    private GraphNodeProvider graphNodeProvider;
    
    @Reference
    private Serializer serializer;
    /**
     * Uri Reference to access the AnnoStore (Listener)
     */
    private UriRef ANNOTATION_GRAPH_NAME = new UriRef("urn:x-localinstance:/fusepool/annotation.graph");
    private MGraph annostore;
    
//    private UriRef CONTENT_GRAPH_NAME = new UriRef("urn:x-localinstance:/fusepool/content.graph");
//    private MGraph annostore;
    
    private class Listener4_5 implements GraphListener {

        public void graphChanged(List<GraphEvent> list) {
            log.info("graphChanged");
            for (GraphEvent e : list) {}
        }
    }
    
    private LUP45.Listener4_5 listener4_5;
    private FilterTriple filter4_5;
    private long delay4_5;
    
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
        this.listener4_5 = new LUP45.Listener4_5();
        this.filter4_5 = new FilterTriple(
                null ,
                null ,
                new UriRef("http://fusepool.eu/ontologies/annostore#EntitiesAnnotation"));
        // this.filter3_4 = new FilterTriple(null, null, null);
        this.delay4_5 = 5000;
        this.clientPush = openXeroxClient.getPush();
        this.clientPull = openXeroxClient.getPull();
        this.predictionHub.register(this);
    }
    
    @Deactivate
    private void deactivator() {
        this.predictionHub.unregister(this);
    }

    public String getName() {
        return "LUP45";
    }

    public String getDescription() {
        return "LUP module providing the needed learning-predicting services for "
                + "task T4.5. Returns a list of persons close to the query.";
    }
    
    public GraphListener getListener() {
        return this.listener4_5;
    }

    public FilterTriple getFilter() {
        return this.filter4_5;
    }

    public long getDelay() {
        return this.delay4_5;
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
            /**
             * 1.) Fetch user-query
             * 2.) Craft SPARQL query
             * 3.) Send to OpenXerox through PULL bundle
             * 4.) Create graph using URIs
             */
            for (String key: params.keySet()) {
                log.info("key: " + key);
            }
            String search = params.get("search");
            Integer offset = Integer.parseInt(params.get("offset"));
            Integer maxFacets = Integer.parseInt(params.get("maxFacets"));
            Integer items = Integer.parseInt(params.get("items"));
            /**
             * ... Send things and get back <LIST OF URIS> from OpenXerox
             * TESTING WITH : <http://fusepool.info/id/1c396582-16e9-4bdf-b497-dc6cbe4a9810>
             * AND :<http://fusepool.info/id/688d7156-09d7-452f-9c64-4ee9676d34bb>
             */
            ArrayList<UriRef> peopleList = new ArrayList<UriRef>();
            peopleList.add(new UriRef("http://fusepool.info/id/1c396582-16e9-4bdf-b497-dc6cbe4a9810"));
            peopleList.add(new UriRef("http://fusepool.info/id/688d7156-09d7-452f-9c64-4ee9676d34bb"));
            final Integer nbResource = peopleList.size();
            MGraph resultGraph = new SimpleMGraph();
            NonLiteral listResource = new BNode();
            RdfList contentList = new RdfList(listResource, resultGraph);
            for (UriRef foundResource: peopleList) {
//                final UriRef foundResource = new UriRef("http://fusepool.info/id/1c396582-16e9-4bdf-b497-dc6cbe4a9810");
                final GraphNode node = graphNodeProvider.getLocal(foundResource);
                resultGraph.addAll(node.getNodeContext());

                /**
                 * Adding contexts for all 'addresses'
                 */
                Iterator<Triple> itResources =
                        resultGraph.filter(foundResource, new UriRef("http://schema.org/address"), null);
                log.info("trying to find some resources...");
                while (itResources.hasNext()) {
                    Resource addressResource = itResources.next().getObject();
                    log.info("found address resource: " + addressResource.toString());
                    GraphNode addressNode = graphNodeProvider.getLocal((UriRef)addressResource);
                    resultGraph.addAll(addressNode.getNodeContext());
                }

                contentList.add(foundResource);

            }
            GraphNode contentStoreView = new GraphNode(new UriRef("http://platform.fusepool.info/ecs/?search="+search+"&offset="+offset+"&maxFacets="+maxFacets+"&items="+items), resultGraph);
            contentStoreView.addProperty(RDF.type, ECS.ContentStoreView);
            contentStoreView.addPropertyValue(ECS.contentsCount, nbResource);
            contentStoreView.addProperty(ECS.contents, listResource);
//            contentStoreView.addProperty(ECS.facet, listResource);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            serializer.serialize(baos, resultGraph, SupportedFormat.TURTLE);
            
            log.info("\n" +baos.toString());
            
            
            
            return new String(baos.toByteArray(), "utf-8");
        } catch (UnsupportedEncodingException ex) {
            log.error("Error");
            return "__error__";
        }
    }
}
