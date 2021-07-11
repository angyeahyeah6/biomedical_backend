package weng.model;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.json.simple.JSONObject;
import weng.util.JDBCHelper;
import weng.util.noSqlColumn.AbcPredicate;
import weng.util.noSqlColumn.PredicationCount;
import weng.util.noSqlData.AbcPredicateInfo;
import weng.util.noSqlData.PredicationCountInfo;
import weng.util.sqlData.Predicate;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LearningPreparation {
    public LearningPreparation() {
    }

    public List<Predicate> predicates(Connection connLit) {
        List<Predicate> predicates = new ArrayList<>();
        String sql = "SELECT `name`, `freq`, `type`, `importance`, `importance_score` FROM `umls_predicates`;";
        JDBCHelper jdbcHelper = new JDBCHelper();
        List<Map<String, Object>> interDocs = jdbcHelper.query(connLit, sql);
        for (Map<String, Object> row : interDocs) {
            predicates.add(new Predicate(
                    String.valueOf(row.get("name")),
                    Integer.parseInt(String.valueOf(row.get("freq"))),
                    String.valueOf(row.get("type")),
                    String.valueOf(row.get("importance")),
                    Integer.parseInt(String.valueOf(row.get("importance_score")))
            ));
        }
        return predicates;
    }

    public List<PredicationCountInfo> predicationCounts(DBCollection collPredicationCount) {
        List<PredicationCountInfo> predicationCounts = new ArrayList<>();
        DBCursor cursor = collPredicationCount.find(new BasicDBObject());
        while (cursor.hasNext()) {
            DBObject info = cursor.next();
            JSONObject json = new JSONObject((Map) info);
            if ((json.get(PredicationCount.intermediate) != null)) {
                predicationCounts.add(new PredicationCountInfo(
                        json.get(PredicationCount.drug).toString(),
                        json.get(PredicationCount.intermediate).toString(),
                        json.get(PredicationCount.disease).toString(),
                        Integer.parseInt(json.get(PredicationCount.count_ab).toString()),
                        Integer.parseInt(json.get(PredicationCount.count_bc).toString())
                ));
            }
        }
        return predicationCounts;
    }

    public List<AbcPredicateInfo> abcPredicates(DBCollection collPredicate) {
        List<AbcPredicateInfo> abcPredicates = new ArrayList<>();
        DBCursor cursor = collPredicate.find(new BasicDBObject());
        while (cursor.hasNext()) {
            DBObject info = cursor.next();
            if (info.get(AbcPredicate.intermediate) != null) {
                DBObject abPredicates = (DBObject) info.get(AbcPredicate.abPredicates);
                DBObject bcPredicates = (DBObject) info.get(AbcPredicate.bcPredicates);
                abcPredicates.add(new AbcPredicateInfo(
                        info.get(AbcPredicate.drug).toString(),
                        info.get(AbcPredicate.intermediate).toString(),
                        info.get(AbcPredicate.disease).toString(),
                        abPredicates.toMap(),
                        bcPredicates.toMap()
                ));
            }

//            JSONObject json = new JSONObject((Map) info);
//            if ((json.get(AbcPredicate.intermediate) != null)) {
//                abcPredicates.add(new AbcPredicateInfo(
//                        json.get(AbcPredicate.drug).toString(),
//                        json.get(AbcPredicate.intermediate).toString(),
//                        json.get(AbcPredicate.disease).toString(),
//                        json.get(AbcPredicate.abPredicates),
//                        json.get(AbcPredicate.bcPredicates)
//                ));
//            }
        }
        return abcPredicates;
    }
}
