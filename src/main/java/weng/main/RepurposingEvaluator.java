package weng.main;

import weng.evaluation.Repurposing;
import weng.util.Utils;
import weng.util.file.RepurposingEval;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by lbj23k on 2017/6/23.
 */
public class RepurposingEvaluator {
    public static void main(String[] args) throws IOException {
        Repurposing repurposing = new Repurposing();

        // Compare files' NDCG
        FileWriter fileWriter = new FileWriter(new File(RepurposingEval.ndcgImporvingResults));
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        bufferedWriter.write("benchmark\tevaluation\timproved percentage\t%");
        bufferedWriter.newLine();
        bufferedWriter.flush();

        int diseaseCount = 1000;
        File benchmark;
        benchmark = new File(RepurposingEval.datFileNdcgDirectory + diseaseCount + RepurposingEval.ndcgLtcAmw);
        System.out.println("benchmark filePath: " + RepurposingEval.datFileNdcgDirectory + diseaseCount + RepurposingEval.ndcgLtcAmw);
        System.out.println(benchmark.getName());

        File directory = new File(RepurposingEval.datFileNdcgDirectory + diseaseCount + "/");
        for (File evalFile : directory.listFiles()) {
            if (!evalFile.getName().equals(benchmark.getName())) {
                System.out.println(evalFile.getName());
                double avgImprove = repurposing.calculateAvgEval(benchmark, evalFile, diseaseCount);
                bufferedWriter.write(
                        benchmark.getName().replace(".dat", "") + "\t" +
                                evalFile.getName().replace(".dat", "") + "\t" +
                                String.valueOf(100 * avgImprove) + "\t%");
                bufferedWriter.newLine();
                bufferedWriter.flush();
            }
        }
        bufferedWriter.close();
        fileWriter.close();
    }
}
