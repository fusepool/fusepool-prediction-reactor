package com.xerox.probe4_5;

import com.xerox.services.LUPEngine;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.event.FilterTriple;
import org.apache.clerezza.rdf.core.event.GraphListener;
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
    /**
     * Uri Reference to access the AnnoStore (Listener)
     */
    private UriRef ANNOTATION_GRAPH_NAME = new UriRef("urn:x-localinstance:/fusepool/annotation.graph");
    private MGraph annostore;
    private UriRef CONTENTSTORE_GRAPH_NAME = new UriRef("urn:x-localinstance:/content.graph");
    private MGraph contentstore;
    
    private void ETL(Iterator<Triple> it, BufferedWriter relationsWriter) throws IOException {
            while (it.hasNext()) {
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
                    relationsWriter.write(docURI + " <hasTitle> " + title + " .\n");
                }
                // y.) Fetch Abstract
                Iterator<Triple> itAbstract = contentstore.filter(docURINonLiteral,
                        new UriRef("http://purl.org/dc/terms/abstract"),
                        null);
                while (itAbstract.hasNext()) {
                    Triple abstractTriple = itAbstract.next();
                    String abstracts = abstractTriple.getObject().toString();
                    abstracts = abstracts.replace("\n", " ");
                    relationsWriter.write(docURI + " <hasAbstract> " + abstracts + " .\n");
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
                        content = content.replace("\n", " ");
                    }
                    relationsWriter.write(docURI + " <hasContent> " + content + " .\n");
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
                        Iterator<Triple> itInventorTypeHuman = contentstore.filter((NonLiteral)Inventor,
                                new UriRef("http://www.w3.org/2000/01/rdf-schema#label"),
                                null);
                        while (itInventorTypeHuman.hasNext()) {
                            String authorName = itInventorTypeHuman.next().getObject().toString();
                            relationsWriter.write(docURI + " <hasAuthor> " + authorName + " .\n");
                        }
                    } else {
                        // 2.) First loop : if (human)
                        Iterator<Triple> itInventorTypeOrg = contentstore.filter((NonLiteral)Inventor,
                                new UriRef("http://www.w3.org/2000/01/rdf-schema#label"),
                                null);
                        while (itInventorTypeOrg.hasNext()) {
                            String orgName = itInventorTypeOrg.next().getObject().toString();
                            relationsWriter.write(docURI + " <hasOrg> " + orgName + " .\n");
                        }
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
                            relationsWriter.write(docURI + " <hasAuthor> " + authorName + " .\n");
                        }
                    } else {
                        // 2.) First loop : if (human)
                        Iterator<Triple> itInventorTypeOrg = contentstore.filter((NonLiteral)Inventor,
                                new UriRef("http://www.w3.org/2000/01/rdf-schema#label"),
                                null);
                        while (itInventorTypeOrg.hasNext()) {
                            String orgName = itInventorTypeOrg.next().getObject().toString();
                            relationsWriter.write(docURI + " <hasOrg> " + orgName + " .\n");
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
                        relationsWriter.write(docURI + " <hasLabel> " + tripleLabelCode.getObject().toString() + " .\n");
                    }
                }
            }
    }
    
    @Activate
    private void activate() {
        /**
         * 1.) Fetch graphs
         * 2.) Create files
         * 3.) ETL
         */
        // 1.) Fetch graphs
        annostore = tcManager.getMGraph(ANNOTATION_GRAPH_NAME);
        contentstore = tcManager.getMGraph(CONTENTSTORE_GRAPH_NAME);
        // 2.) Create files
        BufferedWriter relationsPatentsWriter = null;
        BufferedWriter relationsPubmedsWriter = null;
        try {
            // Create dump files for (entities / relations)
            File relationsFilePatents = new File("patent_RELATIONS.ttl");
            File relationsFilePubmed = new File("pubmed_RELATIONS.ttl");

            // This will output the full path where the file will be written to...
            log.info(relationsFilePatents.getCanonicalPath());
            log.info(relationsFilePubmed.getCanonicalPath());
            
            // 3.) ETL
            relationsPatentsWriter = new BufferedWriter(new FileWriter(relationsFilePatents));
            relationsPubmedsWriter = new BufferedWriter(new FileWriter(relationsFilePubmed));
            
            // 3.2) PubMed ETL
            Iterator<Triple> itPubMed = contentstore.filter(null,
                    new UriRef("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                    new UriRef("http://purl.org/ontology/bibo/Document"));
            this.ETL(itPubMed, relationsPubmedsWriter);
            // 3.1) Patent ETL
            Iterator<Triple> itPatent = contentstore.filter(null,
                    new UriRef("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                    new UriRef("http://www.patexpert.org/ontologies/pmo.owl#PatentPublication"));
            this.ETL(itPatent, relationsPatentsWriter);
            
//            while (itPatent.hasNext()) {
//                Triple triplePatent = itPatent.next();
//                String docURI = triplePatent.getSubject().toString();
//                /**
//                 * x.) Fetch Titles (ontology: <http://purl.org/dc/terms/title>)
//                 * y.) Fetch Abstract (ontology: <http://purl.org/dc/terms/abstract>)
//                 * z.) Fetch Content (ontology: <http://rdfs.org/sioc/ns#content>)
//                 *  z.1.) Remove ^^<http://www.w3.org/2001/XMLSchema#string>
//                 *  z.2.) Remove ""
//                 * a.) Fetch Author(s) (ontology: <http://www.patexpert.org/ontologies/pmo.owl#inventor>)
//                 * b.) Fetch Labels (ontology: <http://www.patexpert.org/ontologies/pmo.owl#classifiedAs>)
//                 * c.) Fetch Organisation (ontology: ???)
//                 */
//                // x.) Fetch Titles
//                Iterator<Triple> itTitles = contentstore.filter((NonLiteral)triplePatent.getSubject(),
//                        new UriRef("http://purl.org/dc/terms/title"),
//                        null);
//                while (itTitles.hasNext()) {
//                    Triple titleTriple = itTitles.next();
//                    String title = titleTriple.getObject().toString();
//                    relationsWriter.write(docURI + " <hasTitle> " + title + " .\n");
//                }
//                // y.) Fetch Abstract
//                Iterator<Triple> itAbstract = contentstore.filter((NonLiteral)triplePatent.getSubject(),
//                        new UriRef("http://purl.org/dc/terms/abstract"),
//                        null);
//                while (itAbstract.hasNext()) {
//                    Triple abstractTriple = itAbstract.next();
//                    String abstracts = abstractTriple.getObject().toString();
//                    abstracts = abstracts.replace("\n", " ");
//                    relationsWriter.write(docURI + " <hasAbstract> " + abstracts + " .\n");
//                }
//                // z.) Fetch Content
//                Iterator<Triple> itContent = contentstore.filter((NonLiteral)triplePatent.getSubject(),
//                        new UriRef("http://rdfs.org/sioc/ns#content"),
//                        null);
//                while (itContent.hasNext()) {
//                    Triple contentTriple = itContent.next();
//                    String content = contentTriple.getObject().toString();
//                    if (content.contains("^^")) {
//                        content = content.substring(0, content.indexOf("^^"));
//                        content = content.replace("\n", " ");
//                    }
//                    relationsWriter.write(docURI + " <hasContent> " + content + " .\n");
//                }
//                // a.) Fetch Author(s) & Organisations
//                Iterator<Triple> itInventor = contentstore.filter((NonLiteral)triplePatent.getSubject(),
//                        new UriRef("http://purl.org/dc/elements/1.1/subject"),
//                        null);
//                while (itInventor.hasNext()) {
//                    /**
//                     * For each "Inventor" check the rdfs:type to see if it's an author
//                     */
//                    Triple tripleInventor = itInventor.next();
//                    Resource Inventor = tripleInventor.getObject();
//                    String inventorString = Inventor.toString();
//                    
//                    // 1.) First loop : if (human)
//                    Iterator<Triple> itInventorType = contentstore.filter((NonLiteral)Inventor,
//                            new UriRef("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
//                            new UriRef("http://xmlns.com/foaf/0.1/Person"));
//                    if (itInventorType.hasNext()) {
//                        Iterator<Triple> itInventorTypeHuman = contentstore.filter((NonLiteral)Inventor,
//                                new UriRef("http://www.w3.org/2000/01/rdf-schema#label"),
//                                null);
//                        while (itInventorTypeHuman.hasNext()) {
//                            String authorName = itInventorTypeHuman.next().getObject().toString();
//                            relationsWriter.write(docURI + " <hasAuthor> " + authorName + " .\n");
//                        }
//                    } else {
//                        // 2.) First loop : if (human)
//                        Iterator<Triple> itInventorTypeOrg = contentstore.filter((NonLiteral)Inventor,
//                                new UriRef("http://www.w3.org/2000/01/rdf-schema#label"),
//                                null);
//                        while (itInventorTypeOrg.hasNext()) {
//                            String orgName = itInventorTypeOrg.next().getObject().toString();
//                            relationsWriter.write(docURI + " <hasOrg> " + orgName + " .\n");
//                        }
//                    }
//                }// a.bis.) Fetch Author(s) & Organisations (WITH 1.0 ONTOLOGY)
//                itInventor = contentstore.filter((NonLiteral)triplePatent.getSubject(),
//                        new UriRef("http://purl.org/dc/terms/subject"),
//                        null);
//                while (itInventor.hasNext()) {
//                    /**
//                     * For each "Inventor" check the rdfs:type to see if it's an author
//                     */
//                    Triple tripleInventor = itInventor.next();
//                    Resource Inventor = tripleInventor.getObject();
//                    String inventorString = Inventor.toString();
//                    
//                    // 1.) First loop : if (human)
//                    Iterator<Triple> itInventorType = contentstore.filter((NonLiteral)Inventor,
//                            new UriRef("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
//                            new UriRef("http://xmlns.com/foaf/0.1/Person"));
//                    if (itInventorType.hasNext()) {
//                        Iterator<Triple> itInventorTypeHuman = contentstore.filter((NonLiteral)Inventor,
//                                new UriRef("http://www.w3.org/2000/01/rdf-schema#label"),
//                                null);
//                        while (itInventorTypeHuman.hasNext()) {
//                            String authorName = itInventorTypeHuman.next().getObject().toString();
//                            relationsWriter.write(docURI + " <hasAuthor> " + authorName + " .\n");
//                        }
//                    } else {
//                        // 2.) First loop : if (human)
//                        Iterator<Triple> itInventorTypeOrg = contentstore.filter((NonLiteral)Inventor,
//                                new UriRef("http://www.w3.org/2000/01/rdf-schema#label"),
//                                null);
//                        while (itInventorTypeOrg.hasNext()) {
//                            String orgName = itInventorTypeOrg.next().getObject().toString();
//                            relationsWriter.write(docURI + " <hasOrg> " + orgName + " .\n");
//                        }
//                    }
//                }
//                // b.) Fetch Label(s)
//                Iterator<Triple> itLabels = contentstore.filter((NonLiteral)triplePatent.getSubject(),
//                        new UriRef("http://www.patexpert.org/ontologies/pmo.owl#classifiedAs"),
//                        null);
//                while (itLabels.hasNext()) {
//                    Triple tripleLabel = itLabels.next();
//                    Iterator<Triple> itLabelCode = contentstore.filter((NonLiteral)tripleLabel.getObject(),
//                            new UriRef("http://www.w3.org/2004/02/skos/core#notation"),
//                            null);
//                    while (itLabelCode.hasNext()) {
//                        Triple tripleLabelCode = itLabelCode.next();
//                        String labelCode = tripleLabelCode.getObject().toString();
////                        log.info("adding relation " + docURI + " hasLabel " + labelCode + "to file.");
//                        relationsWriter.write(docURI + " <hasLabel> " + tripleLabelCode.getObject().toString() + " .\n");
//                    }
//                }
//            }
//            
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // Close the writer regardless of what happens...
                relationsPatentsWriter.close();
                relationsPubmedsWriter.close();
            } catch (Exception e) {
            }
        }
    }
    
    @Deactivate
    private void deactivator() {
        // Nothing to do here
    }

    public String getName() {
        return "LUP45";
    }

    public String getDescription() {
        return "LUP module not implemented yet, which should provide some LUP services for the Adaptative Layout T4.5 task.";
    }
    
    public GraphListener getListener() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public FilterTriple getFilter() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public long getDelay() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void BI(String modification) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void updateModels() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Nothing to do since the the model is not on the platform.
     */
    public void save() {}
    public void load() {}
    
    public String predict(String param) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void updateModels(HashMap<String, String> params) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public String predict(HashMap<String, String> params) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
