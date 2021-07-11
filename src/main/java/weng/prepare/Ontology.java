package weng.prepare;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import weng.node.DrugbankNode;
import weng.node.OmimNode;
import weng.util.DbConnector;
import weng.util.Utils;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;


public class Ontology {

    public Ontology() {
    }

    public SetMultimap<String, String> getOntologyNeighbors(Connection connBioRel) {
        Logger logger = Logger.getAnonymousLogger();
        SetMultimap<String, String> m_ontologyNeighbors;
        String filepath = "dat_file/ontologyNeighbors.dat";
        File ontologyFile = new File(filepath);
        if (ontologyFile.exists()) {
            logger.info("Read cached ontology neighbors...");
            m_ontologyNeighbors = (SetMultimap<String, String>) new Utils().readObjectFile(filepath);
            logger.info("Read cached ontology done...");
        } else {
            m_ontologyNeighbors = createOntologyNeighbors(filepath, connBioRel);
        }
        return m_ontologyNeighbors;
    }

    private SetMultimap<String, String> createOntologyNeighbors(String filepath, Connection connBioRel) {
        Logger logger = Logger.getAnonymousLogger();
        logger.info("Ontologies: DrugBank, OMIM, CTD");

        ArrayList<Object> neighborsAndDrugBankCount = getNeighborsAndDrugBankCount(connBioRel);
        SetMultimap<String, String> ontologyNeighbors = (SetMultimap<String, String>) neighborsAndDrugBankCount.get(0);
        SetMultimap<String, String> ontologyNeighbors_DrugBank = (SetMultimap<String, String>) neighborsAndDrugBankCount.get(1); // For statistics

        ArrayList<Object> neighborsAndOMIMCount = getNeighborsAndOMIMCount(ontologyNeighbors, connBioRel);
        ontologyNeighbors = (SetMultimap<String, String>) neighborsAndOMIMCount.get(0);
        SetMultimap<String, String> ontologyNeighbors_OMIM = (SetMultimap<String, String>) neighborsAndOMIMCount.get(1); // For statistics

        ArrayList<Object> neighborsAndCTDCount = getNeighborsAndCTDCount(ontologyNeighbors, connBioRel);
        ontologyNeighbors = (SetMultimap<String, String>) neighborsAndCTDCount.get(0);
        SetMultimap<String, String> ontologyNeighbors_CTD = (SetMultimap<String, String>) neighborsAndCTDCount.get(1); // For statistics

        // For statistics
        logger.info("DrugBank: " + ontologyNeighbors_DrugBank.keySet().size() + " / " + ontologyNeighbors_DrugBank.size());
        logger.info("OMIM: " + ontologyNeighbors_OMIM.keySet().size() + " / " + ontologyNeighbors_OMIM.size());
        logger.info("CTD: " + ontologyNeighbors_CTD.keySet().size() + " / " + ontologyNeighbors_CTD.size());

        // Serialize
        new Utils().writeObject(ontologyNeighbors, filepath);
        return ontologyNeighbors;
    }

