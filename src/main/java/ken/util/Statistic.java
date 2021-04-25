package ken.util;

import ken.label.AttrTester;
import ken.label.LabeledInterm;
import ken.label.LabeledNetwork;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by lbj23k on 2017/6/30.
 */
public class Statistic {
    public static void main(String[] args) {
        statLabelCase();
    }

    private static void statLabelCase() {
        AttrTester attrTester = new AttrTester();
        ArrayList<LabeledNetwork> pgs = attrTester.getPredicateGraph();
        int intermC = 0, veryImportantC = 0, importantC = 0, unimportantC = 0, caseC = pgs.size();
        for (LabeledNetwork pg : pgs) {
            for (LabeledInterm interm : pg.getIntermediate()) {
                if (interm.getImportance() == 1) unimportantC++;
                else if (interm.getImportance() == 2) importantC++;
                else veryImportantC++;
                intermC++;
            }
        }
        System.out.format("very important:%d, important:%d, unimportant:%d, intermC: %d, caseC: %d\n",
                veryImportantC, importantC, unimportantC, intermC, caseC);
        System.out.format("avg very important:%f, important:%f, unimportant:%f, intermC: %f\n",
                veryImportantC / (double) caseC, importantC / (double) caseC, unimportantC / (double) caseC, intermC / (double) caseC);
    }

    private static void statGoldenSet() {
        Map<String, Map<String, Integer>> goldenMap =
                (Map<String, Map<String, Integer>>) Utils.readObjectFile("dat_file/eval/golden_pairRank.dat");
        Map<String, Map<String, Integer>> treatMap =
                (Map<String, Map<String, Integer>>) Utils.readObjectFile("dat_file/eval/golden_treatRank.dat");
        int noScore3 = 0;
        for (String drug : goldenMap.keySet()) {
            if (!goldenMap.get(drug).values().contains(3)) noScore3++;
        }
        System.out.println("Initial size: " + goldenMap.size());
        System.out.println("Filter no score 3: " + (goldenMap.size() - noScore3));
        System.out.println("Filter by ratio (#score>0/#total>0.01): " + treatMap.size());


    }
}
