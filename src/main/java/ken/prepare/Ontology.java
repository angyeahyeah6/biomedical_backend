package ken.prepare;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import ken.node.DrugbankNode;
import ken.node.OmimNode;
import ken.util.DbConnector;
import ken.util.Utils;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.logging.Logger;

;

public class Ontology {
    private static SetMultimap<String, String> m_ontologyNeighbors = null;
    private static String filepath = "dat_file/ontologyNeighbors.dat";

    public static void main(String[] args) {
        System.out.println("Finished!!!");
//        createOntologyNeighbors();
        getOntologyNeighbors();
        System.out.println("Finished!!!");
    }

    public static SetMultimap<String, String> getOntologyNeighbors() {
        if (m_ontologyNeighbors == null) {
            Logger logger = Logger.getAnonymousLogger();
            m_ontologyNeighbors = LinkedHashMultimap.create();

            File ontologyFile = new File(filepath);
            if (ontologyFile.exists()) {
                logger.info("Read cached ontology neighbors...");
                m_ontologyNeighbors = (SetMultimap<String, String>) Utils.readObjectFile(filepath);

                logger.info("Read cached ontology done...");
            } else {
                m_ontologyNeighbors = createOntologyNeighbors();
            }
        }
        return m_ontologyNeighbors;
    }

    private static SetMultimap<String, String> createOntologyNeighbors() {
        Logger logger = Logger.getAnonymousLogger();
        logger.info("Ontologies: DrugBank, OMIM, CTD");
        SetMultimap<String, String> ontologyNeighbors = LinkedHashMultimap.create();

        //For statistics
        SetMultimap<String, String> ontologyNeighbors_DrugBank = LinkedHashMultimap.create();
        SetMultimap<String, String> ontologyNeighbors_OMIM = LinkedHashMultimap.create();
        SetMultimap<String, String> ontologyNeighbors_CTD = LinkedHashMultimap.create();
        //End

        HashSet<String> fullMeshSeeds = new LinkedHashSet<>(MeshConceptObject.getSeedsNonRepeated());

        int mainMeshCount = 0;
        int neighborMeshCount = 0;
        //Add drugbank bidirectional relation
        for (String mesh : fullMeshSeeds) {
            DrugbankNode dbNode = new DrugbankNode(mesh);
            if (dbNode.isExist()) {
                ++mainMeshCount;
                for (String neighborMesh : dbNode.getMeshNeighbors()) {
                    if (fullMeshSeeds.contains(neighborMesh) && !mesh.equals(neighborMesh)) {
                        ++neighborMeshCount;
                        //Put both, avoid self-related
                        ontologyNeighbors.put(mesh, neighborMesh);
                        ontologyNeighbors.put(neighborMesh, mesh);

                        //For statistics
                        ontologyNeighbors_DrugBank.put(mesh, neighborMesh);
                        ontologyNeighbors_DrugBank.put(neighborMesh, mesh);
                        //End
                    }
                }
            }
        }

        logger.info("After DrugBank: " + ontologyNeighbors.keySet().size() + " / " + ontologyNeighbors.size());
        logger.info("DrugBank: " + mainMeshCount + " / " + neighborMeshCount);

        mainMeshCount = 0;
        neighborMeshCount = 0;
        for (String mesh : fullMeshSeeds) {
            OmimNode mimNode = new OmimNode(mesh);
            if (mimNode.isExist()) {
                ++mainMeshCount;
                for (String relatedMesh : mimNode.getMeshNeighbors()) {
                    if (fullMeshSeeds.contains(relatedMesh) && !mesh.equals(relatedMesh)) {
                        ++neighborMeshCount;
                        //MIM table already save bidirection info, still add both side?
                        ontologyNeighbors.put(mesh, relatedMesh);
                        ontologyNeighbors.put(relatedMesh, mesh);

                        //For statistics
                        ontologyNeighbors_OMIM.put(mesh, relatedMesh);
                        ontologyNeighbors_OMIM.put(relatedMesh, mesh);
                        //End
                    }
                }
            }
        }
        logger.info("DrugBank + OMIM: " + ontologyNeighbors.keySet().size() + " / " + ontologyNeighbors.size());
        logger.info("OMIM: " + mainMeshCount + " / " + neighborMeshCount);

        mainMeshCount = 0;
        neighborMeshCount = 0;
        //CTD chem_gene && gene_disease

        try (Connection connBioRelation = DbConnector.getBioRelationConnection();
             PreparedStatement psQueryCtd = connBioRelation.prepareStatement("SELECT `Neighbor_Mesh` FROM `ctd_neighbor` WHERE `Mesh` = ?")) {
//		try (PreparedStatement psQueryCtd = connBioRelation.prepareStatement("SELECT `Neighbor` FROM `ctd_new_all` WHERE `MESH` = ?")) {
            for (String mesh : fullMeshSeeds) {
                psQueryCtd.clearParameters();
                psQueryCtd.setString(1, mesh);
                ResultSet rsQueryCtd = psQueryCtd.executeQuery();
                if (rsQueryCtd.next()) {
                    ++mainMeshCount;

                    //Already bidirection so no need manual put
                    String neighborRaw = rsQueryCtd.getString("Neighbor_Mesh");
//					String neighborRaw = rsQueryCtd.getString("Neighbor");
                    for (String relatedMesh : neighborRaw.split("\n")) {
                        if (fullMeshSeeds.contains(relatedMesh)) {
                            ++neighborMeshCount;
                            ontologyNeighbors.put(mesh, relatedMesh);

                            //For statistics
                            ontologyNeighbors_CTD.put(mesh, relatedMesh);
                            //End
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
        logger.info("CTD: " + mainMeshCount + " / " + neighborMeshCount + "(" + (neighborMeshCount / 2) + ")");

        //For statistics
        logger.info("DrugBank: " + ontologyNeighbors_DrugBank.keySet().size() + " / " + ontologyNeighbors_DrugBank.size());
        logger.info("OMIM: " + ontologyNeighbors_OMIM.keySet().size() + " / " + ontologyNeighbors_OMIM.size());
        logger.info("CTD: " + ontologyNeighbors_CTD.keySet().size() + " / " + ontologyNeighbors_CTD.size());
        //End

        //Serialize
        Utils.writeObject(ontologyNeighbors, filepath);
        return ontologyNeighbors;
    }
}
