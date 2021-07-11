package weng.util.calculator;

import weng.feature.PredicateType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class predicateTypeRatioCalc {
    public static double count(HashMap<String, Float> weight, String type) {
        List<String> esType = new ArrayList<>();
        esType.add("escalating");
        esType.add("suppressing");
        float w1 = weight.get(type);
        float w2 = weight.get(esType.stream().filter(e -> !type.equals(e)).collect(Collectors.toList()).get(0));
        float sum = w1 + w2;
        return (sum != 0f) ? new PredicateType(w1 / sum).getProb() : w1;
    }
}
