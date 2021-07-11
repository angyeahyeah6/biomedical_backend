package weng.parser;

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import weng.prepare.MeshConcept;
import weng.prepare.UmlsConcept;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Logger;

public class CtdParser {

    public CtdParser() {
    }

    public void ctdDiseaseMap(Connection connBioCon) throws IOException, SQLException {
        SetMultimap<String, String> meshMimMap = LinkedHashMultimap.create();
        CSVReader ctdMedicIn = new CSVReader(new FileReader("ctd/CTD_diseases.csv"));
        String[] line;
        while ((line = ctdMedicIn.readNext()) != null) {
            if (!line[0].startsWith("#")) {
                String diseaseMeshId = "";
                String diseaseIdRaw = line[1];
                if (diseaseIdRaw.split(":")[0].equals("MESH")) {
                    diseaseMeshId = diseaseIdRaw.split(":")[1];
                }

                String altIdsRaw = line[2];
                if (!diseaseMeshId.isEmpty() && diseaseMeshId.length() > 0) {
                    for (String altIdRaw : altIdsRaw.split("\\|")) {
                        if (altIdRaw.split(":")[0].equals("OMIM")) {
                            String altMIMid = altIdRaw.split(":")[1];
                            meshMimMap.put(diseaseMeshId, altMIMid);
                        }
                    }
                }
            }
        }
        ctdMedicIn.close();

        String insertRelation = "INSERT INTO `omim_meshid` (`MIM`, `MeshId`) VALUES (?, ?)";
        PreparedStatement psInsRel = connBioCon.prepareStatement(insertRelation);
        for (Entry<String, String> e : meshMimMap.entries()) {
            String meshId = e.getKey();
            String mim = e.getValue();

            //Database
            psInsRel.clearParameters();
            psInsRel.setString(1, mim);
            psInsRel.setString(2, meshId);
            psInsRel.addBatch();

        }
        psInsRel.executeBatch();
        psInsRel.close();
    }

    public void mapDBOmimMeshToOmimUmls(Connection connBioCon) throws SQLException {
        HashMap<String, String> omimMesh = getOmimMesh(connBioCon);
        HashMap<String, String> meshNameMim = getMeshNameMim(omimMesh, connBioCon);
        HashMap<String, String> meshNameCui = getMeshNameCui(meshNameMim, connBioCon);
        insertInToDBOmimUmlsCui(meshNameCui, meshNameMim, connBioCon);
    }

    public HashMap<String, String> getOmimMesh(Connection connBioCon) throws SQLException {
        System.out.println("SELECT FROM `omim_meshid`");
        String selOmimMesh = "SELECT `MIM`, `MeshId` FROM `omim_meshid`;";
        PreparedStatement psSelOmimMesh = connBioCon.prepareStatement(selOmimMesh);
        ResultSet rsSelOmimMesh = psSelOmimMesh.executeQuery();
        HashMap<String, String> omimMesh = new HashMap<>();
        while (rsSelOmimMesh.next()) {
            String mim = rsSelOmimMesh.getString("MIM");
            String meshId = rsSelOmimMesh.getString("MeshId");
            omimMesh.put(mim, meshId);
        }
        rsSelOmimMesh.close();
        psSelOmimMesh.close();
        return omimMesh;
    }

    public HashMap<String, String> getMeshNameMim(HashMap<String, String> omimMesh, Connection connBioCon) throws SQLException {
        System.out.println("SELECT FROM `mesh_descriptor` and `mesh_supplement`");
        String selDesc = "SELECT DISTINCT `DescriptorName` FROM `mesh_descriptor` WHERE `DescriptorUI`=?;";
        String selSupp = "SELECT DISTINCT `HeadingMappedTo` FROM `mesh_supplement` WHERE `SupplementalRecordUI`=?;";
        PreparedStatement psSelDesc = connBioCon.prepareStatement(selDesc);
        PreparedStatement psSelSupp = connBioCon.prepareStatement(selSupp);
        HashMap<String, String> meshNameMim = new HashMap<>();
        for (String mim : omimMesh.keySet()) {
            String meshId = omimMesh.get(mim);
            String meshName;
            if (meshId.startsWith("C")) {
                String columnName = "HeadingMappedTo";
                meshName = setMeshName(meshId, psSelSupp, columnName);
            } else {
                String columnName = "DescriptorName";
                meshName = setMeshName(meshId, psSelDesc, columnName);
            }
            if (!meshName.isEmpty()) {
                meshNameMim.put(meshName, mim);
            }
        }
        psSelSupp.close();
        psSelDesc.close();
        return meshNameMim;
    }

