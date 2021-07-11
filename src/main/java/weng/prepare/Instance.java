package weng.prepare;

import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.mongodb.*;
import weka.core.Instances;
import weng.evaluation.IndexScore;
import weng.feature.SemanticType;
import weng.network.PredictNetwork;
import weng.node.LiteratureNode;
import weng.util.JDBCHelper;
import weng.util.Utils;
import weng.util.noSqlColumn.Predication;
import weng.util.file.RepurposingEval;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by lbj23k on 2017/6/22.
 */
public class Instance {
    private Logger logger = Logger.getAnonymousLogger();

    public void createInstFile(boolean runOldModel, Connection connSem,
                               HashMap<String, String> umlsCuiNameMap,
                               HashMap<String, String> umlsNameCuiMap,
                               Connection connLit, DBCollection collInfo, DBCollection collRef) throws IOException, SQLException {
        Utils utils = new Utils();
        Map<String, Map<String, IndexScore>> perfectRank = new HashMap<>();
        for (int i = 1; i <= 100; i++) {
            int fileNum = 500 * i;
            perfectRank.putAll((Map<String, Map<String, IndexScore>>) utils.readObjectFile(RepurposingEval.goldenRankSplit_noSql + fileNum + ".dat"));
        }
        List<String> drugs = new ArrayList<>(perfectRank.keySet());
        System.out.println(drugs.size());
        int count = 0;
        Map<String, Integer> neighborCount = neighborCount();
        for (String drug : drugs) {
            if (!drug.contains("/")) {
                System.out.println("\n" + ++count + ":" + drug);
                Map<String, IndexScore> perfectDrugRank = perfectRank.get(drug);
                Map<String, Instances> diseaseMap = new HashMap<>();
                int diseaseCount = 0;
                for (String disease : perfectDrugRank.keySet()) {
                    ++diseaseCount;
                    if (diseaseCount % 10 == 0) {
                        System.out.print("-d " + diseaseCount);
                    }
                    HashMap<String, HashSet<String>> semGroupsByIntermediate = getSemGroupsByIntermediate(drug, disease, connSem);
                    HashMap<String, HashMap<String, Integer>> semGroupMatrixByIntermediate = getSemGroupMatrixByIntermediate(semGroupsByIntermediate);
                    PredictNetwork predicateNet = new PredictNetwork(runOldModel, neighborCount, drug, disease, 1809, 2004, umlsCuiNameMap, umlsNameCuiMap, connLit, semGroupMatrixByIntermediate, collInfo, collRef);
                    diseaseMap.put(disease, predicateNet.getInst());
                }

                if (runOldModel) {
                    utils.writeObject(diseaseMap, RepurposingEval.instance_oldFeatureTo100 + drug + ".dat");
                } else {
                    utils.writeObject(diseaseMap, RepurposingEval.instance_newFeatureTo100 + drug + ".dat");
                }
            }
        }
    }

