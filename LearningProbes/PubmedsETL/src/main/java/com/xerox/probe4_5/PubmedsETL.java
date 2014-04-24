package com.xerox.probe4_5;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Iterator;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.LockableMGraph;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.event.FilterTriple;
import org.apache.clerezza.rdf.core.event.GraphListener;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the predictor engine service
 * @author aczerny
 */
@Component(immediate = true, metatype = true)
@Service(Object.class)
@Property(name = "javax.ws.rs", boolValue = true)
@Path("pubmed/etl")
public class PubmedsETL
{
    /**
     * Using slf4j for logging
     */
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(PubmedsETL.class);
    /**
     * Accessing the TripleCollection Manager via the OSGi framework
     */
    @Reference
    private TcManager tcManager;
    @Reference(target = "(name=urn:x-localinstance:/fusepool/annotation.graph)")
    private LockableMGraph annoGraph; 
    @Reference(target = "(name=urn:x-localinstance:/content.graph)")
    private LockableMGraph ecsGraph; 
    /**
     * Uri Reference to access the AnnoStore (Listener)
     */
    private UriRef ANNOTATION_GRAPH_NAME = new UriRef("urn:x-localinstance:/fusepool/annotation.graph");
    private MGraph annostore;
    private UriRef CONTENTSTORE_GRAPH_NAME = new UriRef("urn:x-localinstance:/content.graph");
    private MGraph contentstore;
    