    public String setMeshName(String meshId, PreparedStatement ps, String columnName) throws SQLException {
        String meshName = "";
        ps.clearParameters();
        ps.setString(1, meshId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            meshName = rs.getString(columnName);
        }
        rs.close();
        return meshName;
    }

    public HashMap<String, String> getMeshNameCui(HashMap<String, String> meshNameMim, Connection connBioCon) throws SQLException {
        System.out.println("SELECT FROM `umls_mesh`");
        String selUmlsMesh = "SELECT `CUI` FROM `umls_mesh` WHERE `Mesh`=?;";
        PreparedStatement psSelUmlsMesh = connBioCon.prepareStatement(selUmlsMesh);
        HashMap<String, String> meshNameCui = new HashMap<>();
        for (String meshName : meshNameMim.keySet()) {
            String cui = "";
            psSelUmlsMesh.clearParameters();
            psSelUmlsMesh.setString(1, meshName);
            ResultSet rsSelUmlsMesh = psSelUmlsMesh.executeQuery();
            while (rsSelUmlsMesh.next()) {
                String cuiTemp = rsSelUmlsMesh.getString("CUI");
                if (cuiTemp.startsWith("C")) {
                    cui = cuiTemp;
                }
            }
            rsSelUmlsMesh.close();
            meshNameCui.put(meshName, cui);
        }
        psSelUmlsMesh.close();
        return meshNameCui;
    }

    public void insertInToDBOmimUmlsCui(HashMap<String, String> meshNameCui,
                                        HashMap<String, String> meshNameMim,
                                        Connection connBioCon) throws SQLException {
        System.out.println("INSERT INTO `omim_umlscui`");
        String insOmimUmlsCui = "INSERT INTO `omim_umlscui` (`MIM`, `cui`) VALUES (?, ?);";
        PreparedStatement psInsOmimUmlsCui = connBioCon.prepareStatement(insOmimUmlsCui);
        for (String meshName : meshNameMim.keySet()) {
            String mim = meshNameMim.get(meshName);
            String cui = meshNameCui.get(meshName);
            if (!cui.isEmpty()) {
                psInsOmimUmlsCui.clearParameters();
                psInsOmimUmlsCui.setString(1, mim);
                psInsOmimUmlsCui.setString(2, cui);
                psInsOmimUmlsCui.addBatch();
                psInsOmimUmlsCui.executeBatch();
                System.out.println("mim: " + mim + "\tcui: " + cui);
            }
        }
        psInsOmimUmlsCui.close();
    }

