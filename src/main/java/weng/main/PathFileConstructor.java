package weng.main;

import weng.labelSys.SemPathFinder;
import weng.prepare.UmlsConcept;
import weng.util.DbConnector;
import weng.util.JDBCHelper;
import weng.util.file.LabeledCases;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by weng on 2018/1/25.
 */
public class PathFileConstructor {
    public static void main(String[] args) throws IOException, SQLException {
        // TODO: Choose more A-C paths
//        DbConnector dbConnector = new DbConnector();
//        Connection connSem = dbConnector.getSemMedConnection();
//        Connection connLit = dbConnector.getLiteratureNodeConnection();
//        HashMap<String, String> umlsCuiNameMap = UmlsConcept.getUmlsCuiNameMap(connSem);
//        FileReader frLabeledCases = new FileReader(new File(LabeledCases.umls));
//        BufferedReader brLabeledCases = new BufferedReader(frLabeledCases);
//        String line = brLabeledCases.readLine();
//        while ((line = brLabeledCases.readLine()) != null) {
//            String[] labelCase = line.split("\t");
//            String A = labelCase[0];
//            String C = labelCase[1];
//            int endYear = Integer.parseInt(labelCase[2]);
//            int fdaYear = Integer.parseInt(labelCase[3]);
//            System.out.println("LabeledCases: " + A + "_" + C);
//
//            SemPathFinder semPathFinder = new SemPathFinder(A, C, endYear, fdaYear, connSem, connLit, umlsCuiNameMap);
//            String csvPath = "output/pathFinder/" + A + "-" + C + "-" + endYear + ".csv";
//            semPathFinder.writeCsv(csvPath);
//            String jsonPath = "output/pathFinder/json/" + A + "_" + C + "_" + endYear + ".json";
//            semPathFinder.writeJson(jsonPath);
//        }
//        frLabeledCases.close();
//        brLabeledCases.close();
//        connSem.close();
//        connLit.close();

        // TODO: Choose one A-C path
        DbConnector dbConnector = new DbConnector();
        Connection connSem = dbConnector.getSemMedConnection();
        Connection connLit = dbConnector.getLiteratureNodeConnection();
        HashMap<String, String> umlsCuiNameMap = UmlsConcept.getUmlsCuiNameMap(connSem);
        String A = "Colesevelam";
        String C = "Diabetes Mellitus, Non-Insulin-Dependent";
        int endYear = 2006;
        int fdaYear = 2008;
        SemPathFinder semPathFinder = new SemPathFinder(A, C, endYear, fdaYear, connSem, connLit, umlsCuiNameMap);
        String csvPath = "output/pathFinder/" + A + "_" + C + "-" + endYear + ".csv";
        semPathFinder.writeCsv(csvPath);
        String jsonPath = "output/pathFinder/json/" + A + "_" + C + "_" + endYear + ".json";
        semPathFinder.writeJson(jsonPath);
        connSem.close();
        connLit.close();
    }
}
