package weng.main;

import org.json.simple.parser.ParseException;
import weng.parser.CtdParser;
import weng.util.DbConnector;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class CtdDBConstructor {
    public static void main(String[] args) throws IOException, SQLException, ParseException {
        long startTime = System.currentTimeMillis();

        DbConnector dbConnector = new DbConnector();
        Connection connBioCon = dbConnector.getBioConceptConnection();
        Connection connBioRel = dbConnector.getBioRelationConnection();
        CtdParser ctdParser = new CtdParser();
//        ctdParser.ctdDiseaseMap(connBioCon); // Insert: omim_meshid
//        ctdParser.mapDBOmimMeshToOmimUmls(connBioCon); // Insert: omim_umlscui

//        ctdParser.queryGeneSeedId(connBioCon); // Create file: ncbigene/matchNcbiId_gene.txt
//        ctdParser.parseCtdFileToDB(connBioCon, connBioRel); // Insert: ctd_relation_umls
        ctdParser.transformCtdRelation(connBioRel); // Insert: ctd_neighbor_umls

        connBioCon.close();
        connBioRel.close();
        System.out.println("Finished!!!");
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double totalTimeInMinutes = totalTime / (1000 * 60);
        System.out.println("Time consumed (in minutes): " + totalTimeInMinutes);
    }
}
