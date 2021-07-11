package weng.parser;

import au.com.bytecode.opencsv.CSVWriter;
import weng.label.LabeledInfo;
import weng.node.LiteratureNode;
import weng.prepare.UmlsConcept;
import weng.util.file.Predicate;
import weng.util.DbConnector;
import weng.util.JDBCHelper;

import java.io.*;
import java.sql.*;
import java.util.*;

/**
 * Created by lbj23k on 2017/3/23.
 */

public class LabelSysParser {
    private final int startYear = 1809;
    private int endYear;
    private int fdaYear;
    private LiteratureNode pivotNode;
    private LiteratureNode targetNode;
    private HashMap<String, ArrayList<LabeledInfo>> ABrelation;
    private HashMap<String, ArrayList<LabeledInfo>> BCrelation;

    public LabelSysParser(String pivot, String target, int endYear, int fdaYear, Connection connSem, Connection connLit, HashMap<String, String> umlsCuiNameMap) throws IOException, SQLException {
        this.endYear = endYear;
        this.pivotNode = new LiteratureNode(pivot, startYear, endYear, connSem, connLit);
        this.targetNode = new LiteratureNode(target, startYear, endYear, connSem, connLit);
        this.fdaYear = fdaYear;
        ArrayList<HashMap<String, ArrayList<LabeledInfo>>> ABAndBCRelation = setABAndBCRelation(umlsCuiNameMap);
        this.ABrelation = ABAndBCRelation.get(0);
        this.BCrelation = ABAndBCRelation.get(1);
    }

    public ArrayList<HashMap<String, ArrayList<LabeledInfo>>> setABAndBCRelation(HashMap<String, String> umlsCuiNameMap) throws IOException, SQLException {
        LiteratureNode pivotNode = getPivotNode();
        LiteratureNode targetNode = getTargetNode();
        String a_cui = pivotNode.getCui();
        String c_cui = targetNode.getCui();
        int endYear = getEndYear();
        String litDBName = DbConnector.LITERATURE_YEAR;

        HashMap<String, ArrayList<LabeledInfo>> ABrelation = new HashMap<>();
        String sqlSelABRelation = "SELECT `o_cui` as `neighbor`, `predicate`, `pmid`, `year`, `is_exist` " +
                "FROM `umls_predication_aggregate_filtered` WHERE `s_cui`=? and `o_cui` in (SELECT `s_cui` " +
                "FROM `umls_predication_aggregate_filtered` WHERE `o_cui`=? and year between ? and ?) " +
                "and year between ? and ?";
        Object[] params_ab = {a_cui, c_cui, startYear, endYear, startYear, endYear};
        JDBCHelper jdbcHelper = new JDBCHelper();
        List<Map<String, Object>> result_ab = jdbcHelper.query(litDBName, sqlSelABRelation, params_ab);

        HashMap<String, ArrayList<LabeledInfo>> BCrelation = new HashMap<>();
        String sqlSelBCRelation = "SELECT `s_cui` as `neighbor`, `predicate`, `pmid`, `year`, `is_exist` " +
                "FROM `umls_predication_aggregate_filtered` WHERE `o_cui`=? and `s_cui` in (SELECT `o_cui` " +
                "FROM `umls_predication_aggregate_filtered` WHERE `s_cui`=? and year between ? and ?) " +
                "and year between ? and ?";
        Object[] params_bc = {c_cui, a_cui, startYear, endYear, startYear, endYear};
        List<Map<String, Object>> result_bc = jdbcHelper.query(litDBName, sqlSelBCRelation, params_bc);

        Set<String> neighborSet = new HashSet<>();
        for (Map row : result_ab) {
            neighborSet.add((String) row.get("neighbor"));
        }
        for (String intermCui : neighborSet) {
            String intermName = umlsCuiNameMap.get(intermCui);
            if (intermName != null) {
                ArrayList<LabeledInfo> tmp = setRelation(result_ab, intermCui);
                ArrayList<LabeledInfo> tmp2 = setRelation(result_bc, intermCui);

                if (tmp.size() > 0 && tmp2.size() > 0) {
                    ABrelation.put(intermName, tmp);
                    BCrelation.put(intermName, tmp2);
                }
            }
        }
        ArrayList<HashMap<String, ArrayList<LabeledInfo>>> ABAndBCRelation = new ArrayList<>();
        ABAndBCRelation.add(ABrelation);
        ABAndBCRelation.add(BCrelation);
        return ABAndBCRelation;
    }

