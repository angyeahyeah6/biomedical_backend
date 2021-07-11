package weng.prepare;

import weng.util.Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


public class UmlsConcept {
    public static final String CHEMI_DRUG_VOCABULARY = "vocabulary/umls_controlledVoc/Chemicals&Drugs.txt";
    public static final String DISORDER_VOCABULARY = "vocabulary/umls_controlledVoc/Disorders.txt";
//    public static final String DISORDER_VOCABULARY = "vocabulary/umls_controlledVoc/Disorders_test.txt";
    public static final String GENE_VOCABULARY = "vocabulary/umls_controlledVoc/Genes&MolecularSequences.txt";
    public static final String PHYSIOLOGY_VOCABULARY = "vocabulary/umls_controlledVoc/Physiology.txt";
    public static final String ANATOMY_VOCABULARY = "vocabulary/umls_controlledVoc/Anatomy.txt";

    public static final ArrayList<String> chemiDrugSeeds = new Utils().readLineFile(CHEMI_DRUG_VOCABULARY);
    public static final ArrayList<String> disorderSeeds = new Utils().readLineFile(DISORDER_VOCABULARY);
    public static final ArrayList<String> geneProteinSeeds = new Utils().readLineFile(GENE_VOCABULARY);
    public static final ArrayList<String> physiologySeeds = new Utils().readLineFile(PHYSIOLOGY_VOCABULARY);
    public static final ArrayList<String> anatomySeeds = new Utils().readLineFile(ANATOMY_VOCABULARY);

    public ArrayList<String> getIntermSeeds() {
        HashSet<String> intermsUmls = new HashSet<>();
        intermsUmls.addAll(disorderSeeds);
        intermsUmls.addAll(geneProteinSeeds);
        intermsUmls.addAll(physiologySeeds);
        intermsUmls.addAll(anatomySeeds);
        return (new ArrayList<>(intermsUmls));
    }

    public static ArrayList<String> getSeedsNonRepeated() {
        ArrayList<String> allSeeds = new ArrayList<>();
        allSeeds.addAll(chemiDrugSeeds);
        allSeeds.addAll(disorderSeeds);
        allSeeds.addAll(geneProteinSeeds);
        allSeeds.addAll(anatomySeeds);
        allSeeds.addAll(physiologySeeds);
        return allSeeds;
    }

    public static List<HashMap<String, String>> getUmlsNameCuiAndCuiNameMap(Connection connSem) throws SQLException {
        // create uml name and CUI (object id) pairs map
        HashMap<String, String> umlsNameCuiMap = new HashMap<>();
        HashMap<String, String> umlsCuiNameMap = new HashMap<>();
        String sqlSelConcept = "SELECT `CUI`, `PREFERRED_NAME` FROM `concept` WHERE `TYPE`=\"META\"";
        PreparedStatement psSelConcept = connSem.prepareStatement(sqlSelConcept);
        ResultSet rsSelConcept = psSelConcept.executeQuery();
        while (rsSelConcept.next()) {
            String conceptName = rsSelConcept.getString("PREFERRED_NAME");
            String cui = rsSelConcept.getString("CUI");
            umlsNameCuiMap.put(conceptName, cui);
            umlsCuiNameMap.put(cui, conceptName);
        }
        rsSelConcept.close();
        psSelConcept.close();

        List<HashMap<String, String>> umlsMaps = new ArrayList<>();
        umlsMaps.add(umlsNameCuiMap);
        umlsMaps.add(umlsCuiNameMap);
        return umlsMaps;
    }

    public static HashMap<String, String> getUmlsNameCuiMap(Connection connSem) throws SQLException {
        HashMap<String, String> umlsNameCuiMap = new HashMap<>();
        String sqlSelConcept = "SELECT `CUI`, `PREFERRED_NAME` FROM `concept` WHERE `TYPE`=\"META\"";
        PreparedStatement psSelConcept = connSem.prepareStatement(sqlSelConcept);
        ResultSet rsSelConcept = psSelConcept.executeQuery();
        while (rsSelConcept.next()) {
            String conceptName = rsSelConcept.getString("PREFERRED_NAME");
            String cui = rsSelConcept.getString("CUI");
            umlsNameCuiMap.put(conceptName, cui);
        }
        rsSelConcept.close();
        psSelConcept.close();
        return umlsNameCuiMap;
    }

