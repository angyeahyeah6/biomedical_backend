package l2r.process;

import java.io.FileNotFoundException;
import java.io.IOException;

import l2r.data.Sample;

/**
 * The Interface DataReader.
 */
public interface DataReader {

    /**
     * 从文档中读取下一个样本（特定query，多个doc）
     * 注意：实现者在这个例子方法的实现过程中，不必保证正规化，使用者必须自行正规化
     *
     * @return 文档中下一个样本，如果为null表示读完了所有文档数据。
     * @throws IOException
     */
    public abstract Sample getNextSample() throws IOException;

    /**
     * 从文档头，开始重新读
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    public abstract void reset() throws FileNotFoundException, IOException;

    /**
     * 最后的清理工作.
     */
    public abstract void close();
}
