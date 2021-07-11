package weng.main;

import weng.parser.DrugBankParser;
import weng.util.DbConnector;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class DrugBankDBConstructor {

    public static void main(String[] args) throws SQLException, IOException {
        System.out.println("[UMLSMappedToMesh] Start");
        long startTime = System.currentTimeMillis();

        DbConnector dbConnector = new DbConnector();
        Connection connBioRel = dbConnector.getBioRelationConnection();
        Connection connBioCon = dbConnector.getBioConceptConnection();
        Connection connSem = dbConnector.getSemMedConnection();
        DrugBankParser drugBankParser = new DrugBankParser();

        // Mapping drug name to UMLS
//        drugBankParser.drugbankUmlsMap(connBioRel, connBioCon);
//        drugBankParser.notMatchedDrugbankUmls2ndMap(connBioRel, connBioCon); // Mapping drugs not mapped to UMLS by 1st mapping step.

        // Use this function when you want to map OMIM entry to Mesh annotation.
//        drugBankParser.omimUmlsMap(connBioCon, connSem);
//        drugBankParser.notMatchedOmimUmls2ndMap(connBioCon, connSem);

        connBioRel.close();
        connBioCon.close();
        connSem.close();
        System.out.println("[UMLSMappedToMesh] Finish");
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double totalTimeInMinutes = totalTime / (1000 * 60);
        System.out.println("Time consumed (in minutes): " + totalTimeInMinutes);

    }
}
