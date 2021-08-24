package weng.util;


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.simpleflatmapper.csv.CsvParser;
import org.simpleflatmapper.csv.CsvReader;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Created by lbj23k on 2017/5/4.
 */
public class Utils implements Serializable{
    /**
     * uses http://simpleflatmapper.org/0101-getting-started-csv.html for csv parser
     * and jackson-core for JsonGenerator
     * alternative to https://dzone.com/articles/how-to-convert-csv-to-json-in-java
     * stream csv as we read it
     * see https://gist.github.com/arnaudroger/cf7806de83766b51dbfe84a1fab559b0 for reduce garbage version
     */

    private static final long serialVersionUID = 689123320491080199L;

    public void csvPreparation(String csv) {
        try {
            FileReader fr = new FileReader(new File(csv));
            BufferedReader br = new BufferedReader(fr);
            String line;

            int count = 1;
            int fileCount = 1;
            FileWriter fw = new FileWriter(new File("dat_file/sql/mysqlCsv/predication_aggregate_filtered/UPAF_" + fileCount + ".csv"), true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("\"id\",\"s_cui\",\"s_novel\",\"predicate\",\"o_cui\",\"o_novel\",\"pmid\",\"year\",\"pid\",\"isExist\"");
            bw.newLine();
            bw.flush();
            while ((line = br.readLine()) != null) {
                if (count >= 51) {
                    line = "\"" + line + "\"";
                    line = line.replaceAll("INSERT INTO `umls_predication_aggregate_filtered` VALUES ", "")
                            .replaceAll("\\),\\(", "\"\n\"")
                            .replaceAll("\\(", "")
                            .replaceAll("\\)", "")
                            .replaceAll(";", "")
                            .replaceAll("'", "\"\"");
                    line += "\n";
                    bw.write(line);
                    bw.flush();
                    if (count % 40 == 0) {
                        fileCount++;
                        System.out.println("file " + fileCount);
                        bw.close();
                        fw.close();
                        fw = new FileWriter(new File("dat_file/sql/mysqlCsv/predication_aggregate_filtered/UPAF_" + fileCount + ".csv"), true);
                        bw = new BufferedWriter(fw);
                        bw.write("\"id\",\"s_cui\",\"s_novel\",\"predicate\",\"o_cui\",\"o_novel\",\"pmid\",\"year\",\"pid\",\"isExist\"");
                        bw.newLine();
                        bw.flush();
                    }
                }
                count++;
            }
            br.close();
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void csvToJson(String csvPath, String jsonPath) throws IOException {
        Logger logger = Logger.getAnonymousLogger();
        logger.info("[csvToJson] Start");

        StringBuffer content = new StringBuffer();
        try (Stream<String[]> stream = CsvParser.stream(new FileReader(csvPath))) {
            stream.forEach(row -> content.append(
                    Arrays.toString(row)
                            .replaceAll(", ", ",")
                            .replaceAll("\\[", "")
                            .replaceAll("]", "\n")));
        }

        CsvReader reader = CsvParser.reader(content.toString());
        JsonFactory jsonFactory = new JsonFactory();
        Iterator<String[]> iterator = reader.iterator();
        String[] headers = iterator.next();
        Writer writer = new StringWriter();
        try (JsonGenerator jsonGenerator = jsonFactory.createGenerator(writer)) {
            jsonGenerator.writeStartArray();
            Boolean isContinue = true;
            while (iterator.hasNext() && isContinue) {
                String[] values = iterator.next();
                if (values[0].matches("[/*!].*")) {
                    isContinue = false;
                }

                if (isContinue) {
                    jsonGenerator.writeStartObject();
                    int nbCells = Math.min(values.length, headers.length);
                    for (int i = 0; i < nbCells; i++) {
                        jsonGenerator.writeFieldName(headers[i]);
                        jsonGenerator.writeString(values[i]);
                    }
                    jsonGenerator.writeEndObject();
                }
            }
            jsonGenerator.writeEndArray();
        }

        try (FileWriter fw = new FileWriter(new File(jsonPath))) {
            fw.write(writer.toString());
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("[csvToJson] Finish");
    }

    public List<String[]> readFile(String filepath, boolean withHeader) {
        ArrayList<String[]> result = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(new File(filepath)))) {
            String line;
            if (withHeader) line = br.readLine();
            while ((line = br.readLine()) != null) {
                result.add(line.split(","));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public void writeLineFile(List<String> ls, String path) {
        try (FileWriter writer = new FileWriter(new File(path))) {
            for (String line : ls) {
                writer.write(line + "\n");
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String> readLineFile(String path) {
        ArrayList<String> result = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(new File(path)))) {
            while (br.ready()) {
                String line = br.readLine();
                if (line.length() != 0)
                    result.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public void writeObject(Object obj, String path) {
        try (FileOutputStream fout = new FileOutputStream(path);
             ObjectOutputStream objout = new ObjectOutputStream(fout)) {
            objout.writeObject(obj);
            objout.flush();
            objout.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public Object readObjectFile(String path) {
        Object result = null;
        try (FileInputStream file = new FileInputStream(path);
             InputStream buffer = new BufferedInputStream(file);
             ObjectInputStream objIn = new ObjectInputStream(buffer)) {
            result = objIn.readObject();
            objIn.close();
            buffer.close();
            file.close();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return result;
    }

    public void writeAttr(String filepath, Instances data) {
        ArffSaver saver = new ArffSaver();
        saver.setInstances(data);
        try {
            saver.setFile(new File(filepath));
            saver.writeBatch();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public Instances readAttrFile(String path) {
        Instances data = null;
        try (BufferedReader reader = new BufferedReader(
                new FileReader(path))) {
            data = new Instances(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }
}
