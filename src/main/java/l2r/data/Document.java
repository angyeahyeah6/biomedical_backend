package l2r.data;

import java.util.ArrayList;

/**
 * 文档类接口.
 */
public interface Document {


    /**
     * Gets the features.
     * 注意：实现者在这个例子方法的实现过程中，不必保证正规化，使用者必须自行正规化
     *
     * @return the features
     */
    public abstract ArrayList<Double> getFeatures();

    /**
     * Gets the relevance.
     *
     * @return the relevance
     */
    public abstract int getRelevance();

    /**
     * Gets the query id.
     *
     * @return the query id
     */
    public abstract int getQid();

}
