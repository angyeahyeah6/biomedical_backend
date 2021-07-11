package weng.main;

import weng.util.Utils;
import weng.util.file.sql.Csv;
import weng.util.file.sql.Json;

import java.io.IOException;

public class NoSqlDataPreparation {
    public static void main(String[] args) {
        Utils utils = new Utils();
        utils.csvPreparation(Csv.filePath);
        for (int fileNum = 1; fileNum <= 147; fileNum++) {
            System.out.println("file " + fileNum);
            String csvPath = Csv.directory + Csv.fileName + fileNum + ".csv";
            String jsonPath = Json.directory + Csv.fileName + fileNum + ".json";
            try {
                utils.csvToJson(csvPath, jsonPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