    public void queryGeneSeedId(Connection connBioCon) throws IOException, SQLException {
        String selUniprotByUmls = "SELECT `unigene`.`GeneID` FROM `omim_umls` AS `umls`, `uniprot_mim` AS `unimim`, `uniprot_entrez_gene` AS `unigene` WHERE `umls`.`umls`=? AND `unimim`.`MIM` = `umls`.`mim` AND `unigene`.`UniProtKB-AC` = `unimim`.`UniProtKB-AC`;";
        PreparedStatement psSelUniprotByUmls = connBioCon.prepareStatement(selUniprotByUmls);
        System.out.println("Start reading genes_proteins_enzymes.txt...");
        ArrayList<String> geneProteinList = new ArrayList<>();
        BufferedReader GeneMoleTxt = new BufferedReader(new InputStreamReader(new FileInputStream(new File("vocabulary/umls_controlledVoc/Genes&MolecularSequences.txt")), "UTF-8"));
        while (GeneMoleTxt.ready()) {
            geneProteinList.add(GeneMoleTxt.readLine());
        }

		/* NCBI Gene name -- help translate */
        System.out.println("Start reading gene info...");
        HashMap<String, String> ncbiSymbol = new HashMap<>(); //symbol, ncbi id
        SetMultimap<String, String> ncbiSyn = LinkedHashMultimap.create(); //synonym, ncbi id
        ArrayList<String> ncbiExRefList_Human = new ArrayList<>();
        try (BufferedReader ncbiIn = new BufferedReader(new FileReader("ncbigene/gene_info"))) {
            String lineRaw;
            ncbiIn.readLine();
            while (ncbiIn.ready()) {
                lineRaw = ncbiIn.readLine();
                String[] entry = lineRaw.split("\t");
                String geneId = entry[1];
                String geneSymbol = entry[2];
                String geneSyns = entry[4];
                String xRef = entry[5];
                if (!geneSymbol.equals("NEWENTRY")) {
                    ncbiSymbol.put(geneSymbol, geneId);
                }
                if (!geneSyns.equals("-")) {
                    for (String geneSyn : geneSyns.split("\\|")) {
                        ncbiSyn.put(geneSyn, geneId);
                    }
                }
                if (!xRef.equals("-")) {
                    for (String exDbIdRaw : xRef.split("\\|")) {
                        if (exDbIdRaw.contains("MIM:")) {
                            ncbiExRefList_Human.add(geneId);
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("ncbiSyn size: " + ncbiSyn.size());
        System.out.println("ncbiExRefList_Human size: " + ncbiExRefList_Human.size());
        System.out.println("geneProteinList size: " + geneProteinList.size());
        Logger logger = Logger.getAnonymousLogger();
        int count_gene = 0;
        int geneSize = geneProteinList.size();
        ArrayList<String> geneMoleList_unMatch = new ArrayList<>();
        LinkedHashMap<String, String> geneNameIdMap = new LinkedHashMap<>(); //gene name, ncbi id
        for (String geneName : geneProteinList) {
            if (++count_gene % 100 == 0) {
                logger.info(count_gene + " / " + geneSize);
            }

            String geneId = "";
            if (ncbiSymbol.containsKey(geneName)) {
                geneId = ncbiSymbol.get(geneName);
            }

            if (geneId.isEmpty()) {
                if (ncbiSyn.containsKey(geneName)) {
                    Set<String> geneIds = ncbiSyn.get(geneName);
                    if (geneIds.size() == 1) {
                        for (String matchId : geneIds) {
                            geneId = matchId;
                        }
                    } else if (geneIds.size() > 1) {
                        for (String matchId : geneIds) {
                            if (ncbiExRefList_Human.contains(matchId)) {
                                geneId = matchId;
                                break;
                            }
                        }
                    }
                }
            }

            if (geneId.isEmpty()) {
                psSelUniprotByUmls.clearParameters();
                psSelUniprotByUmls.setString(1, geneName);
                ResultSet rsQueryUniUmls = psSelUniprotByUmls.executeQuery();
                if (rsQueryUniUmls.next()) {
                    geneId = rsQueryUniUmls.getString("GeneID");
                }
                rsQueryUniUmls.close();
            }

            if (!geneId.isEmpty()) {
                geneNameIdMap.put(geneName, geneId);
                System.out.println("Gene Matched: " + geneName + " <=> " + geneId);
            } else {
                geneMoleList_unMatch.add(geneName);
                System.err.println("Gene: '" + geneName + "' has no matched NCBI ID.");
            }
        }
        psSelUniprotByUmls.close();
        System.out.println("geneProteinList size: " + geneProteinList.size());
        System.out.println("ncbiSymbol size: " + ncbiSymbol.size());
        System.out.println("ncbiSyn size: " + ncbiSyn.size());
        System.out.println("ncbiExRefList_Human size: " + ncbiExRefList_Human.size());


        // Output Gene matching Id.
        System.out.println(geneNameIdMap.size() + " Gene are found matched NCBI ID.");
        File matchIdFile = new File("ncbigene/matchNcbiId_gene.txt");
        BufferedWriter matchOutput;
        try {
            matchOutput = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(matchIdFile), "UTF-8"));
            for (Entry<String, String> geneNameId : geneNameIdMap.entrySet()) {
                matchOutput.write(geneNameId.getKey() + "\t" + geneNameId.getValue());
                matchOutput.newLine();
            }
            matchOutput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Output Gene not matching Id.
        System.out.println(geneMoleList_unMatch.size() + " Gene are not found matched NCBI ID.");
        File notMatchIdFile = new File("ncbigene/notMatchNcbiId_gene.txt");
        BufferedWriter notMatchOutput;
        try {
            notMatchOutput = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(notMatchIdFile), "UTF-8"));
            for (String geneName : geneMoleList_unMatch) {
                notMatchOutput.write(geneName);
                notMatchOutput.newLine();
            }
            notMatchOutput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void parseCtdFileToDB(Connection connBioCon, Connection connBioRel) throws SQLException, IOException {
        // Get meshGeneIdsMap
        String drugGeneFilePath = "ctd/CTD_chem_gene_ixns.csv"; /* drug-gene */
        SetMultimap<String, String> meshGeneIdsMap = getMeshGeneByDrugGene(drugGeneFilePath); // mesh UI, ncbi id
        System.out.println("meshGeneIdsMap size-1: " + meshGeneIdsMap.size());
        String geneDiseaseFilePath = "ctd/CTD_genes_diseases.csv"; /* gene-disease */
        meshGeneIdsMap = getMeshGeneByGeneDisease(meshGeneIdsMap, geneDiseaseFilePath);
        System.out.println("meshGeneIdsMap size-2: " + meshGeneIdsMap.size());

        // Get meshUiNameMapOfChem and ncbiMeshMap
        System.out.println("Get meshUiNameMapOfChem");
        LinkedHashMap<String, String> meshUiNameMapOfChem = getMeshOfChem(meshGeneIdsMap, connBioCon); // Chem's mesh name: (mesh UI, mesh name)
        System.out.println("Get ncbiMeshMap");
        String matchNcbiIdGeneFilePath = "ncbigene/matchNcbiId_gene.txt";
        HashMap<String, String> geneIdNameMap = getMeshOfGene(matchNcbiIdGeneFilePath); // Gene's matched Mesh name: (geneId, geneMeshName)
        ArrayList<Object> ncbiMesh = getNcbiMesh(meshGeneIdsMap, geneIdNameMap);
        ArrayList<String> geneNotMatch = (ArrayList<String>) ncbiMesh.get(0);
        saveGeneNotMachedFile(geneNotMatch);
        LinkedHashMap<String, String> ncbiMeshMap = (LinkedHashMap<String, String>) ncbiMesh.get(1); // ncbi id, mesh name

        // Map meshGeneIdsMap to UMLS
        HashMap<String, Set<String>> meshNameGeneIdsMap = getMeshNameGeneMap(meshGeneIdsMap, connBioCon); // mesh name, ncbi id
        HashMap<String, String> meshNameUmlsCuiMapByGene = getMeshNameUmlsCuiMap(meshNameGeneIdsMap, connBioCon); // mesh name, UMLS cui
        SetMultimap<String, String> umlsCuiGeneIdsMap = getUmlsCuiGeneMap(meshNameGeneIdsMap, meshNameUmlsCuiMapByGene); // UMLS cui, ncbi id

        // Map meshUiNameMapOfChem to UMLS
        LinkedHashMap<String, String> umlsCuiNameMapOfChem = getUmlsCuiNameMapOfChem(meshUiNameMapOfChem, connBioCon);

        // Map ncbiMeshMap to UMLS
        LinkedHashMap<String, String> geneIdUmlsNameMap = getGeneIdUmlsNameMap(ncbiMeshMap, connBioCon);

        // Insert into DB
        System.out.println("INSERT INTO DB `ctd_relation_umls`");
        insertInToDbCtdRel(connBioRel, umlsCuiGeneIdsMap, umlsCuiNameMapOfChem, geneIdUmlsNameMap);

        System.out.println("Mesh count: " + meshGeneIdsMap.keySet().size());
        System.out.println("Gene count: " + meshGeneIdsMap.values().size());
        System.out.println("MeshUI-Mesh count: " + meshUiNameMapOfChem.size());
        System.out.println("Gene-Mesh count: " + ncbiMeshMap.size());
    }

    public LinkedHashMap<String, String> getGeneIdUmlsNameMap(LinkedHashMap<String, String> ncbiMeshMap,
                                                              Connection connBioCon) throws SQLException {
        LinkedHashMap<String, String> geneIdUmlsNameMap = new LinkedHashMap<>();
        String selUmlsMesh = "SELECT `CUI`, `Preferred_Name` FROM `umls_mesh` WHERE `Mesh`=?;";
        PreparedStatement psSelUmlsMesh = connBioCon.prepareStatement(selUmlsMesh);
        for (String geneId : ncbiMeshMap.keySet()) {
            String meshName = ncbiMeshMap.get(geneId);
            psSelUmlsMesh.clearParameters();
            psSelUmlsMesh.setString(1, meshName);
            ResultSet rsSelUmlsMesh = psSelUmlsMesh.executeQuery();
            while (rsSelUmlsMesh.next()) {
                String cui = rsSelUmlsMesh.getString("CUI");
                if (cui.startsWith("C")) {
                    String umlsName = rsSelUmlsMesh.getString("Preferred_Name");
                    geneIdUmlsNameMap.put(geneId, umlsName);
                }
            }
            rsSelUmlsMesh.close();
        }
        psSelUmlsMesh.close();
        return geneIdUmlsNameMap;
    }

    public LinkedHashMap<String, String> getUmlsCuiNameMapOfChem(LinkedHashMap<String, String> meshUiNameMapOfChem,
                                                                 Connection connBioCon) throws SQLException {
        LinkedHashMap<String, String> umlsCuiNameMapOfChem = new LinkedHashMap<>();
        String selUmlsMesh = "SELECT `CUI`, `Preferred_Name` FROM `umls_mesh` WHERE `Mesh`=?;";
        PreparedStatement psSelUmlsMesh = connBioCon.prepareStatement(selUmlsMesh);
        for (String meshName : meshUiNameMapOfChem.values()) {
            psSelUmlsMesh.clearParameters();
            psSelUmlsMesh.setString(1, meshName);
            ResultSet rsSelUmlsMesh = psSelUmlsMesh.executeQuery();
            while (rsSelUmlsMesh.next()) {
                String cui = rsSelUmlsMesh.getString("CUI");
                if (cui.startsWith("C")) {
                    String umlsName = rsSelUmlsMesh.getString("Preferred_Name");
                    umlsCuiNameMapOfChem.put(cui, umlsName);
                }
            }
            rsSelUmlsMesh.close();
        }
        psSelUmlsMesh.close();
        return umlsCuiNameMapOfChem;
    }

    public SetMultimap<String, String> getUmlsCuiGeneMap(HashMap<String, Set<String>> meshNameGeneMap,
                                                         HashMap<String, String> meshNameUmlsCuiMap) {
        SetMultimap<String, String> umlsCuiGeneMap = LinkedHashMultimap.create(); // UMLS cui, ncbi id
        for (String meshName : meshNameGeneMap.keySet()) {
            Set<String> geneIds = meshNameGeneMap.get(meshName);
            String cui = meshNameUmlsCuiMap.get(meshName);
            if (!cui.isEmpty()) {
                for (String geneId : geneIds) {
                    umlsCuiGeneMap.put(cui, geneId);
                }
            }
        }
        return umlsCuiGeneMap;
    }

    public HashMap<String, String> getMeshNameUmlsCuiMap(HashMap<String, Set<String>> meshNameGeneIdsMap, Connection connBioCon) throws SQLException {
        String selUmlsMesh = "SELECT `CUI` FROM `umls_mesh` WHERE `Mesh`=?;";
        PreparedStatement psSelUmlsMesh = connBioCon.prepareStatement(selUmlsMesh);
        HashMap<String, String> meshNameUmlsCuiMap = new HashMap<>(); // mesh name, UMLS cui
        for (String meshName : meshNameGeneIdsMap.keySet()) {
            String cui = "";
            psSelUmlsMesh.clearParameters();
            psSelUmlsMesh.setString(1, meshName);
            ResultSet rsSelUmlsMesh = psSelUmlsMesh.executeQuery();
            while (rsSelUmlsMesh.next()) {
                String cuiTemp = rsSelUmlsMesh.getString("CUI");
                if (cuiTemp.startsWith("C")) {
                    cui = cuiTemp;
                }
            }
            rsSelUmlsMesh.close();
            meshNameUmlsCuiMap.put(meshName, cui);
        }
        psSelUmlsMesh.close();
        return meshNameUmlsCuiMap;
    }

    public HashMap<String, Set<String>> getMeshNameGeneMap(SetMultimap<String, String> meshGeneMap,
                                                           Connection connBioCon) throws SQLException {
        String selDesc = "SELECT DISTINCT `DescriptorName` FROM `mesh_descriptor` WHERE `DescriptorUI`=?;";
        String selSupp = "SELECT DISTINCT `HeadingMappedTo` FROM `mesh_supplement` WHERE `SupplementalRecordUI`=?;";
        PreparedStatement psSelDesc = connBioCon.prepareStatement(selDesc);
        PreparedStatement psSelSupp = connBioCon.prepareStatement(selSupp);
        HashMap<String, Set<String>> meshNameGeneMap = new HashMap<>(); // mesh name, ncbi id
        for (String meshId : meshGeneMap.keySet()) {
            Set<String> geneIds = meshGeneMap.get(meshId);
            String meshName;
            if (meshId.startsWith("C")) {
                String columnName = "HeadingMappedTo";
                meshName = setMeshName(meshId, psSelSupp, columnName);
            } else {
                String columnName = "DescriptorName";
                meshName = setMeshName(meshId, psSelDesc, columnName);
            }
            if (!meshName.isEmpty()) {
                meshNameGeneMap.put(meshName, geneIds);
            }
        }
        psSelDesc.close();
        psSelSupp.close();
        return meshNameGeneMap;
    }

    public void insertInToDbCtdRel(Connection connBioRel,
                                   SetMultimap<String, String> umlsCuiGeneMap,
                                   LinkedHashMap<String, String> umlsCuiNameMapOfChem,
                                   LinkedHashMap<String, String> geneIdUmlsNameMap) throws SQLException {
        String insCtdRel = "INSERT INTO `ctd_relation_umls` (`umls_1`, `umls_2`) VALUES (?, ?)";
        PreparedStatement psInsCtdRel = connBioRel.prepareStatement(insCtdRel);
        SetMultimap<String, String> tempRel = getTempRel(umlsCuiGeneMap, umlsCuiNameMapOfChem, geneIdUmlsNameMap);
        Integer count = 0;
        for (Entry<String, String> e : tempRel.entries()) {
            count++;
            String umls_1 = e.getKey();
            String umls_2 = e.getValue();
            psInsCtdRel.clearParameters();
            psInsCtdRel.setString(1, umls_1);
            psInsCtdRel.setString(2, umls_2);
            psInsCtdRel.addBatch();
            System.out.println("Pair-" + count + ": (" + umls_1 + "), (" + umls_2 + ")");

            int remainder = count % 100000;
            if (remainder == 0) {
                System.out.println("----------line_count: " + count + " execute batch...-----------");
                psInsCtdRel.executeBatch();
            }
        }
        //Import remaining batch (<100000) not be executed.
        System.out.println("----------line_count: " + count + " execute batch...-----------");
        psInsCtdRel.executeBatch();
        psInsCtdRel.close();
    }

    public SetMultimap<String, String> getTempRel(SetMultimap<String, String> umlsCuiGeneMap,
                                                  LinkedHashMap<String, String> umlsCuiNameMapOfChem,
                                                  LinkedHashMap<String, String> geneIdUmlsNameMap) {
        SetMultimap<String, String> tempRel = LinkedHashMultimap.create();
        for (Entry<String, String> e : umlsCuiGeneMap.entries()) {
            String umlsCui = e.getKey();
            String ncbiID = e.getValue();
            String umls_1 = umlsCuiNameMapOfChem.get(umlsCui);
            String umls_2 = geneIdUmlsNameMap.get(ncbiID);
            if (umls_1 != null && umls_2 != null && !umls_1.equals(umls_2)) {
                if (!umls_1.isEmpty() && !umls_2.isEmpty() && !umls_1.equals(umls_2)) {
                    tempRel.put(umls_1, umls_2);
                    tempRel.put(umls_2, umls_1);
                }
            }
        }
        return tempRel;
    }

    public void saveGeneNotMachedFile(ArrayList<String> geneNotMatch) {
        // Output Gene not matching Mesh annotation.
        System.out.println(geneNotMatch.size() + " Gene still do not directly matched in Mesh.");
        File geneNotMatchFile = new File("ctd/geneNotMatchMesh_ctd.txt");
        BufferedWriter geneNotMatchOutput;
        try {
            geneNotMatchOutput = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(geneNotMatchFile), "UTF-8"));
            for (String geneId : geneNotMatch) {
                geneNotMatchOutput.write(geneId);
                geneNotMatchOutput.newLine();
            }
            geneNotMatchOutput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Object> getNcbiMesh(SetMultimap<String, String> meshGeneMap,
                                         HashMap<String, String> geneIdNameMap) {
        Logger logger = Logger.getAnonymousLogger();
        ArrayList<String> geneNotMatch = new ArrayList<>();
        LinkedHashMap<String, String> ncbiMeshMap = new LinkedHashMap<>(); // ncbi id, mesh name
        int count_gene = 0;
        int geneSize = meshGeneMap.values().size();
        for (String geneId : meshGeneMap.values()) {

            if (++count_gene % 100 == 0) {
                logger.info(count_gene + " / " + geneSize);
            }

            String meshName = "";
            if (geneIdNameMap.containsKey(geneId)) {
                meshName = geneIdNameMap.get(geneId);
            }

            if (!meshName.isEmpty()) {
                ncbiMeshMap.put(geneId, meshName);
                System.out.println("Gene Mapping: " + geneId + " <=> " + meshName);
            } else {
                geneNotMatch.add(geneId);
                System.err.println("Gene: '" + geneId + "' has no mapping Mesh annotation.");
            }
        }
        ArrayList<Object> ncbiMesh = new ArrayList<>();
        ncbiMesh.add(geneNotMatch);
        ncbiMesh.add(ncbiMeshMap);
        return ncbiMesh;
    }

    public HashMap<String, String> getMeshOfGene(String matchNcbiIdGeneFilePath) throws IOException {
        System.out.println("Get gene's matched Mesh name...");
        HashMap<String, String> geneIdNameMap = new HashMap<>(); // Gene's matched Mesh name: (geneId, geneMeshName)
        BufferedReader ncbiGeneMoleMatchTxt = new BufferedReader(new InputStreamReader(new FileInputStream(new File(matchNcbiIdGeneFilePath)), "UTF-8"));
        while (ncbiGeneMoleMatchTxt.ready()) {
            String[] entry = ncbiGeneMoleMatchTxt.readLine().split("\\t");
            geneIdNameMap.put(entry[1], entry[0]); // geneId, geneMeshName
        }
        ncbiGeneMoleMatchTxt.close();
        return geneIdNameMap;
    }

    public LinkedHashMap<String, String> getMeshOfChem(SetMultimap<String, String> meshGeneMap, Connection connBioCon) throws SQLException {
        System.out.println("Get chem's mesh name...");
        String queryMeshNameSql = "SELECT `DescriptorName` FROM `mesh_descriptor` WHERE `DescriptorUI` = ?";
        PreparedStatement psQueryMeshName = connBioCon.prepareStatement(queryMeshNameSql);
        String querySupplMappingSql = "SELECT `HeadingMappedTo` FROM `mesh_supplement` WHERE `SupplementalRecordUI` = ?";
        PreparedStatement psQuerySupplMapping = connBioCon.prepareStatement(querySupplMappingSql);

        LinkedHashMap<String, String> meshUiNameMap = new LinkedHashMap<>(); // mesh UI, mesh name
        for (String meshUI : meshGeneMap.keySet()) {
            String meshName = "";
            if (meshUI.charAt(0) == 'C') { // If first char of UI = C ==> check supplement
                psQuerySupplMapping.clearParameters();
                psQuerySupplMapping.setString(1, meshUI);
                ResultSet rsQuerySupplMapping = psQuerySupplMapping.executeQuery();
                if (rsQuerySupplMapping.next()) {
                    meshName = rsQuerySupplMapping.getString("HeadingMappedTo");
                }
                rsQuerySupplMapping.close();
            } else if (meshUI.charAt(0) == 'D') {
                psQueryMeshName.clearParameters();
                psQueryMeshName.setString(1, meshUI);
                ResultSet rsQueryMeshName = psQueryMeshName.executeQuery();
                if (rsQueryMeshName.next()) {
                    meshName = rsQueryMeshName.getString("DescriptorName");
                }
                rsQueryMeshName.close();
            }

            if (!meshName.isEmpty()) {
                meshUiNameMap.put(meshUI, meshName);
            } else {
                System.err.println("meshUI not match mesh name: " + meshUI);
            }
        }
        psQueryMeshName.close();
        psQuerySupplMapping.close();
        return meshUiNameMap;
    }

    public SetMultimap<String, String> getMeshGeneByDrugGene(String drugGeneFilePath) {
        SetMultimap<String, String> meshGeneMap = LinkedHashMultimap.create(); //mesh UI, ncbi id
        try (CSVReader ctdChemGeneIn = new CSVReader(new FileReader(drugGeneFilePath))) {
            String[] line;
            while ((line = ctdChemGeneIn.readNext()) != null) {
                if (line[0].charAt(0) != '#') {
                    String meshUI_chem = line[1].trim();
                    String geneNcbi = line[4].trim();
                    meshGeneMap.put(meshUI_chem, geneNcbi);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return meshGeneMap;
    }

    public SetMultimap<String, String> getMeshGeneByGeneDisease(SetMultimap<String, String> meshGeneMap,
                                                                String geneDiseaseFilePath) {
        try (CSVReader ctdDiseaseGeneIn = new CSVReader(new FileReader(geneDiseaseFilePath))) {
            String[] line;
            while ((line = ctdDiseaseGeneIn.readNext()) != null) {
                if (line[0].charAt(0) != '#') {
                    String geneId = line[1].trim();
                    String meshUI_disease = line[3].split(":")[1].trim();
                    meshGeneMap.put(meshUI_disease, geneId);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return meshGeneMap;
    }

    public void transformCtdRelation(Connection connBioRel) throws SQLException {
        SetMultimap<String, String> relationMap = getCtdRelation(connBioRel);
        insertInToDbCtdNeibor(relationMap, connBioRel);
    }

    public void insertInToDbCtdNeibor(SetMultimap<String, String> relationMap,
                                      Connection connBioRel) throws SQLException {
        String insCtdNeighbor = "INSERT INTO `ctd_neighbor_umls` (`umls`, `neighbor_umls`) VALUES (?, ?)";
        PreparedStatement psInsert = connBioRel.prepareStatement(insCtdNeighbor);
        int count = 0;
        int totalRelSize = relationMap.keySet().size();
        for (String key : relationMap.keySet()) {
            count++;
            StringBuilder strb = new StringBuilder();
            for (String value : relationMap.get(key)) {
                strb.append(value);
                strb.append("\n");
            }
            psInsert.setString(1, key);
            psInsert.setString(2, strb.toString());
            psInsert.addBatch();
            System.out.println("Process: " + count + "/" + totalRelSize + " - " + key);
        }
        psInsert.executeBatch();
        psInsert.close();
    }

    public SetMultimap<String, String> getCtdRelation(Connection connBioRel) throws SQLException {
        SetMultimap<String, String> relationMap = LinkedHashMultimap.create();
        UmlsConcept umlsConcept = new UmlsConcept();
        HashSet<String> fullMeshSeed = new HashSet<>(umlsConcept.getSeedsNonRepeated());
        String selCtdRel = "SELECT `umls_1`, `umls_2` FROM `ctd_relation_umls`";
        PreparedStatement psSelCtdRel = connBioRel.prepareStatement(selCtdRel);
        ResultSet rsQuery = psSelCtdRel.executeQuery();
        while (rsQuery.next()) {
            String umls1 = rsQuery.getString("umls_1");
            String umls2 = rsQuery.getString("umls_2");
            if (fullMeshSeed.contains(umls1) && fullMeshSeed.contains(umls2) && !umls1.equals(umls2)) {
                relationMap.put(umls1, umls2);
            }
        }
        rsQuery.close();
        psSelCtdRel.close();
        return relationMap;
    }
}
