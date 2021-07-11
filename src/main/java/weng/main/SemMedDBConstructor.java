package weng.main;

import weng.parser.SemMedPreparser;
import weng.util.DbConnector;

import java.sql.Connection;
import java.sql.SQLException;

public class SemMedDBConstructor {
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        System.out.println("[SemMedDBConstructor] Start");
        SemMedPreparser semMedPreparserUmls = new SemMedPreparser();
        DbConnector dbConnector = new DbConnector();
        Connection connSem = dbConnector.getSemMedConnection();
        Connection connLit = dbConnector.getLiteratureNodeConnection();
        try {
            semMedPreparserUmls.createUMLSPmidCount(connLit);
//            semMedPreparserUmls.createUMLSPredicationAggregate(connSem, connLit);
//            semMedPreparserUmls.createUMLSPredicationAggregateFilteredBySemType(connSem, connLit);
//            semMedPreparserUmls.createUMLSNeighborByPredication(connLit);
//            semMedPreparserUmls.processMedlineGroupByYear(connSem, connLit);
            connLit.close();
            connSem.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("[SemMedDBConstructor] Finish");
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double totalTimeInSecond = totalTime / (1000);
        System.out.println("Time consumed (in seconds): " + totalTimeInSecond);
    }
}