    public ArrayList<Object> getNeighborsAndCTDCount(SetMultimap<String, String> ontologyNeighbors, Connection connBioRel) {
        Logger logger = Logger.getAnonymousLogger();
        UmlsConcept umlsConcept = new UmlsConcept();
        HashSet<String> fullUmlsSeeds = new LinkedHashSet<>(umlsConcept.getSeedsNonRepeated());
        int mainUmlsCount = 0;
        int neighborUmlsCount = 0;
        // CTD chem_gene && gene_disease
        SetMultimap<String, String> ontologyNeighbors_CTD = LinkedHashMultimap.create(); // For statistics
        String selUmlsNeighbor = "SELECT `neighbor_umls` FROM `ctd_neighbor_umls` WHERE `umls` = ?";
        try (PreparedStatement psQueryCtd = connBioRel.prepareStatement(selUmlsNeighbor)) {
            for (String umls : fullUmlsSeeds) {
                psQueryCtd.clearParameters();
                psQueryCtd.setString(1, umls);
                ResultSet rsQueryCtd = psQueryCtd.executeQuery();
                if (rsQueryCtd.next()) {
                    ++mainUmlsCount;
                    // Already bidirection so no need manual put
                    String neighborRaw = rsQueryCtd.getString("neighbor_umls");
                    for (String relatedUmls : neighborRaw.split("\n")) {
                        if (fullUmlsSeeds.contains(relatedUmls)) {
                            ++neighborUmlsCount;
                            ontologyNeighbors.put(umls, relatedUmls);
                            ontologyNeighbors_CTD.put(umls, relatedUmls); //For statistics
                        }
                    }
                }
                rsQueryCtd.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
        logger.info("DrugBank + OMIM + CTD: " + ontologyNeighbors.keySet().size() + " / " + ontologyNeighbors.size());
        logger.info("CTD: " + mainUmlsCount + " / " + neighborUmlsCount + "(" + (neighborUmlsCount / 2) + ")");
        ArrayList<Object> ontologyNeighborsAndCount = new ArrayList<>();
        ontologyNeighborsAndCount.add(ontologyNeighbors);
        ontologyNeighborsAndCount.add(ontologyNeighbors_CTD);
        return ontologyNeighborsAndCount;
    }

    public ArrayList<Object> getNeighborsAndOMIMCount(SetMultimap<String, String> ontologyNeighbors, Connection connBioRel) {
        Logger logger = Logger.getAnonymousLogger();
        UmlsConcept umlsConcept = new UmlsConcept();
        HashSet<String> fullUmlsSeeds = new LinkedHashSet<>(umlsConcept.getSeedsNonRepeated());
        SetMultimap<String, String> ontologyNeighbors_OMIM = LinkedHashMultimap.create();
        int mainUmlsCount = 0;
        int neighborUmlsCount = 0;
        for (String umls : fullUmlsSeeds) {
            OmimNode mimNode = new OmimNode(umls, connBioRel);
            if (mimNode.isExist()) {
                ++mainUmlsCount;
                for (String relatedUmls : mimNode.getUmlsNeighbors()) {
                    if (fullUmlsSeeds.contains(relatedUmls) && !umls.equals(relatedUmls)) {
                        ++neighborUmlsCount;
                        //MIM table already savePossibleUmlsFile bidirection info, still add both side?
                        ontologyNeighbors.put(umls, relatedUmls);
                        ontologyNeighbors.put(relatedUmls, umls);

                        //For statistics
                        ontologyNeighbors_OMIM.put(umls, relatedUmls);
                        ontologyNeighbors_OMIM.put(relatedUmls, umls);
                    }
                }
            }
        }
        logger.info("DrugBank + OMIM: " + ontologyNeighbors.keySet().size() + " / " + ontologyNeighbors.size());
        logger.info("OMIM: " + mainUmlsCount + " / " + neighborUmlsCount);
        ArrayList<Object> ontologyNeighborsAndCount = new ArrayList<>();
        ontologyNeighborsAndCount.add(ontologyNeighbors);
        ontologyNeighborsAndCount.add(ontologyNeighbors_OMIM);
        return ontologyNeighborsAndCount;
    }

    public ArrayList<Object> getNeighborsAndDrugBankCount(Connection connBioRel) {
        Logger logger = Logger.getAnonymousLogger();
        UmlsConcept umlsConcept = new UmlsConcept();
        HashSet<String> fullUmlsSeeds = new LinkedHashSet<>(umlsConcept.getSeedsNonRepeated());
        int mainUmlsCount = 0;
        int neighborUmlsCount = 0;
        SetMultimap<String, String> ontologyNeighbors = LinkedHashMultimap.create();
        SetMultimap<String, String> ontologyNeighbors_DrugBank = LinkedHashMultimap.create(); // For statistics
        for (String umls : fullUmlsSeeds) { // Add drugbank bidirectional relation
            DrugbankNode dbNode = new DrugbankNode(umls, connBioRel);
            if (dbNode.isExist()) {
                ++mainUmlsCount;
                Set<String> umlsNeighbors = dbNode.getUmlsNeighbors(connBioRel);
                for (String umlsNeighbor : umlsNeighbors) {
                    if (fullUmlsSeeds.contains(umlsNeighbor) && !umls.equals(umlsNeighbor)) {
                        ++neighborUmlsCount;
                        // Put both, avoid self-related
                        ontologyNeighbors.put(umls, umlsNeighbor);
                        ontologyNeighbors.put(umlsNeighbor, umls);

                        // For statistics
                        ontologyNeighbors_DrugBank.put(umls, umlsNeighbor);
                        ontologyNeighbors_DrugBank.put(umlsNeighbor, umls);
                    }
                }
            }
        }
        logger.info("After DrugBank: " + ontologyNeighbors.keySet().size() + " / " + ontologyNeighbors.size());
        logger.info("DrugBank: " + mainUmlsCount + " / " + neighborUmlsCount);
        ArrayList<Object> ontologyNeighborsAndCount = new ArrayList<>();
        ontologyNeighborsAndCount.add(ontologyNeighbors);
        ontologyNeighborsAndCount.add(ontologyNeighbors_DrugBank);
        return ontologyNeighborsAndCount;
    }

//    public ArrayList<Object> getNeighborsAndDrugBankCount(Connection connBioRel) {
//        Logger logger = Logger.getAnonymousLogger();
//        UmlsConcept umlsConcept = new UmlsConcept();
//        HashSet<String> fullUmlsSeeds = new LinkedHashSet<>(umlsConcept.getSeedsNonRepeated());
//        int mainUmlsCount = 0;
//        int neighborUmlsCount = 0;
//        SetMultimap<String, String> ontologyNeighbors = LinkedHashMultimap.create();
//        SetMultimap<String, String> ontologyNeighbors_DrugBank = LinkedHashMultimap.create(); // For statistics
//        for (String umls : fullUmlsSeeds) { // Add drugbank bidirectional relation
//            DrugbankNode dbNode = new DrugbankNode(umls, connBioRel);
//            if (dbNode.isExist()) {
//                ++mainUmlsCount;
//                Set<String> umlsNeighbors = dbNode.getUmlsNeighbors(connBioRel);
//                for (String umlsNeighbor : umlsNeighbors) {
//                    if (fullUmlsSeeds.contains(umlsNeighbor) && !umls.equals(umlsNeighbor)) {
//                        ++neighborUmlsCount;
//                        // Put both, avoid self-related
//                        ontologyNeighbors.put(umls, umlsNeighbor);
//                        ontologyNeighbors.put(umlsNeighbor, umls);
//
//                        // For statistics
//                        ontologyNeighbors_DrugBank.put(umls, umlsNeighbor);
//                        ontologyNeighbors_DrugBank.put(umlsNeighbor, umls);
//                    }
//                }
//            }
//        }
//        logger.info("After DrugBank: " + ontologyNeighbors.keySet().size() + " / " + ontologyNeighbors.size());
//        logger.info("DrugBank: " + mainUmlsCount + " / " + neighborUmlsCount);
//        ArrayList<Object> ontologyNeighborsAndCount = new ArrayList<>();
//        ontologyNeighborsAndCount.add(ontologyNeighbors);
//        ontologyNeighborsAndCount.add(ontologyNeighbors_DrugBank);
//        return ontologyNeighborsAndCount;
//    }
}
