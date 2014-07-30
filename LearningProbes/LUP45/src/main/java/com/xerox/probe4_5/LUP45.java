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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.clerezza.rdf.core.Literal;

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
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.clerezza.rdf.utils.RdfList;
import org.apache.clerezza.rdf.utils.graphnodeprovider.GraphNodeProvider;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
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
    @Reference(target = "(name=urn:x-localinstance:/fusepool/annotation.graph)")
    private LockableMGraph annoGraph; 
    
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

    public String middleViewService(HashMap<String, String> params) {
        try {
            String search = params.get("search");
            Integer offset = Integer.parseInt(params.get("offset"));
            Integer maxFacets = Integer.parseInt(params.get("maxFacets"));
            Integer items = Integer.parseInt(params.get("items"));
            /**
             * Watch out for the: subject=http://platform.fusepool.info/code/country/JP for instance !!
             */
            HashMap<String, String> paramsOpenXerox = new HashMap<String, String>();
            paramsOpenXerox.put("query", search);
            JSONObject jsonResultObject = new JSONObject(clientPull.doPost("https://psparql.services.open.xerox.com/getauthors/", paramsOpenXerox));
            JSONArray jsonResultArray = jsonResultObject.getJSONArray("AuthorList");
            log.info(jsonResultObject.toString());
            /**
            * ... Send things and get back <LIST OF URIS> from OpenXerox
            * TESTING WITH : <http://fusepool.info/id/1c396582-16e9-4bdf-b497-dc6cbe4a9810>
            * AND :<http://fusepool.info/id/688d7156-09d7-452f-9c64-4ee9676d34bb>
            */
            ArrayList<UriRef> peopleList = new ArrayList<UriRef>();
            for (Integer index = 0 ; index < jsonResultArray.length() ; index++) {
                UriRef newUri = new UriRef(jsonResultArray.getString(index).substring(1, jsonResultArray.getString(index).length()-1));
                log.info("looking for people with uri: " + newUri.toString());
                peopleList.add(newUri);
            }

            HashMap<UriRef, Integer> facetsIndex = new HashMap<UriRef, Integer>();
            final Integer nbResource = peopleList.size();
            MGraph resultGraph = new SimpleMGraph();
            NonLiteral listResource = new BNode();
            RdfList contentList = new RdfList(listResource, resultGraph);
            log.info("peopleList size: " + peopleList.size());
            for (UriRef foundResource: peopleList) {
                //                final UriRef foundResource = new UriRef("http://fusepool.info/id/1c396582-16e9-4bdf-b497-dc6cbe4a9810");
                final GraphNode peopleNode = graphNodeProvider.getLocal(foundResource);
                log.info("graphNodeProvider.getLocal(): " + peopleNode.toString());
                
                /**
                 * HERE we check if the context returns the country indicated by the subject.
                 * 
                 */
                resultGraph.addAll(peopleNode.getNodeContext());

                Iterator<Triple> itAddressResources =
                    resultGraph.filter(foundResource, new UriRef("http://schema.org/address"), null);
                log.info("trying to find some address...");
                while (itAddressResources.hasNext()) {
                    Resource addressResource = itAddressResources.next().getObject();
                    log.info("found address: " + addressResource.toString());
                    GraphNode addressNode = graphNodeProvider.getLocal((UriRef)addressResource);
                    resultGraph.addAll(addressNode.getNodeContext());
                    /**
                     * Considering FACETS
                     */
                    Iterator<Triple> itCountryResources =
                        resultGraph.filter((NonLiteral) addressResource, new UriRef("http://schema.org/addressCountry"), null);
                    log.info("beginning to iterate over countries...");
                    while (itCountryResources.hasNext()) {
                        UriRef countryURI = (UriRef) itCountryResources.next().getObject();
                        log.info("iterating over countries: " + countryURI);
                        if (facetsIndex.containsKey(countryURI)) {
                            log.info("incrementing facet count for country: " + countryURI);
                            Integer count = facetsIndex.get(countryURI);
                            facetsIndex.remove(countryURI);
                            facetsIndex.put(countryURI, count+1);
                        } else {
                            log.info("intializing facet for country : " + countryURI);
                            facetsIndex.put(countryURI, 1);
                        }
                    }
                }

                contentList.add(foundResource);

            }
            GraphNode contentStoreView = new GraphNode(new UriRef("http://platform.fusepool.info/ecs/?search="+search+"&offset="+offset+"&maxFacets="+maxFacets+"&items="+items), resultGraph);
            contentStoreView.addProperty(RDF.type, ECS.ContentStoreView);
            contentStoreView.addPropertyValue(ECS.contentsCount, nbResource);
            contentStoreView.addProperty(ECS.contents, listResource);
            // Adding Facets for organizations (IMPOSSIBLE FOR NOW)
            // Adding Facets for countries (POSSIBLE)
            for (UriRef key : facetsIndex.keySet()) {
                /*
                    BNode facetResource = new BNode();
                    GraphNode facetNode = new GraphNode(facetResource, resultGraph);
                    node.addProperty(ECS.facet, facetResource);
                    UriRef facetValue = new UriRef(entry.getKey());
                    Integer facetCount = entry.getValue();
                    facetNode.addProperty(ECS.facetValue, facetValue);
                    addResourceDescription(facetValue, resultGraph);
                    facetNode.addPropertyValue(ECS.facetCount, facetCount);
                 */
                Integer facetCount = facetsIndex.get(key);
                log.info("adding facet: " + key.toString() + " with count: " + facetCount);
                BNode facetResource = new BNode();
                GraphNode facetNode = new GraphNode(facetResource, resultGraph);
                
                contentStoreView.addProperty(ECS.facet, facetResource);
                
                facetNode.addProperty(ECS.facetValue, key);
                facetNode.addPropertyValue(ECS.facetCount, facetCount);
                /**
                 * I'm afraid I will have to workaround the lack of data about countries.
                 * Let's just try with rdf-label ontology.
                 * EDIT: It works, but the label could be better-looking.
                 */
                GraphNode countryNode = new GraphNode(key, resultGraph);
                countryNode.addProperty(RDF.type, OWL.Class);
                countryNode.addPropertyValue(new UriRef("http://www.w3.org/2000/01/rdf-schema#label"), key.toString());
            }
            //            contentStoreView.addProperty(ECS.facet, listResource);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            serializer.serialize(baos, resultGraph, SupportedFormat.TURTLE);

            log.info("\n" +baos.toString());

            return new String(baos.toByteArray(), "utf-8");
        } catch (Exception ex) {
            log.error("Error", ex);
            return "__error__";
        }
    }
    
        public String previewViewService(HashMap<String, String> params) {
        try {
            UriRef entityUri = new UriRef(params.get("entityURI"));
            MGraph resultGraph = new SimpleMGraph();
            final GraphNode entityNode = graphNodeProvider.getLocal(entityUri);
            /**
             * 1.) Add all context of given entityUri
             * 2.) Add all context of address
             * 3.) Add all context of documents
             */
            // TODO: filter context of document
            /**
             * 1.)
             */
            resultGraph.addAll(entityNode.getNodeContext());
            Iterator<Triple> itAddressResources =
                            resultGraph.filter(entityUri, new UriRef("http://schema.org/address"), null);
            log.info("trying to find some address...");
            while (itAddressResources.hasNext()) {
                Resource addressResource = itAddressResources.next().getObject();
                log.info("found address: " + addressResource.toString());
                GraphNode addressNode = graphNodeProvider.getLocal((UriRef)addressResource);
                /**
                 * 2.)
                 */
                resultGraph.addAll(addressNode.getNodeContext());
            }

            Iterator<Triple> itPatentsResources =
                    resultGraph.filter(entityUri, new UriRef("http://www.patexpert.org/ontologies/pmo.owl#inventorOf"), null);
            log.info("trying to find some patents...");
            while (itPatentsResources.hasNext()) {
                Resource patentResource = itPatentsResources.next().getObject();
                log.info("found patent: " + patentResource.toString());
                GraphNode patentsNode = graphNodeProvider.getLocal((UriRef)patentResource);
                /**
                 * 3.)
                 */
                resultGraph.addAll(patentsNode.getNodeContext());
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            serializer.serialize(baos, resultGraph, SupportedFormat.TURTLE);

            log.info("\n" +baos.toString());

            return new String(baos.toByteArray(), "utf-8");
        } catch (Exception ex) {
            log.error("Error", ex);
            return "__error__";
        }
    }
    
    public String predict(HashMap<String, String> params) {
        try {
            /**
             * 1.) Fetch user-query
             * 2.) Send to OpenXerox through PULL bundle
             * 3.) Create graph using URIs
             */
            for (String key: params.keySet()) {
                log.info("key: " + key);
            }
            String prediction = "__error__";
            if (params.keySet().contains("search")) {
                log.info("middleViewService");
                prediction = middleViewService(params);
            } else if (params.keySet().contains("entityURI")) {
                log.info("previewViewService");
                prediction = previewViewService(params);
            } else {
                log.info("defaultService");
                String search = params.get("search");
                Integer offset = Integer.parseInt(params.get("offset"));
                Integer maxFacets = Integer.parseInt(params.get("maxFacets"));
                Integer items = Integer.parseInt(params.get("items"));

                HashMap<String, String> paramsOpenXerox = new HashMap<String, String>();
                paramsOpenXerox.put("query", search);
                JSONObject jsonResultObject = new JSONObject(clientPull.doPost("https://psparql.services.open.xerox.com/getauthors/", paramsOpenXerox));
                JSONArray jsonResultArray = jsonResultObject.getJSONArray("AuthorList");
                log.info(jsonResultObject.toString());
                /**
                 * ... Send things and get back <LIST OF URIS> from OpenXerox
                 * TESTING WITH : <http://fusepool.info/id/1c396582-16e9-4bdf-b497-dc6cbe4a9810>
                 * AND :<http://fusepool.info/id/688d7156-09d7-452f-9c64-4ee9676d34bb>
                 */
                ArrayList<UriRef> peopleList = new ArrayList<UriRef>();
                for (Integer index = 0 ; index < jsonResultArray.length() ; index++) {
                    UriRef newUri = new UriRef(jsonResultArray.getString(index).substring(1, jsonResultArray.getString(index).length()-1));
                    log.info("looking for people with uri: " + newUri.toString());
                    peopleList.add(newUri);
                }
                /**
                 * Test persons
                 */
    //            peopleList.add(new UriRef("http://fusepool.info/id/1c396582-16e9-4bdf-b497-dc6cbe4a9810"));
    //            peopleList.add(new UriRef("http://fusepool.info/id/688d7156-09d7-452f-9c64-4ee9676d34bb"));

                final Integer nbResource = peopleList.size();
                MGraph resultGraph = new SimpleMGraph();
                NonLiteral listResource = new BNode();
                RdfList contentList = new RdfList(listResource, resultGraph);
                log.info("peopleList size: " + peopleList.size());
                for (UriRef foundResource: peopleList) {
    //                final UriRef foundResource = new UriRef("http://fusepool.info/id/1c396582-16e9-4bdf-b497-dc6cbe4a9810");
                    final GraphNode node = graphNodeProvider.getLocal(foundResource);
                    log.info("graphNodeProvider.getLocal(): " + node.toString());
                    resultGraph.addAll(node.getNodeContext());

                    Iterator<Triple> itAddressResources =
                            resultGraph.filter(foundResource, new UriRef("http://schema.org/address"), null);
                    log.info("trying to find some address...");
                    while (itAddressResources.hasNext()) {
                        Resource addressResource = itAddressResources.next().getObject();
                        log.info("found address: " + addressResource.toString());
                        GraphNode addressNode = graphNodeProvider.getLocal((UriRef)addressResource);
                        resultGraph.addAll(addressNode.getNodeContext());
                    }

                    Iterator<Triple> itPatentsResources =
                            resultGraph.filter(foundResource, new UriRef("http://www.patexpert.org/ontologies/pmo.owl#inventorOf"), null);
                    log.info("trying to find some patents...");
                    while (itPatentsResources.hasNext()) {
                        Resource patentResource = itPatentsResources.next().getObject();
                        log.info("found patent: " + patentResource.toString());
                        GraphNode patentsNode = graphNodeProvider.getLocal((UriRef)patentResource);

                        /**
                         * Reto's code
                         */
    //                    Iterator<Literal> itLiteral = patentsNode.getObjectNodes(new UriRef("http://www.patexpert.org/ontologies/pmo.owl#inventorOf")).next().getLiterals(new UriRef("http://purl.org/dc/terms/title"));
    //                    while (itLiteral.hasNext()) {
    //                        Literal titleLiteral = itLiteral.next();
    //                        log.info("RETO'S CODE: " + titleLiteral.toString());
    //                    }

                        resultGraph.addAll(patentsNode.getNodeContext());

                    }
                    //Adding Facets for organizations

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
                
                prediction = new String(baos.toByteArray(), "utf-8");
            }
            
            return prediction;
            
//            String search = params.get("search");
//            Integer offset = Integer.parseInt(params.get("offset"));
//            Integer maxFacets = Integer.parseInt(params.get("maxFacets"));
//            Integer items = Integer.parseInt(params.get("items"));
//            
//            HashMap<String, String> paramsOpenXerox = new HashMap<String, String>();
//            paramsOpenXerox.put("query", search);
//            JSONObject jsonResultObject = new JSONObject(clientPull.doPost("https://psparql.services.open.xerox.com/getauthors/", paramsOpenXerox));
//            JSONArray jsonResultArray = jsonResultObject.getJSONArray("AuthorList");
//            log.info(jsonResultObject.toString());
//            /**
//             * ... Send things and get back <LIST OF URIS> from OpenXerox
//             * TESTING WITH : <http://fusepool.info/id/1c396582-16e9-4bdf-b497-dc6cbe4a9810>
//             * AND :<http://fusepool.info/id/688d7156-09d7-452f-9c64-4ee9676d34bb>
//             */
//            ArrayList<UriRef> peopleList = new ArrayList<UriRef>();
//            for (Integer index = 0 ; index < jsonResultArray.length() ; index++) {
//                UriRef newUri = new UriRef(jsonResultArray.getString(index).substring(1, jsonResultArray.getString(index).length()-1));
//                log.info("looking for people with uri: " + newUri.toString());
//                peopleList.add(newUri);
//            }
//            /**
//             * Test persons
//             */
////            peopleList.add(new UriRef("http://fusepool.info/id/1c396582-16e9-4bdf-b497-dc6cbe4a9810"));
////            peopleList.add(new UriRef("http://fusepool.info/id/688d7156-09d7-452f-9c64-4ee9676d34bb"));
//            
//            final Integer nbResource = peopleList.size();
//            MGraph resultGraph = new SimpleMGraph();
//            NonLiteral listResource = new BNode();
//            RdfList contentList = new RdfList(listResource, resultGraph);
//            log.info("peopleList size: " + peopleList.size());
//            for (UriRef foundResource: peopleList) {
////                final UriRef foundResource = new UriRef("http://fusepool.info/id/1c396582-16e9-4bdf-b497-dc6cbe4a9810");
//                final GraphNode node = graphNodeProvider.getLocal(foundResource);
//                log.info("graphNodeProvider.getLocal(): " + node.toString());
//                resultGraph.addAll(node.getNodeContext());
//
//                Iterator<Triple> itAddressResources =
//                        resultGraph.filter(foundResource, new UriRef("http://schema.org/address"), null);
//                log.info("trying to find some address...");
//                while (itAddressResources.hasNext()) {
//                    Resource addressResource = itAddressResources.next().getObject();
//                    log.info("found address: " + addressResource.toString());
//                    GraphNode addressNode = graphNodeProvider.getLocal((UriRef)addressResource);
//                    resultGraph.addAll(addressNode.getNodeContext());
//                }
//                
//                Iterator<Triple> itPatentsResources =
//                        resultGraph.filter(foundResource, new UriRef("http://www.patexpert.org/ontologies/pmo.owl#inventorOf"), null);
//                log.info("trying to find some patents...");
//                while (itPatentsResources.hasNext()) {
//                    Resource patentResource = itPatentsResources.next().getObject();
//                    log.info("found patent: " + patentResource.toString());
//                    GraphNode patentsNode = graphNodeProvider.getLocal((UriRef)patentResource);
//
//                    /**
//                     * Reto's code
//                     */
////                    Iterator<Literal> itLiteral = patentsNode.getObjectNodes(new UriRef("http://www.patexpert.org/ontologies/pmo.owl#inventorOf")).next().getLiterals(new UriRef("http://purl.org/dc/terms/title"));
////                    while (itLiteral.hasNext()) {
////                        Literal titleLiteral = itLiteral.next();
////                        log.info("RETO'S CODE: " + titleLiteral.toString());
////                    }
//                    
//                    resultGraph.addAll(patentsNode.getNodeContext());
//                        
//                }
//                //Adding Facets for organizations
//                
//                contentList.add(foundResource);
//
//            }
//            GraphNode contentStoreView = new GraphNode(new UriRef("http://platform.fusepool.info/ecs/?search="+search+"&offset="+offset+"&maxFacets="+maxFacets+"&items="+items), resultGraph);
//            contentStoreView.addProperty(RDF.type, ECS.ContentStoreView);
//            contentStoreView.addPropertyValue(ECS.contentsCount, nbResource);
//            contentStoreView.addProperty(ECS.contents, listResource);
////            contentStoreView.addProperty(ECS.facet, listResource);
//            
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            serializer.serialize(baos, resultGraph, SupportedFormat.TURTLE);
//            
//            log.info("\n" +baos.toString());
//            
//            
//            
//            return new String(baos.toByteArray(), "utf-8");
        } catch (Exception ex) {
            log.error("Error", ex);
            return "__error__";
        }
    }
}
