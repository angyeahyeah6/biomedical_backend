package weng.main;

import weng.prepare.Ontology;
import weng.util.DbConnector;

import java.sql.Connection;

public class OntologyConstructor {
    public static void main(String[] args) {
        Ontology ontology = new Ontology();
        DbConnector dbConnector = new DbConnector();
        Connection connBioRel = dbConnector.getBioRelationConnection();
        ontology.getOntologyNeighbors(connBioRel);
        System.out.println("Finished!!!");
    }
}