    public static HashMap<String, String> getUmlsCuiNameMap(Connection connSem) throws SQLException {
        HashMap<String, String> umlsCuiNameMap = new HashMap<>();
        String sqlSelConcept = "SELECT `CUI`, `PREFERRED_NAME` FROM `concept` WHERE `TYPE`=\"META\"";
        PreparedStatement psSelConcept = connSem.prepareStatement(sqlSelConcept);
        ResultSet rsSelConcept = psSelConcept.executeQuery();
        while (rsSelConcept.next()) {
            String cui = rsSelConcept.getString("CUI");
            String conceptName = rsSelConcept.getString("PREFERRED_NAME");
            umlsCuiNameMap.put(cui, conceptName);
        }
        rsSelConcept.close();
        psSelConcept.close();
        return umlsCuiNameMap;
    }

    public Set<String> getChemiDrugs(Connection connSem) throws SQLException {
        String sql = "SELECT `PREFERRED_NAME` FROM `concept` AS `c` INNER JOIN `concept_semtype` AS `cs` " +
                "ON `c`.`CONCEPT_ID`=`cs`.`CONCEPT_ID` WHERE `SEMTYPE`=\"aapp\" or `SEMTYPE`=\"antb\" " +
                "or `SEMTYPE`=\"bacs\" or `SEMTYPE`=\"bodm\" or `SEMTYPE`=\"carb\" or `SEMTYPE`=\"chem\" " +
                "or `SEMTYPE`=\"chvf\" or `SEMTYPE`=\"chvs\" or `SEMTYPE`=\"clnd\" or `SEMTYPE`=\"eico\" " +
                "or `SEMTYPE`=\"elii\" or `SEMTYPE`=\"enzy\" or `SEMTYPE`=\"hops\" or `SEMTYPE`=\"horm\" " +
                "or `SEMTYPE`=\"imft\" or `SEMTYPE`=\"irda\" or `SEMTYPE`=\"inch\" or `SEMTYPE`=\"lipd\" " +
                "or `SEMTYPE`=\"nsba\" or `SEMTYPE`=\"nnon\" or `SEMTYPE`=\"orch\" or `SEMTYPE`=\"opco\" " +
                "or `SEMTYPE`=\"phsu\" or `SEMTYPE`=\"rcpt\" or `SEMTYPE`=\"strd\" or `SEMTYPE`=\"vita\" " +
                "GROUP BY `CUI`, `PREFERRED_NAME`";
        PreparedStatement ps = connSem.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        Set<String> chemiDrugs = new HashSet<>();
        while (rs.next()) {
            String chemiDrug = rs.getString("PREFERRED_NAME");
            chemiDrugs.add(chemiDrug);
        }
        rs.close();
        ps.close();
        return chemiDrugs;
    }

    public Set<String> getDisorders(Connection connSem) throws SQLException {
        String sql = "SELECT `PREFERRED_NAME` FROM `concept` AS `c` INNER JOIN `concept_semtype` AS `cs` " +
                "ON `c`.`CONCEPT_ID`=`cs`.`CONCEPT_ID` WHERE `SEMTYPE`=\"acab\" or `SEMTYPE`=\"anab\" or " +
                "`SEMTYPE`=\"comd\" or `SEMTYPE`=\"cgab\" or `SEMTYPE`=\"dsyn\" or `SEMTYPE`=\"emod\" or " +
                "`SEMTYPE`=\"fndg\" or `SEMTYPE`=\"inpo\" or `SEMTYPE`=\"mobd\" or `SEMTYPE`=\"neop\" or " +
                "`SEMTYPE`=\"patf\" or `SEMTYPE`=\"sosy\" GROUP BY `CUI`, `PREFERRED_NAME`";
        PreparedStatement ps = connSem.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        Set<String> disorders = new HashSet<>();
        while (rs.next()) {
            String disorder = rs.getString("PREFERRED_NAME");
            disorders.add(disorder);
        }
        rs.close();
        ps.close();
        return disorders;
    }

