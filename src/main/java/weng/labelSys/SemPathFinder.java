package weng.labelSys;

import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import weng.label.LabeledInfo;
import weng.node.LiteratureNode;
import weng.prepare.UmlsConcept;
import weng.util.dbAttributes.MySqlDb;
import weng.util.file.Predicate;
import weng.util.JDBCHelper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import weng.util.file.Stopwords;
import weng.util.predict.Disease;
import weng.util.predict.DiseaseScore;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by lbj23k on 2017/3/23.
 */

public class SemPathFinder {
    Logger logger = Logger.getAnonymousLogger();
    private final int startYear = 1809;
    private int endYear;
    private int fdaYear;
    private LiteratureNode pivotNode;
    private LiteratureNode targetNode;
    private HashMap<String, ArrayList<LabeledInfo>> ABrelation;
    private HashMap<String, ArrayList<LabeledInfo>> BCrelation;
    private List<String> importantNeighborLs;

    public SemPathFinder(String pivot, String target, int endYear, int fdaYear, Connection connSem, Connection connLit, HashMap<String, String> umlsCuiNameMap) throws IOException, SQLException {
        this.endYear = endYear;
        this.pivotNode = new LiteratureNode(pivot, startYear, endYear, connSem, connLit);
        this.targetNode = new LiteratureNode(target, startYear, endYear, connSem, connLit);
        this.fdaYear = fdaYear;
        ArrayList<HashMap<String, ArrayList<LabeledInfo>>> ABAndBCRelation = setABAndBCRelation(umlsCuiNameMap, connLit);
        this.ABrelation = ABAndBCRelation.get(0);
        this.BCrelation = ABAndBCRelation.get(1);
    }

    public SemPathFinder(boolean runOldModel, String pivot, String target, int endYear, int fdaYear, List<String> importantNeighborLs,
                         Connection connSem, Connection connLit, HashMap<String, String> umlsCuiNameMap) throws IOException, SQLException {
        this.endYear = endYear;
        this.pivotNode = new LiteratureNode(pivot, startYear, endYear, connSem, connLit);
        this.targetNode = new LiteratureNode(target, startYear, endYear, connSem, connLit);
        this.fdaYear = fdaYear;
        this.importantNeighborLs = importantNeighborLs;
        ArrayList<HashMap<String, ArrayList<LabeledInfo>>> ABAndBCRelation = setABAndBCRelation(runOldModel, umlsCuiNameMap, connLit);
        this.ABrelation = ABAndBCRelation.get(0);
        this.BCrelation = ABAndBCRelation.get(1);
    }

