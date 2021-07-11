//package weng.util.calculator;
//
//import weng.feature.PredicateType;
//
//import java.util.HashMap;
//
//public class CrossEntropyCalc {
//
//    public static double count(HashMap<String, Float> abWeight,
//                               HashMap<String, Float> bcWeight) {
//        float abEW = abWeight.get("escalating");
//        float abSW = abWeight.get("suppressing");
//        float abESSum = abEW + abSW;
//        float abE = (abESSum != 0f) ? new PredicateType(abEW / abESSum).getProb() : 0f;
//        float abS = (abESSum != 0f) ? new PredicateType(abSW / abESSum).getProb() : 0f;
//
//        float bcEW = bcWeight.get("escalating");
//        float bcSW = bcWeight.get("suppressing");
//        float bcESSum = bcEW + bcSW;
//        float bcE = (bcESSum != 0f) ? new PredicateType(bcEW / bcESSum).getProb() : 0f;
//        float bcS = (bcESSum != 0f) ? new PredicateType(bcSW / bcESSum).getProb() : 0f;
//        return (formula(abE, bcE, abS, bcS) + formula(bcE, abE, bcS, abS)) / 2;
//    }
//
//    public static double formula(float e1, float e2, float s1, float s2) {
//        return -(e1 * Math.log(sigmoid(e2)) + s1 * Math.log(sigmoid(s2)));
//    }
//
//    private static double sigmoid(double x) {
//        return 1d / (1d + Math.exp(-x));
//    }
//}

package weng.util.calculator;

import weng.feature.PredicateType;

import java.util.HashMap;

public class CrossEntropyCalc {

    public static double count(HashMap<String, Float> abWeight,
                               HashMap<String, Float> bcWeight) {
        float abEW = abWeight.get("escalating") + (float) Math.pow(10, -15);
        float abSW = abWeight.get("suppressing") + (float) Math.pow(10, -15);
        float abESSum = abEW + abSW + (float) Math.pow(10, -15) * 2;
        float abE = new PredicateType(abEW / abESSum).getProb();
        float abS = new PredicateType(abSW / abESSum).getProb();

        float bcEW = bcWeight.get("escalating") + (float) Math.pow(10, -15);
        float bcSW = bcWeight.get("suppressing") + (float) Math.pow(10, -15);
        float bcESSum = bcEW + bcSW + (float) Math.pow(10, -15) * 2;
        float bcE = new PredicateType(bcEW / bcESSum).getProb();
        float bcS = new PredicateType(bcSW / bcESSum).getProb();

        return (formula(abE, bcE, abS, bcS) + formula(bcE, abE, bcS, abS)) / 2;
    }

    public static double formula(float e1, float e2, float s1, float s2) {
        return -(e1 * Math.log(e2) + s1 * Math.log(s2));
    }
}