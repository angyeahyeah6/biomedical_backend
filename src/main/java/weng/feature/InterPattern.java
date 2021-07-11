package weng.feature;

import java.util.HashMap;

public class InterPattern {
    private int offset;
    private int amplify;

    private final float minMaxThreshold = 0.3f;

    public InterPattern(HashMap<String, Float> abWeight,
                        HashMap<String, Float> bcWeight) {
        String abES = esClassify(abWeight, minMaxThreshold);
        String bcES = esClassify(bcWeight, minMaxThreshold);
        setOffset(offset(abES, bcES));
        setAmplify(amplify(abES, bcES));
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public void setAmplify(int amplify) {
        this.amplify = amplify;
    }

    public int getOffset() {
        return offset;
    }

    public int getAmplify() {
        return amplify;
    }

    public int offset(String ABESClass, String BCESClass) {
        return ABESClass.equals("escalating") && BCESClass.equals("suppressing") ||
                ABESClass.equals("suppressing") && BCESClass.equals("escalating") ? 1 : 0;
    }

    public int amplify(String ABESClass, String BCESClass) {
        return ABESClass.equals("escalating") && BCESClass.equals("escalating") ||
                ABESClass.equals("suppressing") && BCESClass.equals("suppressing") ? 1 : 0;
    }

    public String esClassify(HashMap<String, Float> ESClassWeight,
                             float ESRelativeFreqThreshold) {
        float escalatingWeight = ESClassWeight.get("escalating");
        float suppressingWeight = ESClassWeight.get("suppressing");
        float undeterminedWeight = ESClassWeight.get("undetermined");
        String ESClass = determineESClass(escalatingWeight, suppressingWeight, undeterminedWeight, ESRelativeFreqThreshold);
        return ESClass;
    }

    public String determineESClass(float escalatingWeight, float suppressingWeight,
                                   float undeterminedWeight, float ESRelativeFreqThreshold) {
        // Method: Min/Max Ratio
        String ESClass;
        float maxWeight = Math.max(escalatingWeight, suppressingWeight);
        float minWeight = Math.min(escalatingWeight, suppressingWeight);
        if (maxWeight != 0) {
            float ESScore = minWeight / maxWeight;
            if (ESScore <= ESRelativeFreqThreshold) {
                if (escalatingWeight > suppressingWeight) {
                    ESClass = "escalating";
                } else {
                    ESClass = "suppressing";
                }
            } else {
                ESClass = "ambiguous";
            }
        } else {
            if (undeterminedWeight != 0) {
                ESClass = "undetermined";
            } else {
                ESClass = "unimportant";
            }
        }
        return ESClass;
    }
}
