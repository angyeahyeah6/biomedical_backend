package l2r.module;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import l2r.process.Normalizer;

/**
 * The Interface Module.
 */
public interface Module {


    /**
     * 把模型写入文件.
     *
     * @param f 模型文件
     * @throws FileNotFoundException
     * @throws IOException
     */
    public abstract void write(File f) throws FileNotFoundException, IOException;

    /**
     * Gets the weights.
     *
     * @return the weights
     */
    public abstract double[] getWeights();

    /**
     * Gets the normalizer.
     *
     * @return the normalizer
     */
    public abstract Normalizer getNormalizer();
}
