package weng.util.file;

import weng.util.Utils;

import java.util.HashSet;
import java.util.Set;

public class Predicate {
//    public static final String filePath = "vocabulary/61Predicate.txt";
    public static final String filePath = "vocabulary/31ImportantPredicate.txt";
//    public static final String filePath = "vocabulary/contentWord.txt";
//    public static final String filePath = "vocabulary/12ImportantPredicate.txt";

    public static final Set<String> set = new HashSet<>(new Utils().readLineFile(filePath));
}