    public ArrayList<LabeledInfo> setRelation(List<Map<String, Object>> relations,
                                              String intermCui) {
        ArrayList<LabeledInfo> ls = new ArrayList<>();
        for (Map<String, Object> relation : relations) {
            String neighborCui = (String) relation.get("neighbor");
            if (!neighborCui.equals(intermCui)) {
                continue;
            }
            String predicate = (String) relation.get("predicate");
            if (Predicate.set.contains(predicate)) {
                int year = (int) relation.get("year");
                String pmid = (String) relation.get("pmid");
                int direction = (int) relation.get("is_exist");
                direction = (direction == 0) ? 2 : 1;
                LabeledInfo pdInfo = new LabeledInfo(direction, predicate, pmid, year);
                ls.add(pdInfo);
            }
        }
        return ls;
    }

    public void setEndYear(int endYear) {
        this.endYear = endYear;
    }

    public void setPivotNode(LiteratureNode pivotNode) {
        this.pivotNode = pivotNode;
    }

    public void setTargetNode(LiteratureNode targetNode) {
        this.targetNode = targetNode;
    }

    public void setFdaYear(int fdaYear) {
        this.fdaYear = fdaYear;
    }

    public int getEndYear() {
        return endYear;
    }

    public LiteratureNode getPivotNode() {
        return pivotNode;
    }

    public LiteratureNode getTargetNode() {
        return targetNode;
    }

    public HashMap<String, ArrayList<LabeledInfo>> getABrelation() {
        return ABrelation;
    }

    public HashMap<String, ArrayList<LabeledInfo>> getBCrelation() {
        return BCrelation;
    }

    public int getFdaYear() {
        return fdaYear;
    }

