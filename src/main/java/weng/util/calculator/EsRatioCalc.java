package weng.util.calculator;

import weng.feature.PredicateType;

import java.util.HashMap;

public class EsRatioCalc {
    public static double count(HashMap<String, Float> weight) {
        float e = weight.get("escalating");
        float s = weight.get("suppressing");
        float sum = weight.values().stream().reduce(0f, (x, y) -> x + y);
        float abE = new PredicateType(e / sum).getProb();
        float abS = new PredicateType(s / sum).getProb();
        return abE + abS;
    }
}
