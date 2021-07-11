package weng.main;

import weng.prepare.UmlsConcept;
import weng.util.DbConnector;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

public class UmlsParser {
    public static DbConnector dbConnector;

    public UmlsParser() {
        dbConnector = new DbConnector();
    }

    public static void main(String[] args) throws SQLException, IOException {
        long startTime = System.currentTimeMillis();

        System.out.println("[CreateUMLSVocabulary] Start");
        UmlsConcept umlsConcept = new UmlsConcept();
        DbConnector dbConnector = new DbConnector();
        Connection connSem = dbConnector.getSemMedConnection();
        Set<String> chemiDrugs = umlsConcept.getChemiDrugs(connSem);
        Set<String> disorders = umlsConcept.getDisorders(connSem);
        Set<String> geneProteins = umlsConcept.getGeneProteins(connSem);
        Set<String> physiologys = umlsConcept.getPhysiologys(connSem);
        Set<String> anatomys = umlsConcept.getAnatomys(connSem);
        connSem.close();
        System.out.println("[CreateUMLSVocabulary] Finish");

        System.out.println("[CreateUMLSVocabularyFiles] Start");
        umlsConcept.createVocabularyFile(chemiDrugs, UmlsConcept.CHEMI_DRUG_VOCABULARY);
        umlsConcept.createVocabularyFile(disorders, UmlsConcept.DISORDER_VOCABULARY);
        umlsConcept.createVocabularyFile(geneProteins, UmlsConcept.GENE_VOCABULARY);
        umlsConcept.createVocabularyFile(physiologys, UmlsConcept.PHYSIOLOGY_VOCABULARY);
        umlsConcept.createVocabularyFile(anatomys, UmlsConcept.ANATOMY_VOCABULARY);
        System.out.println("[CreateUMLSVocabularyFiles] Finish");

        System.out.println("Finished!!!");
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double totalTimeInMinutes = totalTime / (1000 * 60);
        System.out.println("Time consumed (in minutes): " + totalTimeInMinutes);
    }
}