    public CSVWriter getCSVWriter(String outputPathFile) {
        CSVWriter csvWriter = null;
        try {
            csvWriter = new CSVWriter(new FileWriter(outputPathFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return csvWriter;
    }

    public ArrayList<String> getImportantPredicate(String importantPredicateFilePath) throws IOException {
        ArrayList<String> importantPredicates = new ArrayList<>();
        File importantPredicateFile = new File(importantPredicateFilePath);
        FileReader frImportantPredicate = new FileReader(importantPredicateFile);
        BufferedReader brImportantPredicate = new BufferedReader(frImportantPredicate);
        String line;
        while ((line = brImportantPredicate.readLine()) != null) {
            importantPredicates.add(line);
        }
        return importantPredicates;
    }

    public Object[] constructLinkParams() {
        LiteratureNode pivotNode = getPivotNode();
        String a_id = pivotNode.getCui();
        String a_name = pivotNode.getName();
        LiteratureNode targetNode = getTargetNode();
        String c_id = targetNode.getCui();
        String c_name = targetNode.getName();
        int year = getEndYear();
        int fda_year = getFdaYear();
        int flag = 0;
        Object[] params = {a_id, a_name, c_id, c_name, year, fda_year, flag};
        return params;
    }

    public ArrayList<Object[]> constructIntermediateParams(int linkID, Connection connSem) throws SQLException {
        ArrayList<Object[]> paramsList = new ArrayList<>();
        HashMap<String, String> umlsNameCuiMap = UmlsConcept.getUmlsNameCuiMap(connSem);
        for (String intermName : getABrelation().keySet()) {
            String b_cui = umlsNameCuiMap.get(intermName);
            if (b_cui != null) {
                Object[] param = {b_cui, intermName, null, linkID};
                paramsList.add(param);
            }
        }
        return paramsList;
    }

    public ArrayList<Object[]> constructRelationParams(int relationType, HashMap<Integer, String> intermediateIDs) {
        ArrayList<Object[]> relationParams = new ArrayList<>();
        if (relationType == 1) {
            relationParams = constructRelationListByType(getABrelation(), relationType, intermediateIDs);
        } else if (relationType == 2) {
            relationParams = constructRelationListByType(getBCrelation(), relationType, intermediateIDs);
        }
        return relationParams;
    }

    public ArrayList<Object[]> constructRelationListByType(HashMap<String, ArrayList<LabeledInfo>> relation,
                                                           int relationType,
                                                           HashMap<Integer, String> intermediateIDList) {
        HashMap<String, HashSet<String>> interactionsByInterm = constructNonDuplicateInteractions(relation);
        ArrayList<Object[]> relationParamsByType = new ArrayList<>();
        for (int intermediateID : intermediateIDList.keySet()) {
            String intermediate = intermediateIDList.get(intermediateID);
            HashSet<String> interactions = interactionsByInterm.get(intermediate);
            for (String interaction : interactions) {
                Object[] param = {relationType, interaction, intermediateID};
                relationParamsByType.add(param);
            }
        }
        return relationParamsByType;
    }

    public HashMap<String, HashSet<String>> constructNonDuplicateInteractions(HashMap<String, ArrayList<LabeledInfo>> relation) {
        HashMap<String, HashSet<String>> interactionsByInterm = new HashMap<>();
        for (String intermName : relation.keySet()) {
            ArrayList<LabeledInfo> relationList = relation.get(intermName);
            HashSet<String> interactions = new HashSet<>();
            for (LabeledInfo relationInfo : relationList) {
                String interaction = relationInfo.getPredicate();
                interactions.add(interaction);
            }
            interactionsByInterm.put(intermName, interactions);
        }
        return interactionsByInterm;
    }

    public ArrayList<Object[]> constructRelationInfoParams(int relationType, HashMap<String, HashMap<String, Integer>> relationIDsByIntermediate) {
        ArrayList<Object[]> relationInfoParams = new ArrayList<>();
        for (String intermediate : relationIDsByIntermediate.keySet()) {
            HashMap<String, Integer> relationIDs = relationIDsByIntermediate.get(intermediate);
            ArrayList<LabeledInfo> relationList = new ArrayList<>();
            if (relationType == 1) {
                relationList = getABrelation().get(intermediate);
            } else if (relationType == 2) {
                relationList = getBCrelation().get(intermediate);
            }
            for (LabeledInfo relationInfo : relationList) {
                String interaction = relationInfo.getPredicate();
                int relationID = relationIDs.get(interaction);
                int direction = relationInfo.getDirection();
                int year = relationInfo.getYear();
                String pmid = relationInfo.getPmid();
                Object[] param = {direction, year, pmid, relationID};
                relationInfoParams.add(param);
            }
        }
        return relationInfoParams;
    }

    public void insertToLabelSystem(Connection connLS, Connection connSem) throws SQLException {
        // InterPattern table
        Object[] linkParams = constructLinkParams();
        insertToLinkTable(linkParams, connLS);

        // Intermediate table
        int linkID = getLinkIDList(linkParams, connLS);
        System.out.println("[Predict] Construct Intermediate Table Data");
        ArrayList<Object[]> intermediateParams = constructIntermediateParams(linkID, connSem);
        insertToIntermediateTable(intermediateParams, connLS);

        // Relation table
        HashMap<Integer, String> intermediateIDs = getIntermediateIDList(linkID, connLS);
        System.out.println("[Predict] Construct AB Relation Table Data");
        int relationType = 1; // ABRelation
        ArrayList<Object[]> ABRelationParams = constructRelationParams(relationType, intermediateIDs);
        insertToRelationTable(ABRelationParams, connLS);

        System.out.println("[Predict] Construct BC Relation Table Data");
        relationType = 2; // BCRelation
        ArrayList<Object[]> BCRelationParams = constructRelationParams(relationType, intermediateIDs);
        insertToRelationTable(BCRelationParams, connLS);

        // Relation_info table:
        System.out.println("[Predict] Construct AB RelationInfo Table Data");
        relationType = 1; // ABRelation
        HashMap<String, HashMap<String, Integer>> ABRelationIDsByIntermediate = getRelationIDList(relationType, intermediateIDs, connLS);
        ArrayList<Object[]> ABRelationInfoParams = constructRelationInfoParams(relationType, ABRelationIDsByIntermediate);
        insertToRelationInfoTable(ABRelationInfoParams, connLS);

        System.out.println("[Predict] Construct BC RelationInfo Table Data");
        relationType = 2; // BCRelation
        HashMap<String, HashMap<String, Integer>> BCRelationIDsByIntermediate = getRelationIDList(relationType, intermediateIDs, connLS);
        ArrayList<Object[]> BCRelationInfoParams = constructRelationInfoParams(relationType, BCRelationIDsByIntermediate);
        insertToRelationInfoTable(BCRelationInfoParams, connLS);
    }

    public void insertToRelationInfoTable(ArrayList<Object[]> params, Connection connLS) throws SQLException {
        System.out.println("[Predict] Insert into RelationInfo Table");
        String insertSql = "INSERT INTO relation_info(direction, year, pmid, relation_id) VALUES(?,?,?,?)";
        PreparedStatement ps = null;
        try {
            ps = connLS.prepareStatement(insertSql);
            for (Object[] param : params) {
                int direction = Integer.parseInt(param[0].toString());
                int year = Integer.parseInt(param[1].toString());
                String pmid = param[2].toString();
                int relationID = Integer.parseInt(param[3].toString());
                ps.setInt(1, direction);
                ps.setInt(2, year);
                ps.setString(3, pmid);
                ps.setInt(4, relationID);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ps.close();
        }
    }

    public HashMap<String, HashMap<String, Integer>> getRelationIDList(int relationType, HashMap<Integer, String> preTableIDs,
                                                                       Connection connLS) throws SQLException {
        System.out.println("[Predict] Get relation_id List");
        HashMap<String, HashMap<String, Integer>> relationIDsByIntermediate = new HashMap<>();
        String selectSql = "SELECT id, interaction FROM relation WHERE type=? and intermediate_id=?";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = connLS.prepareStatement(selectSql);
            for (int preTableID : preTableIDs.keySet()) {
                String intermediate = preTableIDs.get(preTableID);
                HashMap<String, Integer> relationIDs = new HashMap<>();
                ps.setInt(1, relationType);
                ps.setInt(2, preTableID);
                rs = ps.executeQuery();
                while (rs.next()) {
                    int relationID = Integer.parseInt(rs.getString("id"));
                    String interaction = rs.getString("interaction");
                    relationIDs.put(interaction, relationID);
                }
                relationIDsByIntermediate.put(intermediate, relationIDs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            rs.close();
            ps.close();
        }
        return relationIDsByIntermediate;
    }

    public void insertToRelationTable(ArrayList<Object[]> params, Connection connLS) throws SQLException {
        System.out.println("[Predict] Insert into Relation Table");
        String insertSql = "INSERT INTO relation(type, interaction, intermediate_id) VALUES(?,?,?)";
        PreparedStatement ps = null;
        try {
            ps = connLS.prepareStatement(insertSql);
            for (Object[] param : params) {
                int type = Integer.parseInt(param[0].toString());
                String interaction = param[1].toString();
                int intermediate_id = Integer.parseInt(param[2].toString());
                ps.setInt(1, type);
                ps.setString(2, interaction);
                ps.setInt(3, intermediate_id);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ps.close();
        }
    }

    public HashMap<Integer, String> getIntermediateIDList(int preTableID, Connection connLS) throws SQLException {
        System.out.println("[Predict] Get intermediate_id List");
        HashMap<Integer, String> IDsByPredicate = new HashMap<>();
        String selectSql = "SELECT id, b_name FROM intermediate WHERE link_id=?";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = connLS.prepareStatement(selectSql);
            ps.setInt(1, preTableID);
            rs = ps.executeQuery();
            while (rs.next()) {
                int id = Integer.parseInt(rs.getString("id"));
                String intermediate = rs.getString("b_name");
                IDsByPredicate.put(id, intermediate);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            rs.close();
            ps.close();
        }
        return IDsByPredicate;
    }

    public void insertToIntermediateTable(ArrayList<Object[]> params, Connection connLS) throws SQLException {
        System.out.println("[Predict] Insert into Intermediate Table");
        String insertSql = "INSERT INTO intermediate (b_id, b_name, importance, link_id) VALUES(?,?,?,?)";
        PreparedStatement ps = null;
        try {
            ps = connLS.prepareStatement(insertSql);
            for (Object[] param : params) {
                String b_id = param[0].toString();
                String b_name = param[1].toString();
                int link_id = Integer.parseInt(param[3].toString());
                ps.setString(1, b_id);
                ps.setString(2, b_name);
                ps.setNull(3, Types.INTEGER);
                ps.setInt(4, link_id);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ps.close();
        }
    }

    public int getLinkIDList(Object[] linkParams, Connection connLS) throws SQLException {
        System.out.println("[Predict] Get link_id");
        int linkID = 0;
        String selectSql = "SELECT id FROM link WHERE a_id=? and c_id=?";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = connLS.prepareStatement(selectSql);
            String a_id = linkParams[0].toString();
            String c_id = linkParams[2].toString();
            ps.setString(1, a_id);
            ps.setString(2, c_id);

            rs = ps.executeQuery();
            while (rs.next()) {
                linkID = Integer.parseInt(rs.getString("id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            rs.close();
            ps.close();
        }
        return linkID;
    }

    public void insertToLinkTable(Object[] params, Connection connLS) throws SQLException {
        System.out.println("[Predict] Insert into InterPattern Table");
        String insertSql = "INSERT INTO link (a_id, a_name, c_id, c_name, year, fda_year, flag) VALUES(?,?,?,?,?,?,?)";
        PreparedStatement ps = null;
        try {
            ps = connLS.prepareStatement(insertSql);
            String a_id = params[0].toString();
            String a_name = params[1].toString();
            String c_id = params[2].toString();
            String c_name = params[3].toString();
            int year = Integer.parseInt(params[4].toString());
            int fda_year = Integer.parseInt(params[5].toString());
            int flag = Integer.parseInt(params[6].toString());
            ps.setString(1, a_id);
            ps.setString(2, a_name);
            ps.setString(3, c_id);
            ps.setString(4, c_name);
            ps.setInt(5, year);
            ps.setInt(6, fda_year);
            ps.setInt(7, flag);
            ps.addBatch();
            ps.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            ps.close();
        }
    }
}
