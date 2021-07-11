package weng.util.calculator;

import weng.feature.PredicateType;

import java.util.HashMap;

public class IntraEntropyCalc {
    public static double count(HashMap<String, Float> weight) {
        float eW = weight.get("escalating");
        float sW = weight.get("suppressing");
        float sum = eW + sW;
        float e = (sum != 0f) ? new PredicateType(eW / sum).getProb() : 0f;
        float s = (sum != 0f) ? new PredicateType(sW / sum).getProb() : 0f;
        return expectedValue(e) + expectedValue(s);
    }

    public static double expectedValue(float x) {
        if (x != 0f) {
            return -x * Math.log(x);
        } else {
            return 0d;
        }
    }
}