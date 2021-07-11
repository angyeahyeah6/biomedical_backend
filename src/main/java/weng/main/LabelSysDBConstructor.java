package weng.main;

import weng.parser.LabelSysParser;
import weng.prepare.UmlsConcept;
import weng.util.DbConnector;
import weng.util.file.LabeledCases;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * Created by weng on 2018/1/25.
 */
public class LabelSysDBConstructor {
    public static void main(String[] args) throws IOException, SQLException {
        // TODO: Choose more A-C paths
        DbConnector dbConnector = new DbConnector();
        Connection connSem = dbConnector.getSemMedConnection();
        Connection connLS = dbConnector.getLabelSystemUmlsConnection();
        Connection connLit = dbConnector.getLiteratureNodeConnection();
        HashMap<String, String> umlsCuiNameMap = UmlsConcept.getUmlsCuiNameMap(connSem);
        FileReader frLabeledCases = new FileReader(new File(LabeledCases.umls));
        BufferedReader brLabeledCases = new BufferedReader(frLabeledCases);
        String line = brLabeledCases.readLine();
        while ((line = brLabeledCases.readLine()) != null) {
            String[] labelCase = line.split("\t");
            String A = labelCase[0];
            String C = labelCase[1];
            int endYear = Integer.parseInt(labelCase[2]);
            int fdaYear = Integer.parseInt(labelCase[3]);
            System.out.println("LabeledCases: " + A + "-" + C);

            LabelSysParser labelSysParser = new LabelSysParser(A, C, endYear, fdaYear, connSem, connLit, umlsCuiNameMap);
            labelSysParser.insertToLabelSystem(connLS, connSem);
        }
        frLabeledCases.close();
        brLabeledCases.close();
        connSem.close();
        connLS.close();

        // TODO: Choose one A-C path
//        DbConnector dbConnector = new DbConnector();
//        Connection connSem = dbConnector.getSemMedConnection();
//        Connection connLS = dbConnector.getLabelSystemUmlsConnection();
//        UmlsConcept umlsConcept = new UmlsConcept();
//        HashMap<String, String> umlsCuiNameMap = umlsConcept.getUmlsCuiNameMap(connSem);
//        String A = "Amphotericin B";
//        String C = "Leishmaniasis, Visceral";
//        int endYear = 1990;
//        int fdaYear = 1997;
//        LabelSysParser labelSysParser = new LabelSysParser(A, C, endYear, fdaYear, connSem, umlsCuiNameMap);
//        labelSysParser.insertToLabelSystem(connLS, connSem);
//        connSem.close();
//        connLS.close();
    }
}