    public ArrayList<HashMap<String, ArrayList<LabeledInfo>>> setABAndBCRelation(boolean runOldModel,
                                                                                 HashMap<String, String> umlsCuiNameMap,
                                                                                 Connection connLit) throws IOException {
        LiteratureNode pivotNode = getPivotNode();
        LiteratureNode targetNode = getTargetNode();
        String a_cui = pivotNode.getCui();
        String c_cui = targetNode.getCui();
        int endYear = getEndYear();

        HashMap<String, ArrayList<LabeledInfo>> ABrelation = new HashMap<>();
        String sqlSelABRelation = "SELECT `o_cui` as `neighbor`, `predicate`, `pmid`, `year`, `is_exist` " +
                "FROM `umls_predication_aggregate_filtered` WHERE `s_cui`=? and `o_cui` in (SELECT `s_cui` " +
                "FROM `umls_predication_aggregate_filtered` WHERE `o_cui`=? and year between ? and ?) " +
                "and year between ? and ?";
        Object[] params_ab = {a_cui, c_cui, startYear, endYear, startYear, endYear};
        JDBCHelper jdbcHelper = new JDBCHelper();
        List<Map<String, Object>> result_ab = jdbcHelper.query(connLit, sqlSelABRelation, params_ab);

        HashMap<String, ArrayList<LabeledInfo>> BCrelation = new HashMap<>();
        String sqlSelBCRelation = "SELECT `s_cui` as `neighbor`, `predicate`, `pmid`, `year`, `is_exist` " +
                "FROM `umls_predication_aggregate_filtered` WHERE `o_cui`=? and `s_cui` in (SELECT `o_cui` " +
                "FROM `umls_predication_aggregate_filtered` WHERE `s_cui`=? and year between ? and ?) " +
                "and year between ? and ?";
        Object[] params_bc = {c_cui, a_cui, startYear, endYear, startYear, endYear};
        List<Map<String, Object>> result_bc = jdbcHelper.query(connLit, sqlSelBCRelation, params_bc);

        Set<String> neighborSet = new HashSet<>();
        for (Map row : result_ab) {
            neighborSet.add((String) row.get("neighbor"));
        }

        if (runOldModel) {
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
        } else {
            for (String intermCui : neighborSet) {
                String intermName = umlsCuiNameMap.get(intermCui);
                if (intermName != null) {
                    if (!Stopwords.set.contains(intermName)) {
                        ArrayList<LabeledInfo> tmp = setRelation(result_ab, intermCui);
                        ArrayList<LabeledInfo> tmp2 = setRelation(result_bc, intermCui);
                        if (tmp.size() > 0 && tmp2.size() > 0) {
                            ABrelation.put(intermName, tmp);
                            BCrelation.put(intermName, tmp2);
                        }
                    }
                }
            }
        }
        ArrayList<HashMap<String, ArrayList<LabeledInfo>>> ABAndBCRelation = new ArrayList<>();
        ABAndBCRelation.add(ABrelation);
        ABAndBCRelation.add(BCrelation);
        return ABAndBCRelation;
    }

