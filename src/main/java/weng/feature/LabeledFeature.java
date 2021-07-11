package weng.feature;

public class LabeledFeature {
    /**
     * TOTAL_MEDLINE_JOURNAL
     * => publication year between 1809-2004 from SemMed_ver26 citations table
     * => Sql: SELECT COUNT(*) FROM `semmed_ver26`.`citations` WHERE `PYEAR` BETWEEN 1809 AND 2004;
     */
    private static final int TOTAL_MEDLINE_JOURNAL = 16174990;

    public LabeledFeature() {
    }

    public static double normalizedMedlineSimilarity(int x, int y, int intersection) {
        if (intersection == 0) return 0;
        double dividend = Math.max(Math.log(x), Math.log(y)) - Math.log(intersection);
        double divisor = Math.log(TOTAL_MEDLINE_JOURNAL) - Math.min(Math.log(x), Math.log(y));
        double distance = dividend / divisor;

        if (distance > 1.0) {
            distance = 1.0;
        }
        return 1 - distance;
    }

//    public static double normalizedMedlineSimilarity(LiteratureNode nodeX, LiteratureNode nodeY, Connection connLit) {
//        // something like this: <1016720,136114>
//        final HashSet<String> xDocuments = nodeX.getDocuments(connLit);
//        final HashSet<String> yDocuments = nodeY.getDocuments(connLit);
//        int occurX = nodeX.df(connLit), occurY = nodeY.df(connLit);
//        Set<String> interSet = Sets.intersection(xDocuments, yDocuments);
//        int cooccurXY = interSet.size();
//
//        if (cooccurXY == 0) return 0;
//        double dividend = Math.max(Math.log(occurX), Math.log(occurY)) - Math.log(cooccurXY);
//        double divisor = Math.log(TOTAL_MEDLINE_JOURNAL) - Math.min(Math.log(occurX), Math.log(occurY));
//        double distance = dividend / divisor;
//
//        if (distance > 1.0) {
//            distance = 1.0;
//        }
//        return 1 - distance;
//    }

    public static double jaccardSimilarity(int x, int y, int intersection) {
        int union = x + y - intersection;
        double result = (double) intersection / union;
        if (result == Double.NaN) {
            result = 0.0;
        }
        return result;
    }

//    public static double jaccardSimilarity(LiteratureNode nodeX, LiteratureNode nodeY, Connection connLit) {
//        final Set<String> xDocuments = nodeX.getDocuments(connLit);
//        final Set<String> yDocuments = nodeY.getDocuments(connLit);
//        Set<String> interSet = Sets.intersection(xDocuments, yDocuments);
//        int intersection = interSet.size();
//        int union = xDocuments.size() + yDocuments.size() - intersection;
//
//        double result = (double) intersection / union;
//        if (result == Double.NaN) {
//            result = 0.0;
//        }
//        return result;
//    }
}