    public Set<String> getGeneProteins(Connection connSem) throws SQLException {
        String sql = "SELECT `PREFERRED_NAME` FROM `concept` AS `c` INNER JOIN `concept_semtype` AS `cs` " +
                "ON `c`.`CONCEPT_ID`=`cs`.`CONCEPT_ID` WHERE `SEMTYPE`=\"amas\" or `SEMTYPE`=\"crbs\" or " +
                "`SEMTYPE`=\"gngm\" or `SEMTYPE`=\"mosq\" or `SEMTYPE`=\"nusq\" GROUP BY `CUI`, `PREFERRED_NAME`";
        PreparedStatement ps = connSem.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        Set<String> geneProteins = new HashSet<>();
        while (rs.next()) {
            String geneProtein = rs.getString("PREFERRED_NAME");
            geneProteins.add(geneProtein);
        }
        rs.close();
        ps.close();
        return geneProteins;
    }

    public Set<String> getPhysiologys(Connection connSem) throws SQLException {
        String sql = "SELECT `PREFERRED_NAME` FROM `concept` AS `c` INNER JOIN `concept_semtype` AS `cs` " +
                "ON `c`.`CONCEPT_ID`=`cs`.`CONCEPT_ID` WHERE `SEMTYPE`=\"celf\" or `SEMTYPE`=\"clna\" " +
                "or `SEMTYPE`=\"genf\" or `SEMTYPE`=\"menp\" or `SEMTYPE`=\"moft\" or `SEMTYPE`=\"orga\" " +
                "or `SEMTYPE`=\"orgf\" or `SEMTYPE`=\"ortf\" or `SEMTYPE`=\"phsf\" GROUP BY `CUI`, `PREFERRED_NAME`";
        PreparedStatement ps = connSem.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        Set<String> physiologys = new HashSet<>();
        while (rs.next()) {
            String physiology = rs.getString("PREFERRED_NAME");
            physiologys.add(physiology);
        }
        rs.close();
        ps.close();
        return physiologys;
    }

    public Set<String> getAnatomys(Connection connSem) throws SQLException {
        String sql = "SELECT `PREFERRED_NAME` FROM `concept` AS `c` INNER JOIN `concept_semtype` AS `cs` " +
                "ON `c`.`CONCEPT_ID`=`cs`.`CONCEPT_ID` WHERE `SEMTYPE`=\"anst\" or `SEMTYPE`=\"blor\" or " +
                "`SEMTYPE`=\"bpoc\" or `SEMTYPE`=\"bsoj\" or `SEMTYPE`=\"bdsu\" or `SEMTYPE`=\"bdsy\" or " +
                "`SEMTYPE`=\"cell\" or `SEMTYPE`=\"celc\" or `SEMTYPE`=\"emst\" or `SEMTYPE`=\"ffas\" or " +
                "`SEMTYPE`=\"tisu\" GROUP BY `CUI`, `PREFERRED_NAME`";
        PreparedStatement ps = connSem.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        Set<String> physiologys = new HashSet<>();
        while (rs.next()) {
            String physiology = rs.getString("PREFERRED_NAME");
            physiologys.add(physiology);
        }
        rs.close();
        ps.close();
        return physiologys;
    }

    public void createVocabularyFile(Set<String> vocabularies, String filePath) throws IOException {
        File file = new File(filePath);
        FileWriter fr = new FileWriter(file);
        BufferedWriter br = new BufferedWriter(fr);
        for (String vocabulary : vocabularies) {
            br.write(vocabulary);
            br.newLine();
        }
        br.flush();
        br.close();
        fr.close();
    }
}