    @Reference
    private Serializer serializer;
    private File relationsFilePubmed;
    
    
    private void ETLmodReto(Iterator<Triple> it, OutputStream relationsWriter) throws IOException {
        MGraph resultGraph = new SimpleMGraph(); 
        while (it.hasNext()) {
            Triple triple = it.next();
            NonLiteral docURINonLiteral = (NonLiteral)triple.getSubject();
            //resultGraph.addAll(new GraphNode(docURINonLiteral, contentstore).getNodeContext());
            Iterator<Triple> itTitles = contentstore.filter(docURINonLiteral,
                        new UriRef("http://purl.org/dc/terms/title"),
                        null);
                while (itTitles.hasNext()) {
                    Triple titleTriple = itTitles.next();
                    resultGraph.add(new TripleImpl(docURINonLiteral, 
                            new UriRef("http://myon/hasTitle"),
                            titleTriple.getObject()));
                }
        }
        serializer.serialize(relationsWriter, resultGraph, SupportedFormat.TURTLE);
    }
    
    
    private void ETL(Iterator<Triple> it, BufferedWriter relationsWriter) throws IOException {
            while (it.hasNext()) {
                try {
                    Triple triple = it.next();
                    NonLiteral docURINonLiteral = (NonLiteral)triple.getSubject();
                    String docURI = triple.getSubject().toString();
                    /**
                     * x.) Fetch Titles (ontology: <http://purl.org/dc/terms/title>)
                     * y.) Fetch Abstract (ontology: <http://purl.org/dc/terms/abstract>)
                     * z.) Fetch Content (ontology: <http://rdfs.org/sioc/ns#content>)
                     *  z.1.) Remove ^^<http://www.w3.org/2001/XMLSchema#string>
                     *  z.2.) Remove ""
                     * a.) Fetch Author(s) (ontology: <http://www.patexpert.org/ontologies/pmo.owl#inventor>)
                     * b.) Fetch Labels (ontology: <http://www.patexpert.org/ontologies/pmo.owl#classifiedAs>)
                     * c.) Fetch Organisation (ontology: ???)
                     */
                    // x.) Fetch Titles
                    Iterator<Triple> itTitles = contentstore.filter(docURINonLiteral,
                            new UriRef("http://purl.org/dc/terms/title"),
                            null);
                    while (itTitles.hasNext()) {
                        Triple titleTriple = itTitles.next();
                        String title = titleTriple.getObject().toString();
                        relationsWriter.write(docURI + " <http://myon/hasTitle> " + title + " .\n");
                    }
                    // y.) Fetch Abstract
                    Iterator<Triple> itAbstract = contentstore.filter(docURINonLiteral,
                            new UriRef("http://purl.org/dc/terms/abstract"),
                            null);
                    while (itAbstract.hasNext()) {
                        Triple abstractTriple = itAbstract.next();
                        String abstracts = abstractTriple.getObject().toString();
                        if (abstracts.contains("^^")) {
                            abstracts = abstracts.substring(0, abstracts.indexOf("^^"));
                        }
//                        abstracts = abstracts.replace("\"", "\\\"");
                        abstracts = abstracts.replace("\n", " ");
                        abstracts = abstracts.replace("\t", " ");
//                        abstracts = "\"" + abstracts + "\"";
                        relationsWriter.write(docURI + " <http://myon/hasAbstract> " + abstracts + " .\n");
                    }
                    // z.) Fetch Content
                    Iterator<Triple> itContent = contentstore.filter(docURINonLiteral,
                            new UriRef("http://rdfs.org/sioc/ns#content"),
                            null);
                    while (itContent.hasNext()) {
                        Triple contentTriple = itContent.next();
                        String content = contentTriple.getObject().toString();
                        if (content.contains("^^")) {
                            content = content.substring(0, content.indexOf("^^"));
                        }
//                        content = content.replace("\"", "\\\"");
                        content = content.replace("\t", " ");
                        content = content.replace("\n", " ");
//                        content = "\"" + content + "\"";
    //                    if (content.length() != 0 ) {
    //                        content = content.substring(1, content.length()-1);
    //                        content = content.substring(0, content.length()-3);
    //                    }
                        content = content.trim();
                        relationsWriter.write(docURI + " <http://myon/hasContent> " + content + " .\n");
                    }
                    // a.) Fetch Author(s) & Organisations
                    Iterator<Triple> itInventor = contentstore.filter(docURINonLiteral,
                            new UriRef("http://purl.org/dc/elements/1.1/subject"),
                            null);
                    while (itInventor.hasNext()) {
                        /**
                         * For each "Inventor" check the rdfs:type to see if it's an author
                         */
                        Triple tripleInventor = itInventor.next();
                        Resource Inventor = tripleInventor.getObject();
                        String inventorString = Inventor.toString();

                        // 1.) First loop : if (human)
                        Iterator<Triple> itInventorType = contentstore.filter((NonLiteral)Inventor,
                                new UriRef("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                                new UriRef("http://xmlns.com/foaf/0.1/Person"));
                        if (itInventorType.hasNext()) {
                            /**
                             * Here, we replace the name of the person by its URI
                             */
                            relationsWriter.write(docURI + " <http://myon/hasAuthor> " + inventorString + " .\n");
    //                        Iterator<Triple> itInventorTypeHuman = contentstore.filter((NonLiteral)Inventor,
    //                                new UriRef("http://www.w3.org/2000/01/rdf-schema#label"),
    //                                null);
    //                        while (itInventorTypeHuman.hasNext()) {
    //                            String authorName = itInventorTypeHuman.next().getObject().toString();
    //                            relationsWriter.write(docURI + " <http://myon/hasAuthor> " + authorName + " .\n");
    //                        }
                        } else {
                            // 2.) Second loop : if not (human)
                            /**
                             * Here, we replace the name of the organisation by its URI
                             */
                            relationsWriter.write(docURI + " <http://myon/hasOrg> " + inventorString + " .\n");
    //                        Iterator<Triple> itInventorTypeOrg = contentstore.filter((NonLiteral)Inventor,
    //                                new UriRef("http://www.w3.org/2000/01/rdf-schema#label"),
    //                                null);
    //                        while (itInventorTypeOrg.hasNext()) {
    //                            String orgName = itInventorTypeOrg.next().getObject().toString();
    //                            relationsWriter.write(docURI + " <http://myon/hasOrg> " + orgName + " .\n");
    //                        }
                        }
                    }// a.bis.) Fetch Author(s) & Organisations (WITH 1.0 ONTOLOGY)
                    itInventor = contentstore.filter(docURINonLiteral,
                            new UriRef("http://purl.org/dc/terms/subject"),
                            null);
                    while (itInventor.hasNext()) {
                        /**
                         * For each "Inventor" check the rdfs:type to see if it's an author
                         */
                        Triple tripleInventor = itInventor.next();
                        Resource Inventor = tripleInventor.getObject();
                        String inventorString = Inventor.toString();

                        // 1.) First loop : if (human)
                        Iterator<Triple> itInventorType = contentstore.filter((NonLiteral)Inventor,
                                new UriRef("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                                new UriRef("http://xmlns.com/foaf/0.1/Person"));
                        if (itInventorType.hasNext()) {
                            Iterator<Triple> itInventorTypeHuman = contentstore.filter((NonLiteral)Inventor,
                                    new UriRef("http://www.w3.org/2000/01/rdf-schema#label"),
                                    null);
                            while (itInventorTypeHuman.hasNext()) {
                                String authorName = itInventorTypeHuman.next().getObject().toString();
                                relationsWriter.write(docURI + " <http://myon/hasAuthor> " + authorName + " .\n");
                            }
                        } else {
                            // 2.) First loop : if (human)
                            Iterator<Triple> itInventorTypeOrg = contentstore.filter((NonLiteral)Inventor,
                                    new UriRef("http://www.w3.org/2000/01/rdf-schema#label"),
                                    null);
                            while (itInventorTypeOrg.hasNext()) {
                                String orgName = itInventorTypeOrg.next().getObject().toString();
                                relationsWriter.write(docURI + " <http://myon/hasOrg> " + orgName + " .\n");
                            }
                        }
                    }
                    // b.) Fetch Label(s)
                    Iterator<Triple> itLabels = contentstore.filter(docURINonLiteral,
                            new UriRef("http://www.patexpert.org/ontologies/pmo.owl#classifiedAs"),
                            null);
                    while (itLabels.hasNext()) {
                        Triple tripleLabel = itLabels.next();
                        Iterator<Triple> itLabelCode = contentstore.filter((NonLiteral)tripleLabel.getObject(),
                                new UriRef("http://www.w3.org/2004/02/skos/core#notation"),
                                null);
                        while (itLabelCode.hasNext()) {
                            Triple tripleLabelCode = itLabelCode.next();
                            String labelCode = tripleLabelCode.getObject().toString();
    //                        log.info("adding relation " + docURI + " hasLabel " + labelCode + "to file.");
                            relationsWriter.write(docURI + " <http://myon/hasLabel> " + tripleLabelCode.getObject().toString() + " .\n");
                        }
                    }
                } catch(Exception e) {
                    log.error("Exception caught, " + e);
                }
            }
    }


    @GET
    @Produces("text/turtle")
    public InputStream getRelationsFilePubmed() {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<InputStream>() {
                public InputStream run() throws Exception {
                    return new FileInputStream(relationsFilePubmed);
                }
            });
        } catch (PrivilegedActionException ex) {
            throw new WebApplicationException(ex.getException(), Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Activate
    private void activate(BundleContext bc) {
        /**
         * 1.) Fetch graphs
         * 2.) Create files
         * 3.) ETL
         */
        // 1.) Fetch graphs
        annostore = tcManager.getMGraph(ANNOTATION_GRAPH_NAME);
        contentstore = tcManager.getMGraph(CONTENTSTORE_GRAPH_NAME);
        // 2.) Create files
        BufferedWriter relationsPubmedsWriter = null;
        try {
            // Create dump files for (entities / relations)
            relationsFilePubmed = bc.getDataFile("pubmed_RELATIONS.ttl");

            // This will output the full path where the file will be written to...
            log.info(relationsFilePubmed.getCanonicalPath());
            
            // 3.) ETL
            relationsPubmedsWriter = new BufferedWriter(new FileWriter(relationsFilePubmed));
            
            // 3.2) PubMed ETL
            Iterator<Triple> itPubMed = contentstore.filter(null,
                    new UriRef("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                    new UriRef("http://purl.org/ontology/bibo/Document"));
            this.ETL(itPubMed, relationsPubmedsWriter);
            
            
//            // 3.1) Patent ETL
//            Iterator<Triple> itPatent = contentstore.filter(null,
//                    new UriRef("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
//                    new UriRef("http://www.patexpert.org/ontologies/pmo.owl#PatentPublication"));
//            this.ETL(itPatent, relationsPatentsWriter);
            
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // Close the writer regardless of what happens...
                relationsPubmedsWriter.close();
            } catch (Exception e) {
            }
        }
    }
    
    @Deactivate
    private void deactivator() {
        // Nothing to do here
    }
}