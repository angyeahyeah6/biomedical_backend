package weng.parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class UmlsConceptObject {
    private String CUI;
    private String PreferredTerm;
    private HashSet<String> MapTerms;

    public static final int CHEMICAL_DRUG = 0;
    public static final int DISORDER = 1;
    public static final int GENE_MOLECULARSEQUENCE = 2;
    public static final int PHYSIOLOGY = 3;
    public static final int ANATOMY = 4;
    private static ArrayList<TreeSet<String>> seedList = null;

    /**
     * @return the cUI
     */
    public String getCUI() {
        return CUI;
    }

    /**
     * @param cUI the cUI to set
     */
    public void setCUI(String cUI) {
        CUI = cUI;
    }

    /**
     * @return the preferredTerm
     */
    public String getPreferredTerm() {
        return PreferredTerm;
    }

    /**
     * @param preferredTerm the preferredTerm to set
     */
    public void setPreferredTerm(String preferredTerm) {
        PreferredTerm = preferredTerm;
    }

    /**
     * @return the mapTerms
     */
    public HashSet<String> getMapTerms() {
        return MapTerms;
    }

    /**
     * @param mapTerms the mapTerms to set
     */
    public void setMapTerms(HashSet<String> mapTerms) {
        MapTerms = mapTerms;
    }

    public static ArrayList<String> getIntermSeeds() {
        HashSet<String> intermsUMLS = new HashSet<>();
        try (BufferedReader drugReader = new BufferedReader(new FileReader(
                "umls/ctrolled_vocabulary/Chemicals&Drugs.txt"));
             BufferedReader disorderReader = new BufferedReader(
                     new FileReader("umls/ctrolled_vocabulary/Disorders.txt"));
             BufferedReader geneReader = new BufferedReader(new FileReader(
                     "umls/ctrolled_vocabulary/Genes&MolecularSequences.txt"));
             BufferedReader physiologyReader = new BufferedReader(
                     new FileReader("umls/ctrolled_vocabulary/Physiology.txt"));
             BufferedReader anatomyReader = new BufferedReader(
                     new FileReader("umls/ctrolled_vocabulary/Anatomy.txt"));) {
            while (drugReader.ready()) {
                intermsUMLS.add(drugReader.readLine());
            }
            while (disorderReader.ready()) {
                intermsUMLS.add(disorderReader.readLine());
            }
            while (geneReader.ready()) {
                intermsUMLS.add(geneReader.readLine());
            }
            while (physiologyReader.ready()) {
                intermsUMLS.add(physiologyReader.readLine());
            }
            while (anatomyReader.ready()) {
                intermsUMLS.add(anatomyReader.readLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return (new ArrayList<String>(intermsUMLS));
    }

    public static ArrayList<String> getDrugSeeds() {
        ArrayList<String> drugsUMLS = new ArrayList<String>();
        BufferedReader drugsReader = null;
        try {
            drugsReader = new BufferedReader(new FileReader("umls/ctrolled_vocabulary/Chemicals&Drugs.txt"));
            while (drugsReader.ready()) {
                drugsUMLS.add(drugsReader.readLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            try {
                drugsReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return drugsUMLS;
    }

    public static ArrayList<String> getDisorderSeeds() {
        ArrayList<String> disorderUMLS = new ArrayList<String>();
        BufferedReader disorderReader = null;
        try {
            disorderReader = new BufferedReader(new FileReader("umls/ctrolled_vocabulary/Disorders.txt"));
            while (disorderReader.ready()) {
                disorderUMLS.add(disorderReader.readLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            try {
                disorderReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return disorderUMLS;
    }

    public static ArrayList<String> getSeedsNonRepeated() {
        if (seedList == null) {
            getSeedsGroupedByCategory();
        }
        HashSet<String> umlsSet = new HashSet<>(seedList.get(0).size() * 4);
        for (TreeSet<String> categoryMeshSet : seedList) {
            umlsSet.addAll(categoryMeshSet);
        }
        return (new ArrayList<String>(umlsSet));
    }

    public static ArrayList<TreeSet<String>> getSeedsGroupedByCategory() {
        if (seedList == null) {
            ArrayList<TreeSet<String>> tempList = null;
            BufferedReader drugReader = null;
            BufferedReader disorderReader = null;
            BufferedReader geneReader = null;
            BufferedReader physiologyReader = null;
            BufferedReader anatomyReader = null;
            try {
                /* Set initial category MeSH */
                tempList = new ArrayList<>(5);
                tempList.add(new TreeSet<String>());
                tempList.add(new TreeSet<String>());
                tempList.add(new TreeSet<String>());
                tempList.add(new TreeSet<String>());
                tempList.add(new TreeSet<String>());

                drugReader = new BufferedReader(new FileReader("umls/ctrolled_vocabulary/Chemicals&Drugs.txt"));
                TreeSet<String> drugsUMLS = new TreeSet<String>();
                while (drugReader.ready()) {
                    drugsUMLS.add(drugReader.readLine());
                }
                tempList.set(CHEMICAL_DRUG, drugsUMLS);

                disorderReader = new BufferedReader(new FileReader("umls/ctrolled_vocabulary/Disorders.txt"));
                TreeSet<String> disorderUMLS = new TreeSet<String>();
                while (disorderReader.ready()) {
                    disorderUMLS.add(disorderReader.readLine());
                }
                tempList.set(DISORDER, disorderUMLS);

                geneReader = new BufferedReader(new FileReader("umls/ctrolled_vocabulary/Genes&MolecularSequences.txt"));
                TreeSet<String> geneUMLS = new TreeSet<String>();
                while (geneReader.ready()) {
                    geneUMLS.add(geneReader.readLine());
                }
                tempList.set(GENE_MOLECULARSEQUENCE, geneUMLS);

                physiologyReader = new BufferedReader(new FileReader("umls/ctrolled_vocabulary/Physiology.txt"));
                TreeSet<String> physiologyUMLS = new TreeSet<String>();
                while (physiologyReader.ready()) {
                    physiologyUMLS.add(physiologyReader.readLine());
                }
                tempList.set(PHYSIOLOGY, physiologyUMLS);

                anatomyReader = new BufferedReader(new FileReader("umls/ctrolled_vocabulary/Anatomy.txt"));
                TreeSet<String> anatomyUMLS = new TreeSet<String>();
                while (anatomyReader.ready()) {
                    anatomyUMLS.add(anatomyReader.readLine());
                }
                tempList.set(ANATOMY, anatomyUMLS);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            } finally {
                try {
                    drugReader.close();
                    disorderReader.close();
                    geneReader.close();
                    physiologyReader.close();
                    anatomyReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            seedList = tempList;
        }
        return seedList;
    }

    public static ArrayList<String> getSeed(int type) {
        String path = null;
        switch (type) {
            case CHEMICAL_DRUG:
                path = "umls/ctrolled_vocabulary/Chemicals&Drugs.txt";
                break;
            case DISORDER:
                path = "umls/ctrolled_vocabulary/Disorders.txt";
                break;
            case GENE_MOLECULARSEQUENCE:
                path = "umls/ctrolled_vocabulary/Genes&MolecularSequences.txt";
                break;
            case PHYSIOLOGY:
                path = "umls/ctrolled_vocabulary/Physiology.txt";
                break;
            case ANATOMY:
                path = "umls/ctrolled_vocabulary/Anatomy.txt";
                break;
            default:
                System.err.println("Getting Seed Failed.");
                System.exit(1);
        }
        Set<String> seedUMLS = new HashSet<>();
        try (BufferedReader seedReader = new BufferedReader(
                new FileReader(path))) {
            while (seedReader.ready()) {
                seedUMLS.add(seedReader.readLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return (new ArrayList<String>(seedUMLS));
    }

    public static ArrayList<String> getEvalDrugNames() {
        ArrayList<String> evalDrugsUmls = new ArrayList<String>(101);
        BufferedReader evalDrugsReader = null;
        try {
            evalDrugsReader = new BufferedReader(new FileReader("umls/100drugs_umls.txt"));
            while (evalDrugsReader.ready()) {
                evalDrugsUmls.add(evalDrugsReader.readLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            try {
                evalDrugsReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return evalDrugsUmls;
    }

}