    public ArrayList<HashMap<String, ArrayList<LabeledInfo>>> setABAndBCRelation(HashMap<String, String> umlsCuiNameMap,
                                                                                 Connection connLit) throws IOException {
        LiteratureNode pivotNode = getPivotNode();
        LiteratureNode targetNode = getTargetNode();
        String a_cui = pivotNode.getCui();
        String c_cui = targetNode.getCui();
        int endYear = getEndYear();

        HashMap<String, ArrayList<LabeledInfo>> ABrelation = new HashMap<>();
        String sqlSelABRelation = "SELECT `o_cui` as `neighbor`, `predicate`, `pmid`, `year`, `is_exist` " +
                "FROM `umls_predication_aggregate_filtered` WHERE `s_cui`=? and `o_cui` in (SELECT `s_cui` " +
                "FROM `umls_predication_aggregate_filtered` WHERE `o_cui`=? and year between ? and ?) " +
                "and year between ? and ?";
        Object[] params_ab = {a_cui, c_cui, startYear, endYear, startYear, endYear};
        JDBCHelper jdbcHelper = new JDBCHelper();
        List<Map<String, Object>> result_ab = jdbcHelper.query(connLit, sqlSelABRelation, params_ab);

        HashMap<String, ArrayList<LabeledInfo>> BCrelation = new HashMap<>();
        String sqlSelBCRelation = "SELECT `s_cui` as `neighbor`, `predicate`, `pmid`, `year`, `is_exist` " +
                "FROM `umls_predication_aggregate_filtered` WHERE `o_cui`=? and `s_cui` in (SELECT `o_cui` " +
                "FROM `umls_predication_aggregate_filtered` WHERE `s_cui`=? and year between ? and ?) " +
                "and year between ? and ?";
        Object[] params_bc = {c_cui, a_cui, startYear, endYear, startYear, endYear};
        List<Map<String, Object>> result_bc = jdbcHelper.query(connLit, sqlSelBCRelation, params_bc);

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
                                              String intermCui) throws IOException {
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

    public void setImportantNeighborLs(List<String> importantNeighborLs) {
        this.importantNeighborLs = importantNeighborLs;
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

    public List<String> getImportantNeighborLs() {
        return importantNeighborLs;
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

    public void writeJson(String outputFile, List<SemPathFinder> semPathFinders) {
        JSONObject jsonObj = new JSONObject();
        JSONObject interaction = new JSONObject();
        jsonObj.put("conceptA", pivotNode.getName());
        jsonObj.put("conceptC", targetNode.getName());
        jsonObj.put("year", getEndYear());
        jsonObj.put("fda_year", getFdaYear());

        for (String bName : getABrelation().keySet()) {
            ArrayList<LabeledInfo> abs = getABrelation().get(bName);
            ArrayList<LabeledInfo> bcs = getBCrelation().get(bName);
            JSONArray interInfo = new JSONArray();
            for (LabeledInfo interm : abs) {
                JSONArray relationInfo = new JSONArray();
                relationInfo.add(1);
                relationInfo.add(interm.getDirection());
                relationInfo.add(interm.getPredicate());
                relationInfo.add(interm.getYear());
                relationInfo.add(interm.getPmid());
                interInfo.add(relationInfo);
            }

            for (LabeledInfo interm : bcs) {
                JSONArray relationInfo = new JSONArray();
                relationInfo.add(2);
                relationInfo.add(interm.getDirection());
                relationInfo.add(interm.getPredicate());
                relationInfo.add(interm.getYear());
                relationInfo.add(interm.getPmid());
                interInfo.add(relationInfo);
            }
            interaction.put(bName, interInfo);
        }
        jsonObj.put("relation", interaction);
        try {
            FileWriter file = new FileWriter(outputFile);
            file.write(jsonObj.toString());
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeJson(String outputFile) {
        JSONObject jsonObj = new JSONObject();
        JSONObject interaction = new JSONObject();
        jsonObj.put("conceptA", pivotNode.getName());
        jsonObj.put("conceptC", targetNode.getName());
        jsonObj.put("year", getEndYear());
        jsonObj.put("fda_year", getFdaYear());

        for (String bName : getABrelation().keySet()) {
            ArrayList<LabeledInfo> abs = getABrelation().get(bName);
            ArrayList<LabeledInfo> bcs = getBCrelation().get(bName);
            JSONArray interInfo = new JSONArray();
            for (LabeledInfo interm : abs) {
                JSONArray relationInfo = new JSONArray();
                relationInfo.add(1);
                relationInfo.add(interm.getDirection());
                relationInfo.add(interm.getPredicate());
                relationInfo.add(interm.getYear());
                relationInfo.add(interm.getPmid());
                interInfo.add(relationInfo);
            }

            for (LabeledInfo interm : bcs) {
                JSONArray relationInfo = new JSONArray();
                relationInfo.add(2);
                relationInfo.add(interm.getDirection());
                relationInfo.add(interm.getPredicate());
                relationInfo.add(interm.getYear());
                relationInfo.add(interm.getPmid());
                interInfo.add(relationInfo);
            }
            interaction.put(bName, interInfo);
        }
        jsonObj.put("relation", interaction);
        try {
            FileWriter file = new FileWriter(outputFile);
            file.write(jsonObj.toString());
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeCsv(String path, List<SemPathFinder> semPathFinders) {
        ArrayList<String[]> records = getAllInfoOfPredicate(semPathFinders);
        records = getAllFreqOfPredicate(records, semPathFinders);
        CSVWriter csvWriter = getCSVWriter(path);
        csvWriter.writeAll(records);
        try {
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeCsv(String path) {
        ArrayList<String[]> records = getAllInfoOfPredicate();
        records = getAllFreqOfPredicate(records);
        CSVWriter csvWriter = getCSVWriter(path);
        csvWriter.writeAll(records);
        try {
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public ArrayList<String[]> getAllInfoOfPredicate(List<SemPathFinder> semPathFinders) {
        int totalB = semPathFinders.stream().map(e -> e.getABrelation().size()).reduce(0, (x, y) -> x + y);
        String[] title =
                {"A", "A-B predicate (Year, PMID)", "B(" + totalB + " intermediates)",
                        "B-C predicate (Year, PMID)", "C",
                        "First direct indication year(" + (semPathFinders.get(0).getEndYear() + 1) + ")",
                        "FDA approved in " + semPathFinders.get(0).getFdaYear()};

        ArrayList<String[]> records = new ArrayList<>();
        records.add(title);
        for (SemPathFinder semPathFinder : semPathFinders) {
            for (String intermName : semPathFinder.getABrelation().keySet()) {
                ArrayList<LabeledInfo> abs = semPathFinder.getABrelation().get(intermName);
                ArrayList<LabeledInfo> bcs = semPathFinder.getBCrelation().get(intermName);
                String[] row = new String[5];
                row[0] = semPathFinder.getPivotNode().getName();
                row[1] = abs.get(0).toString();
                row[2] = intermName;
                row[3] = bcs.get(0).toString();
                row[4] = semPathFinder.getTargetNode().getName();
                records.add(row);
                row = new String[5];

                int maxSize = Math.max(abs.size(), bcs.size());
                for (int j = 1; j < maxSize; j++) {
                    if (j < abs.size() && j < bcs.size()) {
                        row[1] = abs.get(j).toString();
                        row[3] = bcs.get(j).toString();
                    } else if (j < abs.size()) {
                        row[1] = abs.get(j).toString();
                    } else if (j < bcs.size()) {
                        row[3] = bcs.get(j).toString();
                    }
                    records.add(row);
                    row = new String[5];
                }
            }
        }
        return records;
    }

    public ArrayList<String[]> getAllInfoOfPredicate() {
        int firstYear = getEndYear() + 1;
        //Create record
        String[] title =
                {"A", "A-B predicate (Year, PMID)", "B(" + getABrelation().size() + " intermediates)",
                        "B-C predicate (Year, PMID)", "C",
                        "First direct indication year(" + firstYear + ")",
                        "FDA approved in " + getFdaYear()};

        ArrayList<String[]> records = new ArrayList<>();
        records.add(title);
        for (String intermName : getABrelation().keySet()) {
            ArrayList<LabeledInfo> abs = getABrelation().get(intermName);
            ArrayList<LabeledInfo> bcs = getBCrelation().get(intermName);
            String[] row = new String[5];
            row[0] = getPivotNode().getName();
            row[1] = abs.get(0).toString();
            row[2] = intermName;
            row[3] = bcs.get(0).toString();
            row[4] = getTargetNode().getName();
            records.add(row);
            row = new String[5];

            int maxSize = Math.max(abs.size(), bcs.size());
            for (int j = 1; j < maxSize; j++) {
                if (j < abs.size() && j < bcs.size()) {
                    row[1] = abs.get(j).toString();
                    row[3] = bcs.get(j).toString();
                } else if (j < abs.size()) {
                    row[1] = abs.get(j).toString();
                } else if (j < bcs.size()) {
                    row[3] = bcs.get(j).toString();
                }
                records.add(row);
                row = new String[5];
            }
        }
        return records;
    }

    public ArrayList<String[]> getAllFreqOfPredicate(ArrayList<String[]> records,
                                                     List<SemPathFinder> semPathFinders) {
        int totalB = semPathFinders.stream().map(e -> e.getABrelation().size()).reduce(0, (x, y) -> x + y);
        String[] title2 =
                {"A", "A-B predicate (count)", "B(" + totalB + " intermediates)",
                        "B-C predicate (count)", "C",
                        "First direct indication year(" + (semPathFinders.get(0).getEndYear() + 1) + ")",
                        "FDA approved in " + semPathFinders.get(0).getFdaYear()};
        records.add(new String[5]);
        records.add(title2);
        for (SemPathFinder semPathFinder : semPathFinders) {
            for (String intermName : semPathFinder.getABrelation().keySet()) {
                LinkedHashMultiset absHmap = LinkedHashMultiset.create(semPathFinder.getABrelation().get(intermName));
                LinkedHashMultiset bcsHmap = LinkedHashMultiset.create(semPathFinder.getBCrelation().get(intermName));
                Iterator<Multiset.Entry<LabeledInfo>> abIt = absHmap.entrySet().iterator();
                Iterator<Multiset.Entry<LabeledInfo>> bcIt = bcsHmap.entrySet().iterator();
                Multiset.Entry<LabeledInfo> abItem = abIt.next();
                Multiset.Entry<LabeledInfo> bcItem = bcIt.next();
                String[] row = new String[5];
                row[0] = semPathFinder.getPivotNode().getName();
                row[1] = abItem.getElement().getString() + "(" + abItem.getCount() + ")";
                row[2] = intermName;
                row[3] = bcItem.getElement().getString() + "(" + bcItem.getCount() + ")";
                row[4] = semPathFinder.getTargetNode().getName();
                records.add(row);
                row = new String[5];
                int maxSize = Math.max(absHmap.entrySet().size(), bcsHmap.entrySet().size());
                for (int j = 1; j < maxSize; j++) {
                    if (j < absHmap.entrySet().size() && j < bcsHmap.entrySet().size()) {
                        abItem = abIt.next();
                        bcItem = bcIt.next();
                        row[1] = abItem.getElement().getString() + "(" + abItem.getCount() + ")";
                        row[3] = bcItem.getElement().getString() + "(" + bcItem.getCount() + ")";
                    } else if (j < absHmap.entrySet().size()) {
                        abItem = abIt.next();
                        row[1] = abItem.getElement().getString() + "(" + abItem.getCount() + ")";
                    } else if (j < bcsHmap.entrySet().size()) {
                        bcItem = bcIt.next();
                        row[3] = bcItem.getElement().getString() + "(" + bcItem.getCount() + ")";
                    }
                    records.add(row);
                    row = new String[5];
                }
            }
        }
        return records;
    }

    public ArrayList<String[]> getAllFreqOfPredicate(ArrayList<String[]> records) {
        int firstYear = getEndYear() + 1;
        String[] title2 =
                {"A", "A-B predicate (count)", "B(" + getABrelation().size() + " intermediates)",
                        "B-C predicate (count)", "C",
                        "First direct indication year(" + firstYear + ")",
                        "FDA approved in " + getFdaYear()};
        records.add(new String[5]);
        records.add(title2);
        for (String intermName : getABrelation().keySet()) {
            LinkedHashMultiset absHmap = LinkedHashMultiset.create(getABrelation().get(intermName));
            LinkedHashMultiset bcsHmap = LinkedHashMultiset.create(getBCrelation().get(intermName));
            Iterator<Multiset.Entry<LabeledInfo>> abIt = absHmap.entrySet().iterator();
            Iterator<Multiset.Entry<LabeledInfo>> bcIt = bcsHmap.entrySet().iterator();
            Multiset.Entry<LabeledInfo> abItem = abIt.next();
            Multiset.Entry<LabeledInfo> bcItem = bcIt.next();
            String[] row = new String[5];
            row[0] = getPivotNode().getName();
            row[1] = abItem.getElement().getString() + "(" + abItem.getCount() + ")";
            row[2] = intermName;
            row[3] = bcItem.getElement().getString() + "(" + bcItem.getCount() + ")";
            row[4] = getTargetNode().getName();
            records.add(row);
            row = new String[5];
            int maxSize = Math.max(absHmap.entrySet().size(), bcsHmap.entrySet().size());
            for (int j = 1; j < maxSize; j++) {
                if (j < absHmap.entrySet().size() && j < bcsHmap.entrySet().size()) {
                    abItem = abIt.next();
                    bcItem = bcIt.next();
                    row[1] = abItem.getElement().getString() + "(" + abItem.getCount() + ")";
                    row[3] = bcItem.getElement().getString() + "(" + bcItem.getCount() + ")";
                } else if (j < absHmap.entrySet().size()) {
                    abItem = abIt.next();
                    row[1] = abItem.getElement().getString() + "(" + abItem.getCount() + ")";
                } else if (j < bcsHmap.entrySet().size()) {
                    bcItem = bcIt.next();
                    row[3] = bcItem.getElement().getString() + "(" + bcItem.getCount() + ")";
                }
                records.add(row);
                row = new String[5];
            }
        }
        return records;
    }

    public Object[] constructLinkParams() {
        LiteratureNode pivotNode = getPivotNode();
        String a_id = pivotNode.getCui();
        String a_name = pivotNode.getName();
        LiteratureNode targetNode = getTargetNode();
        String c_id = targetNode.getCui();
        String c_name = targetNode.getName();
        int year = getEndYear();
        int fda_year = -1;
        int flag = 1;
        return new Object[]{a_id, a_name, c_id, c_name, year, fda_year, flag};
    }

    public Object[] diseaseParams(int predictDrugId) {
        LiteratureNode targetNode = getTargetNode();
        String c_name = targetNode.getName();
        return new Object[]{c_name, predictDrugId};
    }

    public ArrayList<Object[]> intermediateParams(int predictDrugId, Connection connSem) throws SQLException {
        ArrayList<Object[]> paramsList = new ArrayList<>();
        HashMap<String, String> umlsNameCuiMap = UmlsConcept.getUmlsNameCuiMap(connSem);
        for (String intermName : getABrelation().keySet()) {
            String b_cui = umlsNameCuiMap.get(intermName);
            int importance = (getImportantNeighborLs().contains(b_cui)) ? 3 : 0;
            Object[] param = {b_cui, intermName, importance, predictDrugId};
            paramsList.add(param);
        }
        return paramsList;
    }

    public ArrayList<Object[]> relationParams(int relationType, HashMap<Integer, String> intermediateIDs) {
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

    public ArrayList<Object[]> relationInfoParams(int relationType, HashMap<String, HashMap<String, Integer>> relationIDsByIntermediate) {
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

    public void insertToLabelSystem(Connection connLS, Connection connSem,
                                    int predictDrugId) throws SQLException {
        // disease table
        Object[] diseaseParams = diseaseParams(predictDrugId);
        insertDisease(diseaseParams, connLS);

        // Intermediate table
        logger.info("[Intermediate] Construction");
        int diseaseId = diseaseIDs(diseaseParams, connLS);
        ArrayList<Object[]> intermediateParams = intermediateParams(diseaseId, connSem);
        insertIntermediate(intermediateParams, connLS);

        // Relation table
        HashMap<Integer, String> intermediateIDs = intermediateIDs(diseaseId, connLS);
        logger.info("[AB Relation] Construction");
        int relationType = 1; // ABRelation
        ArrayList<Object[]> ABRelationParams = relationParams(relationType, intermediateIDs);
        insertRelation(ABRelationParams, connLS);

        logger.info("[BC Relation] Construction");
        relationType = 2; // BCRelation
        ArrayList<Object[]> BCRelationParams = relationParams(relationType, intermediateIDs);
        insertRelation(BCRelationParams, connLS);

        // Relation_info table:
        logger.info("[AB RelationInfo] Construction");
        relationType = 1; // ABRelation
        HashMap<String, HashMap<String, Integer>> ABRelationIDsByIntermediate = relationIDs(relationType, intermediateIDs, connLS);
        ArrayList<Object[]> ABRelationInfoParams = relationInfoParams(relationType, ABRelationIDsByIntermediate);
        insertRelationInfo(ABRelationInfoParams, connLS);

        logger.info("[BC RelationInfo] Construction");
        relationType = 2; // BCRelation
        HashMap<String, HashMap<String, Integer>> BCRelationIDsByIntermediate = relationIDs(relationType, intermediateIDs, connLS);
        ArrayList<Object[]> BCRelationInfoParams = relationInfoParams(relationType, BCRelationIDsByIntermediate);
        insertRelationInfo(BCRelationInfoParams, connLS);
    }

//    public void insertToLabelSystem(Connection connLS, Connection connSem) throws SQLException {
//        // InterPattern table
//        Object[] linkParams = constructLinkParams();
//        insertToLinkTable(linkParams, connLS);
//
//        // Intermediate table
//        int linkID = getLinkIDList(linkParams, connLS);
//        logger.info("[Predict] Construct Intermediate Table Data");
//        ArrayList<Object[]> intermediateParams = intermediateParams(linkID, connSem);
//        insertIntermediate(intermediateParams, connLS);
//
//        // Relation table
//        HashMap<Integer, String> intermediateIDs = intermediateIDs(linkID, connLS);
//        logger.info("[Predict] Construct AB Relation Table Data");
//        int relationType = 1; // ABRelation
//        ArrayList<Object[]> ABRelationParams = relationParams(relationType, intermediateIDs);
//        insertRelation(ABRelationParams, connLS);
//
//        logger.info("[Predict] Construct BC Relation Table Data");
//        relationType = 2; // BCRelation
//        ArrayList<Object[]> BCRelationParams = relationParams(relationType, intermediateIDs);
//        insertRelation(BCRelationParams, connLS);
//
//        // Relation_info table:
//        logger.info("[Predict] Construct AB RelationInfo Table Data");
//        relationType = 1; // ABRelation
//        HashMap<String, HashMap<String, Integer>> ABRelationIDsByIntermediate = relationIDs(relationType, intermediateIDs, connLS);
//        ArrayList<Object[]> ABRelationInfoParams = relationInfoParams(relationType, ABRelationIDsByIntermediate);
//        insertRelationInfo(ABRelationInfoParams, connLS);
//
//        logger.info("[Predict] Construct BC RelationInfo Table Data");
//        relationType = 2; // BCRelation
//        HashMap<String, HashMap<String, Integer>> BCRelationIDsByIntermediate = relationIDs(relationType, intermediateIDs, connLS);
//        ArrayList<Object[]> BCRelationInfoParams = relationInfoParams(relationType, BCRelationIDsByIntermediate);
//        insertRelationInfo(BCRelationInfoParams, connLS);
//    }

    public void insertRelationInfo(ArrayList<Object[]> params, Connection connLS) {
        logger.info("[RelationInfo] Insert");
        String insertSql = "INSERT INTO `" + MySqlDb.predict_relation_info + "` (direction, year, pmid, relation_id) VALUES(?,?,?,?)";
        try {
            PreparedStatement ps = connLS.prepareStatement(insertSql);
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
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public HashMap<String, HashMap<String, Integer>> relationIDs(int relationType, HashMap<Integer, String> preTableIDs,
                                                                 Connection connLS) throws SQLException {
        logger.info("[Predict] Get relation_id List");
        HashMap<String, HashMap<String, Integer>> relationIDsByIntermediate = new HashMap<>();
        String selectSql = "SELECT id, interaction FROM `" + MySqlDb.predict_relation + "` WHERE type=? and intermediate_id=?";
        try {
            PreparedStatement ps = connLS.prepareStatement(selectSql);
            for (int preTableID : preTableIDs.keySet()) {
                String intermediate = preTableIDs.get(preTableID);
                HashMap<String, Integer> relationIDs = new HashMap<>();
                ps.setInt(1, relationType);
                ps.setInt(2, preTableID);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    int relationID = Integer.parseInt(rs.getString("id"));
                    String interaction = rs.getString("interaction");
                    relationIDs.put(interaction, relationID);
                }
                rs.close();
                relationIDsByIntermediate.put(intermediate, relationIDs);
            }
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return relationIDsByIntermediate;
    }

    public void insertRelation(ArrayList<Object[]> params, Connection connLS) throws SQLException {
        logger.info("[Relation] Insert");
        String insertSql = "INSERT INTO `" + MySqlDb.predict_relation + "` (type, interaction, intermediate_id) VALUES(?,?,?)";
        try {
            PreparedStatement ps = connLS.prepareStatement(insertSql);
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
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public HashMap<Integer, String> intermediateIDs(int preTableID, Connection connLS) throws SQLException {
        logger.info("[Predict] Get intermediate_id List");
        HashMap<Integer, String> IDsByPredicate = new HashMap<>();
        String selectSql = "SELECT id, b_name FROM `" + MySqlDb.predict_intermediate + "` WHERE `pre_disease_id`=?";
        try {
            PreparedStatement ps = connLS.prepareStatement(selectSql);
            ps.setInt(1, preTableID);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int id = Integer.parseInt(rs.getString("id"));
                String intermediate = rs.getString("b_name");
                IDsByPredicate.put(id, intermediate);
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return IDsByPredicate;
    }

    public void insertIntermediate(ArrayList<Object[]> params, Connection connLS) {
        logger.info("[Intermediate] Insert");
        String insertSql = "INSERT INTO `" + MySqlDb.predict_intermediate + "` (b_id, b_name, importance, pre_disease_id) VALUES(?,?,?,?)";
        try {
            PreparedStatement ps = connLS.prepareStatement(insertSql);
            for (Object[] param : params) {
                String b_cui = param[0].toString();
                String b_name = param[1].toString();
                int importance = Integer.parseInt(param[2].toString());
                int predictDrugId = Integer.parseInt(param[3].toString());
                ps.setString(1, b_cui);
                ps.setString(2, b_name);
                ps.setInt(3, importance);
                ps.setInt(4, predictDrugId);
                ps.addBatch();
            }
            ps.executeBatch();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getLinkIDList(Object[] linkParams, Connection connLS) {
        System.out.println("[Predict] Get link_id");
        int linkID = 0;
        String selectSql = "SELECT id FROM link WHERE a_id=? and c_id=?";
        try {
            PreparedStatement ps = connLS.prepareStatement(selectSql);
            String a_id = linkParams[0].toString();
            String c_id = linkParams[2].toString();
            ps.setString(1, a_id);
            ps.setString(2, c_id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                linkID = Integer.parseInt(rs.getString("id"));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return linkID;
    }

    public int diseaseIDs(Object[] linkParams, Connection connLS) {
        logger.info("[Disease] Get id");
        int linkID = 0;
        String selectSql = "SELECT id FROM `" + MySqlDb.predict_diseases + "` WHERE name=? and pre_d_id=?";
        try {
            PreparedStatement ps = connLS.prepareStatement(selectSql);
            String name = linkParams[0].toString();
            int pre_d_id = Integer.parseInt(linkParams[1].toString());
            ps.setString(1, name);
            ps.setInt(2, pre_d_id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                linkID = Integer.parseInt(rs.getString("id"));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return linkID;
    }

    public void insertToLinkTable(Object[] params, Connection connLS) throws SQLException {
        System.out.println("[Predict] Insert into InterPattern Table");
        String insertSql = "INSERT INTO link (a_id, a_name, c_id, c_name, year, fda_year, flag) VALUES(?,?,?,?,?,?,?)";
        try {
            PreparedStatement ps = connLS.prepareStatement(insertSql);
            String a_cui = params[0].toString();
            String a_name = params[1].toString();
            String c_cui = params[2].toString();
            String c_name = params[3].toString();
            int year = Integer.parseInt(params[4].toString());
            int fda_year = Integer.parseInt(params[5].toString());
            int flag = Integer.parseInt(params[6].toString());
            ps.setString(1, a_cui);
            ps.setString(2, a_name);
            ps.setString(3, c_cui);
            ps.setString(4, c_name);
            ps.setInt(5, year);
            ps.setInt(6, fda_year);
            ps.setInt(7, flag);
            ps.addBatch();
            ps.executeBatch();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertDisease(Object[] params, Connection connLS) throws SQLException {
        logger.info("[Disease] Insert");
        String insertSql = "INSERT INTO `" + MySqlDb.predict_diseases + "` (name,pre_d_id) VALUES (?,?)";
        try {
            PreparedStatement ps = connLS.prepareStatement(insertSql);
            String c_name = params[0].toString();
            int predictDrugId = Integer.parseInt(params[1].toString());
            ps.setString(1, c_name);
            ps.setInt(2, predictDrugId);
            ps.addBatch();
            ps.executeBatch();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
