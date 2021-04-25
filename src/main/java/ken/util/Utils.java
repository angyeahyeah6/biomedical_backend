package ken.util;


import weka.core.Instances;
import weka.core.converters.ArffSaver;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lbj23k on 2017/5/4.
 */
public class Utils {
    public static List<String[]> readFile(String filepath, boolean withHeader) {
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

    public static void writeLineFile(List<String> ls, String path) {
        try (FileWriter writer = new FileWriter(new File(path))) {
            for (String line : ls) {
                writer.write(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<String> readLineFile(String path) {
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

    public static void writeObject(Object obj, String path) {
        try (FileOutputStream fout = new FileOutputStream(path);
             ObjectOutputStream objout = new ObjectOutputStream(fout)) {
            objout.writeObject(obj);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static Object readObjectFile(String path) {
        Object result = null;
        try (FileInputStream file = new FileInputStream(path);
             InputStream buffer = new BufferedInputStream(file);
             ObjectInputStream objIn = new ObjectInputStream(buffer)) {
            result = objIn.readObject();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return result;
    }

    public static void writeAttr(String filepath, Instances data) {
        ArffSaver saver = new ArffSaver();
        saver.setInstances(data);
        try {
            saver.setFile(new File(filepath));
            saver.writeBatch();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static Instances readAttrFile(String path) {
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
