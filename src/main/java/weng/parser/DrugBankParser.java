package weng.parser;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class DrugBankParser {

    public DrugBankParser() {
    }

//    public void drugbankUmlsMap(Connection connBioRel, Connection connBioCon) throws SQLException {
//        String selAllDrugSql = "SELECT `ID`, `Name` FROM `drugbank`";
//        String insDruUmlsSql = "INSERT INTO `drugbank_umls` (`id`, `name`, `umls`) VALUES (?, ?, ?)";
//        String selUmlsMatchesSql = "SELECT DISTINCT `PREFERRED_NAME` FROM `semmed_ver26`.`concept` WHERE `PREFERRED_NAME`=? and `TYPE`=\"META\";";
//        PreparedStatement psSelAllDrug = connBioRel.prepareStatement(selAllDrugSql);
//        PreparedStatement psInsDrugUmls = connBioCon.prepareStatement(insDruUmlsSql);
//        PreparedStatement psSelUmlsMatches = connBioCon.prepareStatement(selUmlsMatchesSql);
//
//        Integer line_count = 0;
//        ResultSet allDrugRSet = psSelAllDrug.executeQuery();
//        psSelAllDrug.close();
//        ArrayList<String> notMatchDrug = new ArrayList<>();
//        while (allDrugRSet.next()) {
//            line_count++;
//            String drugId = allDrugRSet.getString("ID");
//            String drugName = allDrugRSet.getString("Name");
//            psSelUmlsMatches.clearParameters();
//            psSelUmlsMatches.setString(1, drugName);
//            ResultSet umlsMatchesRSet = psSelUmlsMatches.executeQuery();
//            String umlsName = "";
//            if (umlsMatchesRSet.next()) {
//                umlsName = umlsMatchesRSet.getString("PREFERRED_NAME"); // Match found
//            }
//            if (!umlsName.isEmpty()) {
//                psInsDrugUmls.clearParameters();
//                psInsDrugUmls.setString(1, drugId);
//                psInsDrugUmls.setString(2, drugName);
//                psInsDrugUmls.setString(3, umlsName);
//                psInsDrugUmls.addBatch();
//                System.out.println(line_count + ": '" + drugName + "' mapped to '" + umlsName + "'");
//            } else {
//                //Match not found -> record it.
//                notMatchDrug.add(drugName);
//                System.err.println(line_count + ": '" + drugName + "' has no matched UMLS!!!");
//            }
//        }
//        System.out.println("----------line_count: " + line_count + " execute batch...-----------");
//        psInsDrugUmls.executeBatch();
//        psInsDrugUmls.close();
//        psSelUmlsMatches.close();
//
//        // Print drugs not matched to UMLS
//        System.out.println(notMatchDrug.size() + " DrugBank drugs not directly matched in UMLS");
//        String notMatchDrugbankStepOneFilePath = "drugbank/notMatch_1_drugbank.txt";
//        arrayListToFile(notMatchDrugbankStepOneFilePath, notMatchDrug);
//    }

    public void notMatchedDrugbankUmls2ndMap(Connection connBioRel, Connection connBioCon) throws SQLException, IOException {
        ArrayList<String> stillNotMatched = new ArrayList<>();
        // (drugName, (mappedUmls))
        HashMap<String, ArrayList<String>> matchedMoreThanOne = new HashMap<>();
        BufferedReader notMatchedTxt = new BufferedReader(new InputStreamReader(new FileInputStream(new File("drugbank/notMatch_1_drugbank.txt")), "UTF-8"));
        String selDrugSql = "SELECT `ID`, `Name`, `Synonym` FROM `drugbank` WHERE `Name` = ?";
        String insDrugUmlsSql = "INSERT IGNORE INTO `drugbank_umls` (`id`, `name`, `umls`) VALUES (?, ?, ?)";
        String selUmlsMatchesSql = "SELECT DISTINCT `PREFERRED_NAME` FROM `semmed_ver26`.`concept` WHERE `PREFERRED_NAME`=? and `TYPE`=\"META\";";
        PreparedStatement psSelDrug = connBioRel.prepareStatement(selDrugSql);
        PreparedStatement psInsDrugUmls = connBioCon.prepareStatement(insDrugUmlsSql);
        PreparedStatement psSelUmlsMatches = connBioCon.prepareStatement(selUmlsMatchesSql);

        Integer line_count = 0; //For execute SQL with batch mode
        while (notMatchedTxt.ready()) {
            line_count++;
            ArrayList<String> mappedUmlsList = new ArrayList<>();
            String drugName = notMatchedTxt.readLine();
            psSelDrug.clearParameters();
            psSelDrug.setString(1, drugName);
            ResultSet selDrugRS = psSelDrug.executeQuery();
            String drugId = "";
            while (selDrugRS.next()) {
                drugId = selDrugRS.getString("ID");
                String synonyms = selDrugRS.getString("Synonym");
                for (String synonym : synonyms.split("\\n")) {
                    psSelUmlsMatches.clearParameters();
                    psSelUmlsMatches.setString(1, synonym);
                    ResultSet selUmlsRS = psSelUmlsMatches.executeQuery();
                    if (selUmlsRS.next()) {
                        System.out.println("(" + line_count + ") Synonym: '" + synonym + "' matched mesh-descriptor ^^");
                        String umlsName = selUmlsRS.getString("PREFERRED_NAME");
                        mappedUmlsList.add(umlsName);
                    }
                }
            }

            if (mappedUmlsList.size() == 0) { // Match still not found
                stillNotMatched.add(drugName);
                System.err.println(line_count + ": '" + drugName + "' has no matched concept!!!");
            } else if (mappedUmlsList.size() == 1) { // Matched one UMLS
                psInsDrugUmls.clearParameters();
                psInsDrugUmls.setString(1, drugId);
                psInsDrugUmls.setString(2, drugName);
                psInsDrugUmls.setString(3, mappedUmlsList.get(0));
                psInsDrugUmls.addBatch();
                System.out.println(line_count + ": '" + drugName + "' mapped to '" + mappedUmlsList.get(0) + "'");
            } else if (mappedUmlsList.size() > 1) { // Matched more than one UMLS
                matchedMoreThanOne.put(drugName, mappedUmlsList);
                System.out.println(line_count + ": '" + drugName + "' has more than one mapped UMLS annotation. @︿@");
            }
        }
        System.out.println("----------line_count: " + line_count + " execute batch...-----------");
        psInsDrugUmls.executeBatch();
        psInsDrugUmls.close();
        psSelDrug.close();
        psSelUmlsMatches.close();
        notMatchedTxt.close();

        // Print drugs not matched to UMLS
        System.out.println(stillNotMatched.size() + " Drugs still do not directly matched in UMLS in 2nd mapping.");
        String NotMatchStepTwoDrugFilePath = "drugbank/notMatch_2_drugbank.txt";
        arrayListToFile(NotMatchStepTwoDrugFilePath, stillNotMatched);

        // Print drugs matched to more than one UMLS
        System.out.println(matchedMoreThanOne.size() + " Drugs matched more than one concept in UMLS in 2nd mapping.");
        File matchMoreThanOneFile = new File("drugbank/matchMoreThanOne_1_drugbank.txt");
        BufferedWriter matchMoreThanOneOutput;
        try {
            matchMoreThanOneOutput = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(matchMoreThanOneFile), "UTF-8"));
            for (String drugName : matchedMoreThanOne.keySet()) {
                matchMoreThanOneOutput.write(drugName);
                matchMoreThanOneOutput.newLine();
                for (String umls : matchedMoreThanOne.get(drugName)) {
                    matchMoreThanOneOutput.write(umls);
                    matchMoreThanOneOutput.newLine();
                }
                matchMoreThanOneOutput.write("===================================");
                matchMoreThanOneOutput.newLine();
            }
            matchMoreThanOneOutput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void arrayListToFile(String filePath, ArrayList<String> contents) {
        File file = new File(filePath);
        BufferedWriter notMatched;
        try {
            notMatched = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file), "UTF-8"));
            for (String content : contents) {
                notMatched.write(content);
                notMatched.newLine();
            }
            notMatched.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void hashSetToFile(String filePath, HashSet<String> contents) {
        File stillNotMatchUmlsFile = new File(filePath);
        BufferedWriter notMatchOutput;
        try {
            notMatchOutput = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(stillNotMatchUmlsFile), "UTF-8"));
            for (String content : contents) {
                notMatchOutput.write(content);
                notMatchOutput.newLine();
            }
            notMatchOutput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void omimUmlsMap(Connection connBioCon, Connection connSem) throws SQLException {
        String selMimSql = "SELECT `MIM`, `Name` FROM `omim_name`";
        String insMimUmlsSql = "INSERT INTO `omim_umls` (`mim`, `name`, `umls`) VALUES (?, ?, ?)";
        String selUmlsMatchesSql = "SELECT `PREFERRED_NAME` FROM `concept` WHERE `PREFERRED_NAME` LIKE ? ORDER BY CASE " +
                "WHEN `PREFERRED_NAME` LIKE ? THEN 1 WHEN `PREFERRED_NAME` LIKE ? THEN 2 WHEN `PREFERRED_NAME` LIKE ? " +
                "THEN 3 WHEN `PREFERRED_NAME` LIKE ? THEN 4 ELSE 5 END, `PREFERRED_NAME`;";
        PreparedStatement psSelMimName = connBioCon.prepareStatement(selMimSql);
        PreparedStatement psInsMimUmls = connBioCon.prepareStatement(insMimUmlsSql);
        PreparedStatement psSelUmlsMatches = connSem.prepareStatement(selUmlsMatchesSql);

        HashSet<String> notMatchedMimMap = new HashSet<>();
        ResultSet rsMimName = psSelMimName.executeQuery();
        psSelMimName.close();
        Integer line_count = 0;
        while (rsMimName.next()) {
            line_count++;
            String mim = rsMimName.getString("MIM");
            String omimName = rsMimName.getString("Name");
            String umlsName = "";

            psSelUmlsMatches.clearParameters();
            psSelUmlsMatches.setString(1, "%" + omimName + "%");    //WHERE clause
            psSelUmlsMatches.setString(2, omimName);                //2-5 => ORDER BY CASE clause
            psSelUmlsMatches.setString(3, omimName + "%human");
            psSelUmlsMatches.setString(4, omimName + "%");
            psSelUmlsMatches.setString(5, "%" + omimName + "%human");
            ResultSet rsQueryUmls = psSelUmlsMatches.executeQuery();
            if (rsQueryUmls.next()) {
                umlsName = rsQueryUmls.getString("PREFERRED_NAME");
            }
            if (umlsName.isEmpty()) { // Match still not found
                notMatchedMimMap.add(omimName);
                System.err.println(line_count + ": '" + omimName + "' has no matched concept!!!");
            } else { // Match found
                psInsMimUmls.clearParameters();
                psInsMimUmls.setString(1, mim);
                psInsMimUmls.setString(2, omimName);
                psInsMimUmls.setString(3, umlsName);
                psInsMimUmls.addBatch();
                System.out.println(line_count + ": '" + omimName + "' mapped to '" + umlsName + "'");
            }

            int remainder = line_count % 1000;
            if (remainder == 0) {
                System.out.println("----------line_count: " + line_count + " execute batch...-----------");
                psInsMimUmls.executeBatch();
            }
        }
        System.out.println("----------line_count: " + line_count + " execute batch...-----------");
        rsMimName.close();
        psInsMimUmls.executeBatch(); // Import remaining batch (<1000) not be executed.
        psInsMimUmls.close();
        psSelUmlsMatches.close();

        // Print OMIM not matched to UMLS
        System.out.println(notMatchedMimMap.size() + " OMIM still do not directly matched in UMLS.");
        String notMatchOmimStepOneFilePath = "omim/notMatch_1_omim.txt";
        hashSetToFile(notMatchOmimStepOneFilePath, notMatchedMimMap);
    }

    public void notMatchedOmimUmls2ndMap(Connection connBioCon, Connection connSem) throws IOException, SQLException {
        String selMimSql = "SELECT `MIM` FROM `omim_name` WHERE `Name` = ?";
        String insMimUmlsSql = "INSERT INTO `omim_umls` (`mim`, `name`, `umls`) VALUES (?, ?, ?)";
        String selMimUmlsSql = "SELECT `omum`.`cui` FROM `omim_name` AS `omim`, `omim_umlscui` AS `omum` WHERE `omim`.`Name`=? AND `omim`.`MIM`=`omum`.`MIM`;";
        String queryUmlsNameSql = "SELECT `PREFERRED_NAME` FROM `concept` WHERE `CUI`=?;";
        PreparedStatement psSelMim = connBioCon.prepareStatement(selMimSql);
        PreparedStatement psInsMimUmls = connBioCon.prepareStatement(insMimUmlsSql);
        PreparedStatement psSelMimUmls = connBioCon.prepareStatement(selMimUmlsSql);
        PreparedStatement psQueryUmlsName = connSem.prepareStatement(queryUmlsNameSql);

        Integer line_count = 0; //For execute SQL with batch mode
        ArrayList<String> stillNotMatched = new ArrayList<>();
        BufferedReader notMatchedTxt = new BufferedReader(new InputStreamReader(new FileInputStream(new File("omim/notMatch_1_omim.txt")), "UTF-8"));
        while (notMatchedTxt.ready()) {
            line_count++;
            String omimName = notMatchedTxt.readLine();
            psSelMimUmls.clearParameters();
            psSelMimUmls.setString(1, omimName);
            ResultSet selMimUmlsRS = psSelMimUmls.executeQuery();
            if (selMimUmlsRS.next()) {
                String meshId = selMimUmlsRS.getString("cui");
                psQueryUmlsName.clearParameters();
                psQueryUmlsName.setString(1, meshId);
                ResultSet rsQueryUmlsName = psQueryUmlsName.executeQuery();
                String umlsName = "";
                if (rsQueryUmlsName.next()) {
                    umlsName = rsQueryUmlsName.getString("PREFERRED_NAME");
                }
                rsQueryUmlsName.close();

                psSelMim.clearParameters();
                psSelMim.setString(1, omimName);
                ResultSet selMimRS = psSelMim.executeQuery();
                while (selMimRS.next()) {
                    // There are some MIM having the same omim name. Add all.
                    String mim = selMimRS.getString("MIM");
                    psInsMimUmls.clearParameters();
                    psInsMimUmls.setString(1, mim);
                    psInsMimUmls.setString(2, omimName);
                    psInsMimUmls.setString(3, umlsName);
                    psInsMimUmls.addBatch();
                    System.out.println(line_count + ": '" + omimName + "' mapped to '" + umlsName + "'");
                }
            } else {
                stillNotMatched.add(omimName);
                System.err.println(line_count + ": Can't find '" + omimName + "' mapping mesh!!!");
            }

            int remainder = line_count % 5000;
            if (remainder == 0) {
                System.out.println("----------line_count: " + line_count + " execute batch...-----------");
                psInsMimUmls.executeBatch();
            }
        }
        // Import remaining batch (<5000) not be executed.
        System.out.println("----------line_count: " + line_count + " execute batch...-----------");
        psInsMimUmls.executeBatch();
        psInsMimUmls.close();
        psSelMimUmls.close();
        psSelMim.close();
        psQueryUmlsName.close();
        notMatchedTxt.close();

        // Print OMIM not matched to UMLS
        System.out.println(stillNotMatched.size() + " OMIM still do not directly matched in UMLS in 2nd mapping.");
        String notMatchOmimStepTwoFilePath = "omim/notMatch_2_omim.txt";
        arrayListToFile(notMatchOmimStepTwoFilePath, stillNotMatched);
    }
}