    public Map<String, Integer> neighborCount() throws IOException {
        Map<String, Integer> neighborCount = new HashMap<>();
        FileReader fileReader = new FileReader(new File(RepurposingEval.neighborCount));
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            String[] info = line.split("\t");
            neighborCount.put(info[0], Integer.parseInt(info[1]));
        }
        bufferedReader.close();
        fileReader.close();
        return neighborCount;
    }

    public void predicationCount(HashMap<String, String> umlsCuiNameMap,
                                 HashMap<String, String> umlsNameCuiMap,
                                 Connection connLit, DBCollection collection) throws IOException, SQLException {
        Utils utils = new Utils();
        Map<String, Map<String, IndexScore>> perfectRank = new HashMap<>();
        for (int i = 1; i <= 100; i++) {
            int fileNum = 500 * i;
            perfectRank.putAll((Map<String, Map<String, IndexScore>>) utils.readObjectFile(RepurposingEval.goldenRankSplit_noSql + fileNum + ".dat"));
        }
        List<String> drugs = new ArrayList<>(perfectRank.keySet());
        int count = 0;
        for (String drug : drugs) {
            System.out.println("\n" + ++count + ":" + drug);
            Map<String, IndexScore> perfectDrugRank = perfectRank.get(drug);
            int diseaseCount = 0;
            for (String disease : perfectDrugRank.keySet()) {
                ++diseaseCount;
                if (diseaseCount % 10 == 0) {
                    System.out.print("-d " + diseaseCount);
                }
                PredictNetwork predicateNet = new PredictNetwork(drug, disease, 1809, 2004, umlsCuiNameMap, umlsNameCuiMap, connLit, collection);
            }
        }
    }

    public HashMap<String, HashMap<String, Integer>> getSemGroupMatrixByIntermediate(HashMap<String, HashSet<String>> semGroupsByIntermediate) {
        HashMap<String, HashMap<String, Integer>> semGroupMatrixByIntermediate = new HashMap<>();
        for (String interm : semGroupsByIntermediate.keySet()) {
            HashSet<String> semGroups = semGroupsByIntermediate.get(interm);
            SemanticType semanticType = new SemanticType();
            HashMap<String, Integer> semGroupAppearedList = semanticType.initSemGroupAppearedList();
            for (String semType : semGroups) {
                semGroupAppearedList.put(semType, 1);
            }
            semGroupMatrixByIntermediate.put(interm, semGroupAppearedList);
        }
        return semGroupMatrixByIntermediate;
    }

    public HashMap<String, HashSet<String>> getSemGroupsByIntermediate(String a_name, String c_name,
                                                                       Connection connSem) {
        HashMap<String, HashSet<String>> semGroupsByIntermediate = new HashMap<>();
        String sqlSelSemType = "select `b_name`, `sg`.`SEMGROUP` from `label_system`.`prediction_view` AS `pv` " +
                "inner join `label_system`.`intermediate` as `in` on `pv`.`id`=`in`.`link_id` " +
                "inner join `semmed_ver26`.`concept` AS `con` on `con`.`PREFERRED_NAME`=`in`.`b_name` " +
                "inner join `semmed_ver26`.`concept_semtype` AS `cs` on `cs`.`CONCEPT_ID`=`con`.`CONCEPT_ID` " +
                "inner join `semmed_ver26`.`semanticType` AS `st` on `cs`.`SEMTYPE`=`st`.`SEMTYPE` " +
                "inner join `semmed_ver26`.`semanticGroup` AS `sg` on `st`.`SEMTYPE_ID`=`sg`.`SEMTYPE_ID` " +
                "where `pv`.`count_b`=`pv`.`label_count` AND `flag`=0 AND `con`.`TYPE`=\"META\" " +
                "AND `pv`.`a_name`=? AND `pv`.`c_name`=?;";
        Object[] params = {a_name, c_name};
        JDBCHelper jdbcHelper = new JDBCHelper();
        List<Map<String, Object>> ls = jdbcHelper.query(connSem, sqlSelSemType, params);
        for (Map<String, Object> row : ls) {
            String intermName = (String) row.get("b_name");
            String semGroup = (String) row.get("SEMGROUP");
            HashSet<String> semGroups = new HashSet<>();
            if (semGroupsByIntermediate.keySet().contains(intermName)) {
                semGroups = semGroupsByIntermediate.get(intermName);
            }
            semGroups.add(semGroup);
            semGroupsByIntermediate.put(intermName, semGroups);
        }
        return semGroupsByIntermediate;
    }

    public void createGoldenFile(Connection connSem, Connection connLit,
                                 HashMap<String, String> umlsNameCuiMap,
                                 HashMap<String, String> umlsCuiNameMap,
                                 DBCollection collNeighborCoOccurPre, DBCollection collNeighborCoOccurPre_ref,
                                 DBCollection collNeighborCoOccurPost, DBCollection collNeighborCoOccurPost_ref,
                                 DBCollection collPredicationPre_s, DBCollection collPredicationPre_s_ref,
                                 DBCollection collPredicationPost_s, DBCollection collPredicationPost_s_ref) throws SQLException {
        Set<String> diseases = new LinkedHashSet<>(UmlsConcept.disorderSeeds);
        LinkedHashMap<String, Map<String, Integer>> goldenMap = new LinkedHashMap<>();
        int count = 0;
        for (String drug : new HashSet<>(UmlsConcept.chemiDrugSeeds)) {
            String drugCui = umlsNameCuiMap.get(drug);
            logger.info(++count + ":" + drug);
            if (count > 463662) {
                LiteratureNode nodePre = new LiteratureNode(drug, 1809, 2004, connSem, connLit);
                LiteratureNode nodePost = new LiteratureNode(drug, 2005, 2016, connSem, connLit);

                Map<String, Multiset<String>> predicateNeighborPost = nodePost.getPredicateNeighbors(umlsCuiNameMap, collPredicationPost_s, collPredicationPost_s_ref);
                Set<String> cooccurNeighborsPre = nodePre.getCooccurNeighbors(umlsCuiNameMap, collNeighborCoOccurPre, collNeighborCoOccurPre_ref).keySet();
                Set<String> cooccurNeighborsPost = nodePost.getCooccurNeighbors(umlsCuiNameMap, collNeighborCoOccurPost, collNeighborCoOccurPost_ref).keySet();

                Set<String> evalSet = Sets.difference(cooccurNeighborsPost, cooccurNeighborsPre);
                Set<String> evalDiseaseSet = Sets.intersection(evalSet, diseases);

                Set<String> possibleBsByDrug = possibleBs(Predication.cui, drugCui, collPredicationPre_s_ref, collPredicationPre_s);
                for (String disease : evalDiseaseSet) {
                    String diseaseCui = umlsNameCuiMap.get(disease);
                    if (!hasIntermediates(possibleBsByDrug, diseaseCui, collPredicationPre_s, collPredicationPre_s_ref))
                        continue;
                    if (predicateNeighborPost.containsKey(disease)) {
                        Multiset<String> diseasePredicate = predicateNeighborPost.get(disease);
                        int diseaseCount = diseasePredicate.size();
                        if (diseasePredicate.count("TREATS") / (double) diseaseCount > 0.5) {
                            Map<String, Integer> rankMap = goldenMap.getOrDefault(drug, new LinkedHashMap<>());
                            rankMap.put(disease, 3);
                            goldenMap.put(drug, rankMap);
                        } else {
                            Map<String, Integer> rankMap = goldenMap.getOrDefault(drug, new LinkedHashMap<>());
                            rankMap.put(disease, 2);
                            goldenMap.put(drug, rankMap);
                        }
                    } else {
                        if (cooccurNeighborsPost.contains(disease)) {
                            Map<String, Integer> rankMap = goldenMap.getOrDefault(drug, new LinkedHashMap<>());
                            rankMap.put(disease, 1);
                            goldenMap.put(drug, rankMap);
                        }
                    }
                }

                Set<String> uselessDiseaseSet = Sets.difference(diseases, evalDiseaseSet);
                if (goldenMap.containsKey(drug) && goldenMap.get(drug).values().contains(3)) {
                    for (String uselessDisease : uselessDiseaseSet) {
                        if (cooccurNeighborsPre.contains(uselessDisease)) {
                            continue;
                        }
                        String uselessDiseaseCui = umlsNameCuiMap.get(uselessDisease);
                        if (!hasIntermediates(possibleBsByDrug, uselessDiseaseCui, collPredicationPre_s, collPredicationPre_s_ref))
                            continue;
                        Map<String, Integer> rankMap = goldenMap.getOrDefault(drug, new LinkedHashMap<>());
                        rankMap.put(uselessDisease, 0);
                        goldenMap.put(drug, rankMap);
                    }
                }
                if ((count % 500) == 0) {
                    new Utils().writeObject(goldenMap, RepurposingEval.goldenPairRankSplit_noSql + count + ".dat");
                    goldenMap = new LinkedHashMap<>();
                }
            }
        }
        new Utils().writeObject(goldenMap, RepurposingEval.goldenPairRankSplit_noSql + count + ".dat");
    }

    public boolean hasIntermediates(Set<String> possibleBsByDrug, String diseaseCui,
                                    DBCollection collPredicationPre_s, DBCollection collPredicationPre_s_ref) {
        if (!possibleBsByDrug.isEmpty()) {
            Set<String> possibleBsByDisease = possibleBs(Predication.cui, diseaseCui, collPredicationPre_s_ref, collPredicationPre_s);
            return Sets.intersection(possibleBsByDrug, possibleBsByDisease).size() > 0;
        } else {
            return false;
        }
    }

    public Set<String> possibleBs(String searchColumn,
                                  String cui,
                                  DBCollection collPredicationPre_s_ref,
                                  DBCollection collPredicationPre_s) {
        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put(searchColumn, cui);
        DBCursor cursor = collPredicationPre_s_ref.find(searchQuery);
        Set<String> possibleBs = new HashSet<>();
        while (cursor.hasNext()) {
            DBCursor cursorInfo = collPredicationPre_s.find(
                    new BasicDBObject(
                            Predication.documentId,
                            new BasicDBObject("$in", cursor.next().get(Predication.ids))));
            while (cursorInfo.hasNext()) {
                DBObject next = cursorInfo.next();
                BasicDBList oCuis = (BasicDBList) next.get(Predication.oCuis);
                possibleBs.addAll(oCuis.stream().map(Object::toString).collect(Collectors.toSet()));
            }
        }
        return possibleBs;
    }

    public void createTreatRankSet() {
        int totalDrug = 0;
        for (int i = 1; i <= 927; i++) {
            int fileNum = 500 * i;
            if (new File(RepurposingEval.goldenPairRankSplit_noSql + fileNum + ".dat").exists()) {
                Map<String, Map<String, Integer>> goldenMap =
                        (Map<String, Map<String, Integer>>) new Utils().readObjectFile(RepurposingEval.goldenPairRankSplit_noSql + fileNum + ".dat");
                Map<String, Map<String, Integer>> treatMap = new LinkedHashMap<>();
                for (String drug : goldenMap.keySet()) {
                    Map<String, Integer> diseaseMap = goldenMap.get(drug);
                    //has TREATS predicate => keep it
                    if (diseaseMap.values().contains(3)) {
                        int zero = Collections.frequency(diseaseMap.values(), 0);
                        int valid = diseaseMap.size() - zero;
                        // #score > 0 / #total >=0.01 => keep it
                        if ((double) valid / diseaseMap.size() >= 0.01) {
                            treatMap.put(drug, diseaseMap);
                        }
                    }
                }
                totalDrug += treatMap.size();
                System.out.println(treatMap.size() + "\t" + treatMap.keySet());
                new Utils().writeObject(treatMap, RepurposingEval.goldenTreatRankSplit_noSql + fileNum + ".dat");
            }
        }

        if (new File(RepurposingEval.goldenPairRankSplit_noSql + "463662.dat").exists()) {
            Map<String, Map<String, Integer>> goldenMap =
                    (Map<String, Map<String, Integer>>) new Utils().readObjectFile(RepurposingEval.goldenPairRankSplit_noSql + "463662.dat");
            Map<String, Map<String, Integer>> treatMap = new LinkedHashMap<>();
            for (String drug : goldenMap.keySet()) {
                Map<String, Integer> diseaseMap = goldenMap.get(drug);
                //has TREATS predicate => keep it
                if (diseaseMap.values().contains(3)) {
                    int zero = Collections.frequency(diseaseMap.values(), 0);
                    int valid = diseaseMap.size() - zero;
                    // #score > 0 / #total >=0.01 => keep it
                    if ((double) valid / diseaseMap.size() >= 0.01) {
                        treatMap.put(drug, diseaseMap);
                    }
                }
            }
            totalDrug += treatMap.size();
            System.out.println(treatMap.size() + "\t" + treatMap.keySet());
            new Utils().writeObject(treatMap, RepurposingEval.goldenTreatRankSplit_noSql + "463662.dat");
        }

        System.out.println("totalDrug: " + totalDrug);
    }

    public void create500EvalRank(int seedC, int diseaseC) {
        int totalDrug = 0;
        for (int i = 1; i <= 927; i++) {
            int fileNum = 500 * i;
            if (new File(RepurposingEval.goldenTreatRankSplit_noSql + fileNum + ".dat").exists()) {
                LinkedHashMap<String, HashMap<String, IndexScore>> map = new LinkedHashMap<>();
                Map<String, Map<String, Integer>> goldenMap =
                        (Map<String, Map<String, Integer>>) new Utils().readObjectFile(RepurposingEval.goldenTreatRankSplit_noSql + fileNum + ".dat");
                //Water??
                goldenMap.remove("Water");
                List<String> drugs = new ArrayList<>(goldenMap.keySet());
                List<String> selectDrug = new ArrayList<>();
                // shuffle it!!
                Collections.shuffle(drugs, new Random(50));
                for (String drug : drugs) {
                    HashMap<String, IndexScore> rankMap = new HashMap<>();
                    List<IndexScore> perfectLs = new ArrayList<>();
                    Map<String, Integer> diseaseMap = goldenMap.get(drug);
                    Set<String> hasRankDisease = new HashSet<>();
                    List<Map.Entry<String, Integer>> diseaseList = diseaseMap.entrySet().stream()
                            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                            .collect(Collectors.toList());
                    for (Map.Entry<String, Integer> diseaseScore : diseaseList) {
                        String disease = diseaseScore.getKey();
                        int score = diseaseScore.getValue();
                        if (score > 0) hasRankDisease.add(disease);
                        IndexScore item = new IndexScore(score, disease);
                        perfectLs.add(item);
                        if (perfectLs.size() == diseaseC) break;
                    }
                    if (perfectLs.size() < diseaseC) {
                        Set<String> uselessDiseaseSet = Sets.difference(diseaseMap.keySet(), hasRankDisease);
                        for (String uselessDisease : uselessDiseaseSet) {
                            int score = diseaseMap.get(uselessDisease);
                            IndexScore item = new IndexScore(score, uselessDisease);
                            perfectLs.add(item);
                            if (perfectLs.size() == diseaseC) break;
                        }
                    }
                    perfectLs.sort(Collections.reverseOrder(new IndexScore.ScoreComparator()));
                    for (IndexScore indexItem : perfectLs) {
                        rankMap.put(indexItem.getName(), indexItem);
                    }
                    map.put(drug, rankMap);
                    selectDrug.add(drug);
//                    if (map.size() == seedC) break;
                }
                totalDrug += map.keySet().size();
                Utils utils = new Utils();
                utils.writeObject(map, RepurposingEval.goldenRankSplit_noSql + fileNum + ".dat");
                writeFocalDrugsFile(selectDrug);
            }
        }

        if (new File(RepurposingEval.goldenTreatRankSplit_noSql + "463662.dat").exists()) {
            LinkedHashMap<String, HashMap<String, IndexScore>> map = new LinkedHashMap<>();
            Map<String, Map<String, Integer>> goldenMap =
                    (Map<String, Map<String, Integer>>) new Utils().readObjectFile(RepurposingEval.goldenTreatRankSplit_noSql + "463662.dat");
            //Water??
            goldenMap.remove("Water");
            List<String> drugs = new ArrayList<>(goldenMap.keySet());
            List<String> selectDrug = new ArrayList<>();
            // shuffle it!!
            Collections.shuffle(drugs, new Random(50));
            for (String drug : drugs) {
                HashMap<String, IndexScore> rankMap = new HashMap<>();
                List<IndexScore> perfectLs = new ArrayList<>();
                Map<String, Integer> diseaseMap = goldenMap.get(drug);
                Set<String> hasRankDisease = new HashSet<>();
                List<Map.Entry<String, Integer>> diseaseList = diseaseMap.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .collect(Collectors.toList());
                for (Map.Entry<String, Integer> diseaseScore : diseaseList) {
                    String disease = diseaseScore.getKey();
                    int score = diseaseScore.getValue();
                    if (score > 0) hasRankDisease.add(disease);
                    IndexScore item = new IndexScore(score, disease);
                    perfectLs.add(item);
                    if (perfectLs.size() == diseaseC) break;
                }
                if (perfectLs.size() < diseaseC) {
                    Set<String> uselessDiseaseSet = Sets.difference(diseaseMap.keySet(), hasRankDisease);
                    for (String uselessDisease : uselessDiseaseSet) {
                        int score = diseaseMap.get(uselessDisease);
                        IndexScore item = new IndexScore(score, uselessDisease);
                        perfectLs.add(item);
                        if (perfectLs.size() == diseaseC) break;
                    }
                }
                perfectLs.sort(Collections.reverseOrder(new IndexScore.ScoreComparator()));
                for (IndexScore indexItem : perfectLs) {
                    rankMap.put(indexItem.getName(), indexItem);
                }
                map.put(drug, rankMap);
                selectDrug.add(drug);
//                if (map.size() == seedC) break;
            }
            totalDrug += map.keySet().size();
            Utils utils = new Utils();
            utils.writeObject(map, RepurposingEval.goldenRankSplit_noSql + "463662.dat");
            writeFocalDrugsFile(selectDrug);
        }
        System.out.println("totalDrug: " + totalDrug);

        List<String> drugs = new Utils().readLineFile(RepurposingEval.focalDrugs);
        Collections.shuffle(drugs, new Random(50));
        writeFocal500DrugsFile(drugs.subList(0, 500));
    }

    public void writeFocalDrugsFile(List<String> selectDrug) {
        try {
            FileWriter fr = new FileWriter(new File(RepurposingEval.focalDrugs), true);
            BufferedWriter br = new BufferedWriter(fr);
            selectDrug.forEach(e -> {
                try {
                    br.write(e);
                    br.newLine();
                    br.flush();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            });
            br.close();
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeFocal500DrugsFile(List<String> selectDrug) {
        try {
            FileWriter fr = new FileWriter(new File(RepurposingEval.focal500Drugs));
            BufferedWriter br = new BufferedWriter(fr);
            selectDrug.forEach(e -> {
                try {
                    br.write(e);
                    br.newLine();
                    br.flush();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            });
            br.close();
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
