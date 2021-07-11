package weng.util.file;

public class RepurposingEval {
    public static final String focalDrugs = "vocabulary/drugs_seed_umls.txt";
    public static final String focal500Drugs = "vocabulary/500drugs_seed_umls.txt";

    public static final String goldenRankSplit_noSql = "dat_file/eval/goldenRank_noSql_";
    public static final String goldenPairRankSplit_noSql = "dat_file/eval/golden_pairRank_noSql_";
    public static final String goldenTreatRankSplit_noSql = "dat_file/eval/golden_treatRank_noSql_";

    public static final String neighborCount = "dat_file/neighborCount.txt";

    public static final String instance_newFeatureTo100 = "dat_file/instMap/newFeature_1-100/";
    public static final String instance_oldFeatureTo100 = "dat_file/instMap/oldFeature_1-100/";

    public static final String datFileNdcgDirectory = "dat_file/ndcg/";
    public static final String ouputNdcgDirectory = "output/ndcg/";

    public static final String ndcgSVMOldFeature_diseaseCount = "/svm_old_feature_";
    public static final String ndcgRFOldFeature_diseaseCount = "/rf_old_feature_";
    public static final String ndcgNBOldFeature_diseaseCount = "/nb_old_feature_";
    public static final String ndcgLROldFeature_diseaseCount = "/lr_old_feature_";

    public static final String ndcgSVMOriginalFeature_diseaseCount = "/svm_original_feature_";
    public static final String ndcgRFOriginalFeature_diseaseCount = "/rf_original_feature_";
    public static final String ndcgNBOriginalFeature_diseaseCount = "/nb_original_feature_";
    public static final String ndcgLROriginalFeature_diseaseCount = "/lr_original_feature_";

    public static final String ndcgLtcAmw = "/ltc_amw.dat";

    public static final String ndcgImporvingResults = "dat_file/ndcg_improving_results.dat";
}
