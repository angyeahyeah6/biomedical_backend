package l2r.data;

import java.util.ArrayList;
import java.util.List;

/**
 * The Class Sample.
 */
public final class Sample {

    /**
     * The documents.
     */
    private ArrayList<Document> documents = new ArrayList<Document>();
    private int docCount = 0;
    //meanFeatures存放所有特征的平均值
    private List<Double> meanFeatures = new ArrayList<Double>();
    //存放meanFeatures各维之和
    private double featuresCount = 0.0;
    //medianFeatures存放最中间的一个特征向量（中位数）
    private List<Double> medianFeatures = new ArrayList<Double>();

    public Document getQueryFeature() {
        ArrayList<Double> queryFeature = new ArrayList<Double>();
        int qid = 0;
        int relId = 0;
        for (Document doc : documents) {
            qid = doc.getQid();
            ArrayList<Double> feature = doc.getFeatures();
            queryFeature.addAll(feature);
        }
        Document queryDoc = new MSDocument(qid, relId, queryFeature);
        return queryDoc;
    }

    /**
     * The qid.
     */
    private int qid;

    /**
     * Instantiates a new sample.
     *
     * @param qid query id
     */
    public Sample(int qid) {
        this.qid = qid;
    }

    /**
     * Adds the.
     *
     * @param doc the doc
     */
    public void add(Document doc) {
        documents.add(doc);
        docCount++;
    }

    /**
     * Gets the documents.
     *
     * @return the documents
     */
    public ArrayList<Document> getDocuments() {
        return documents;
    }


    /**
     * 计算该sample的特征向量的平均值，方法是将每篇文档的特征向量的对应维的值相加，最后除以文档总数
     */
    public void computeMeanFeatures() {
        if (docCount == 0)
            return;
        int featureSize = documents.get(0).getFeatures().size();
        Double[] feature = new Double[featureSize];
        for (int i = 0; i < docCount; i++) {
            for (int j = 0; j < featureSize; j++) {
                if (i == 0) {
                    feature[j] = documents.get(i).getFeatures().get(j);
                } else {
                    feature[j] += documents.get(i).getFeatures().get(j);
                }
            }
        }
        for (int k = 0; k < featureSize; k++) {
            meanFeatures.add(feature[k] / docCount);
        }
    }

    /**
     * 获取该sample最中间一个文档的特征向量
     */
    public void computeMedianFeatures() {
        int median = docCount / 2;
        medianFeatures = documents.get(median).getFeatures();
    }

    /**
     * 用平均值计算该sample的模|sample|
     *
     * @return
     */
    public double getModule() {
        double module = 0.0;
        for (double x : meanFeatures) {
            module += x * x;
        }
        return Math.sqrt(module);
    }

    /**
     * Gets the qid.
     *
     * @return the qid
     */
    public int getQid() {
        return qid;
    }

    public int getDocCount() {
        return docCount;
    }

    public void setDocCount(int docCount) {
        this.docCount = docCount;
    }

    public List<Double> getMeanFeatures() {
        return meanFeatures;
    }

    public void setMeanFeatures(List<Double> meanFeatures) {
        this.meanFeatures = meanFeatures;
    }

    public List<Double> getMedianFeatures() {
        return medianFeatures;
    }

    public void setMedianFeatures(List<Double> medianFeatures) {
        this.medianFeatures = medianFeatures;
    }

    public double getFeaturesCount() {
        return featuresCount;
    }

    public void setFeaturesCount(double featuresCount) {
        this.featuresCount = featuresCount;
    }

}
