package l2r.process.listnet;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.IllegalClassFormatException;
import java.util.ArrayList;
import java.util.Arrays;

import l2r.data.Document;
import l2r.data.Sample;
import l2r.module.ListNetModule;
import l2r.module.Module;
import l2r.parameter.Parameters;
import l2r.process.DataReader;
import l2r.process.MSDataReaderMem;
import l2r.process.MSNormalizer;
import l2r.process.Normalizer;
import l2r.process.util.DotMultiply;

/**
 * The Class Trainer.
 */
public class Trainer {

    /**
     * Train. 注意每一百轮训练会保存一次模型，模型命名方式为时间_轮数。其中时间为long类型的表示
     *
     * @param f 训练文本文件
     * @return 训练模型
     * @throws IOException                 Signals that an I/O exception has occurred.
     * @throws IllegalClassFormatException 当计算点积时两个向量维度不一时
     */
    public static Module train(File f) throws IOException,
            IllegalClassFormatException {
        System.out.println("Getting normaizer...");
        Normalizer nor = MSNormalizer.getNormalizer(f);//归一向量
        nor.debug();
        DataReader reader = new MSDataReaderMem(f);
        int featureSize = reader.getNextSample().getDocuments().get(0)
                .getFeatures().size();

        System.out.println("初始化权重（weights）...");
        double[] weights = new double[featureSize];
        for (int i = 0; i < featureSize; i++) {
            weights[i] = Parameters.getWeightInit();
        }

        // 训练
        for (int i = 0; i < Parameters.getEpochNum(); i++) {

            reader.reset();
            Sample sample = null;
            // 为了计算方差，保存oldweights
            double[] oldweights = Arrays.copyOf(weights, weights.length);

            while ((sample = reader.getNextSample()) != null) {// 同一组qid:docs的计算
                sample = nor.normalize(sample);// 归一化
                ArrayList<Document> doclist = sample.getDocuments();

                double[] Z = new double[doclist.size()];// 当前权重向量和每一个doc向量的点乘结果
                double[] ExpYList = new double[doclist.size()];// e^rel e的rel次方
                double ExpYSum = 0.0;// 该组docs的总 e^rel 值

                for (int doc = 0; doc < doclist.size(); doc++) {// 计算 ExpYSum
                    Z[doc] = DotMultiply.dotMutply(weights, doclist.get(doc).getFeatures());//权重与特征向量的积==相关程度分数
                    ExpYList[doc] = Math.exp(doclist.get(doc).getRelevance());//e^标注好的相关度
                    ExpYSum += ExpYList[doc];//总相关度之和
                }

                for (int v = 0; v < featureSize; v++) {
                    double deltaWP1 = 0.0;
                    double deltaWP2 = 0.0;
                    double deltaWP3 = 0.0;
                    for (int doc = 0; doc < doclist.size(); doc++) {
                        deltaWP1 -= ((ExpYList[doc]) * doclist.get(doc).getFeatures().get(v)); // e^y * 所有的特征向量值 求和的相对值

                        double expz = Math.exp(Z[doc]); // 此文档与权重的点乘结果，即最初的rank

                        deltaWP2 += (expz * doclist.get(doc).getFeatures().get(v)); // rank值 * 每个特征向量值 做和

                        deltaWP3 += expz; // 最初的rank之和
                    }
                    double deltaW = deltaWP1 / ExpYSum + deltaWP2 / deltaWP3; //
                    weights[v] -= Parameters.getStep() * deltaW; // 变化权重向量（学习步长*前进长度）
                }
            }
            // 每多少轮训练会保存一次模型
            if ((Parameters.getSave() > 0) && (i % Parameters.getSave() == 0)
                    && i > 0) {
                Module m = ListNetModule.getInstance(weights, nor);
                m.write(new File("tmp" + "_" + i + ".listnet"));
            }
            // 计算方差
            double sum = 0.0;
            for (int v = 0; v < weights.length; v++) {
                sum += Math.pow(oldweights[v] - weights[v], 2);// (arg1)^(arg2)
                // x^y
            }
//			System.out.println("finish training " + (i + 1) + "/"
//					+ Parameters.getEpochNum() + " 方差是：" + sum);

//			System.out.println("权重：");
//			for (int v = 0; v < weights.length; v++) {
//				System.out.println("weight[" + v + "] = " + weights[v]);
//			}
        }
        return ListNetModule.getInstance(weights, nor);
    }
}
