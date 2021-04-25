package l2r.process;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


import l2r.data.Document;
import l2r.data.MSDocument;
import l2r.data.Sample;

public class SampleReader {
    private List<Sample> samples = new ArrayList<Sample>();
    private List<Integer> qidList = new ArrayList<Integer>();

    /**
     * 从filePath中读取文件，生成samples（相同的qid的document组成一个sample）
     *
     * @param filePath
     */
    public void readSamples(String filePath) {
        Sample sample = null;
        BufferedReader reader = null;
        String str = null;
        File file = new File(filePath);
        try {
            reader = new BufferedReader(new FileReader(file));
            try {
                //初始化，开始时samples为空，第一次读取一定生成一个新的sample
                if ((str = reader.readLine()) != null) {
                    Document doc = MSDocument.parseDoc(str);
                    int qid = doc.getQid();
                    qidList.add(qid);
                    sample = new Sample(qid);
                    sample.add(doc);
                }
                while ((str = reader.readLine()) != null) {
                    Document doc = MSDocument.parseDoc(str);
                    int qid = doc.getQid();
                    if (!qidList.contains(qid)) {
                        //如果qidList不包含该document的qid，说明上一个sample已经读取完毕，将上一个加入samples列表，并开始构建一个新的sample
                        //一个sample读取完毕后，同时计算它的平均值和中位数
                        samples.add(sample);
                        qidList.add(qid);
                        sample = new Sample(qid);
                        sample.add(doc);
                    } else {
                        //如果qidList包含该document的qid，说明该document仍属于当前的sample，将其添加到当前sample中
                        sample.add(doc);
                    }
                }
                //因为最后一个sample在上面过程中没有加入，因此还要将其加入列表
                samples.add(sample);
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 对sample做简单的预处理，首先对所有的特征向量做归一化处理，然后把包含太多或太少（10%）document的samples删除
     *
     * @return
     * @throws IOException
     */
    public List<Sample> preprocess() throws IOException {
        normalize();
        deleteSamples();
        return samples;
    }

    /**
     * 调用normalizer中的方法对所有文档的features进行归一化
     *
     * @throws IOException
     */
    public void normalize() {
        MSNormalizer normalizer = new MSNormalizer();
        normalizer.computeNorParameters(samples);
        for (int i = 0; i < samples.size(); i++) {
            Sample sample = samples.get(i);
            for (int j = 0; j < sample.getDocuments().size(); j++) {
                Document doc = sample.getDocuments().get(j);
                doc = normalizer.normalize(doc);
                sample.getDocuments().set(j, doc);
            }
            samples.set(i, sample);
        }
    }

    /**
     * 删除两种samples节点，一种是包含的document数量过多或过少的；另一种是与其它所有sample相似度值
     * 都很高的（这些sample没有区分性，会影响聚类结果）
     */
    public void deleteSamples() {
        //System.out.println("before deleteCount:");
        sortByDocCount();
        //display();
        int deleteCount = (int) (samples.size() * 0.1);
        while (deleteCount > 0 && samples.size() > 0) {
            samples.remove(0);
            samples.remove(samples.size() - 1);
            deleteCount--;
        }

        //System.out.println("after deleteCount:");
        //display();
        for (int i = 0; i < samples.size(); i++) {
            samples.get(i).computeMeanFeatures();
            samples.get(i).computeMedianFeatures();
        }
        sortByFeatures();
        //System.out.println("before deleteFeature:");
        //displayFeature();
        deleteCount = (int) (samples.size() * 0.1);
        while (deleteCount > 0 && samples.size() > 0) {
            samples.remove(0);
            deleteCount--;
        }
        //System.out.println("after deleteFeature:");
        //displayFeature();

    }

    /**
     * 将samples按照document的数量多少从小到大排列
     */
    public void sortByDocCount() {
        Collections.sort(samples, new Comparator<Sample>() {
            public int compare(Sample s1, Sample s2) {
                return s1.getDocCount() - s2.getDocCount();
            }
        });
    }

    /**
     * 将每个sample的meanFeatures的每一维的值相加，并按降序排列
     */
    public void sortByFeatures() {
        Collections.sort(samples, new Comparator<Sample>() {
            public int compare(Sample s1, Sample s2) {
                double feature1 = 0.0;
                double feature2 = 0.0;
                for (int i = 0; i < s1.getMeanFeatures().size(); i++) {
                    feature1 += s1.getMeanFeatures().get(i);
                    feature2 += s2.getMeanFeatures().get(i);
                }
                if (feature1 < feature2)
                    return 1;
                else if (feature1 > feature2)
                    return -1;
                else
                    return 0;
            }
        });
    }

    /**
     * 输出每个sample的document数
     */
    public void display() {
        for (int i = 0; i < samples.size(); i++) {
            System.out.println("qid: " + samples.get(i).getQid() + "  docCount: " + samples.get(i).getDocCount());
        }
    }

    /**
     * 输出显示每个sample的特征向量（平均值）
     */
    public void displayFeature() {
        double featureCount = 0.0;
        for (Sample sample : samples) {
            for (double feature : sample.getMeanFeatures()) {
                featureCount += feature;
            }
            System.out.println("qid: " + sample.getQid() + " featureCount:" + featureCount);
            featureCount = 0.0;
        }
    }

    public List<Sample> getSamples() {
        return samples;
    }


    public void setSamples(List<Sample> samples) {
        this.samples = samples;
    }


    public List<Integer> getQidList() {
        return qidList;
    }


    public void setQidList(List<Integer> qidList) {
        this.qidList = qidList;
    }
}
