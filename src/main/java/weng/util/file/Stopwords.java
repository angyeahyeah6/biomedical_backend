package weng.util.file;

import weng.util.Utils;

import java.util.HashSet;
import java.util.Set;

public class Stopwords {
    public static final String filePath = "vocabulary/stopwords.txt";
    public static final Set<String> set = new HashSet<>(new Utils().readLineFile(filePath));
}
