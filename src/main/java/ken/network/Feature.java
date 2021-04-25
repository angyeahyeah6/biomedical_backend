package ken.network;

import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import ken.node.LiteratureNode;
import ken.prepare.Ontology;

import java.util.HashSet;
import java.util.Set;

public class Feature {
    //publication year between 1809-2004 from SemMed_ver26 citations table
    private static final int TOTAL_MEDLINE_JOURNAL = 4268024;
    //all nodes' frequency between 1809 and 2004
    private static final int TOTAL_FREQ_NETWORK = 44458497;

    public static double normalizedMedlineSimilarity(LiteratureNode nodeX, LiteratureNode nodeY) {
        double distance = 0.0;
        //something like this: <1016720,136114>
        final HashSet<String> xDocuments = nodeX.getDocuments();
        final HashSet<String> yDocuments = nodeY.getDocuments();
        int occurX = nodeX.df(), occurY = nodeY.df();
        int cooccurXY = 0;
        Set<String> interSet = Sets.intersection(xDocuments, yDocuments);
        cooccurXY = interSet.size();

        if (cooccurXY == 0) return 0;
        double dividend = Math.max(Math.log(occurX), Math.log(occurY)) - Math.log(cooccurXY);
        double divisor = Math.log(TOTAL_MEDLINE_JOURNAL) - Math.min(Math.log(occurX), Math.log(occurY));
        distance = dividend / divisor;

        if (distance > 1.0) {
            distance = 1.0;
        }

        return 1 - distance;
    }

    public static double jaccardSimilarity(LiteratureNode nodeX, LiteratureNode nodeY) {
        double result = 0.0;
        int intersection = 0;
        int union = 0;

        final Set<String> xDocuments = nodeX.getDocuments();
        final Set<String> yDocuments = nodeY.getDocuments();

        Set<String> interSet = Sets.intersection(xDocuments, yDocuments);

        intersection = interSet.size();
        union = xDocuments.size() + yDocuments.size() - intersection;

        result = (double) intersection / union;
        if (result == Double.NaN) {
            result = 0.0;
        }

        return result;
    }

    public static int ontologyMention(LiteratureNode nodeX, LiteratureNode nodeY) {
        SetMultimap<String, String> ontNodeNeighbors = Ontology.getOntologyNeighbors();
        int result = 0;
        String meshNameX = nodeX.getName();
        String meshNameY = nodeY.getName();
        if (ontNodeNeighbors.containsKey(meshNameX)) {
            if (ontNodeNeighbors.get(meshNameX).contains(meshNameY)) {
                result += 1;
            }
        }
        return result;
    }

}
