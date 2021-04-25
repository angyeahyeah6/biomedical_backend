package l2r.process.listnet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.instrument.IllegalClassFormatException;
import java.util.ArrayList;

import l2r.data.Document;
import l2r.data.MSDocument;
import l2r.module.ListNetModule;
import l2r.module.Module;
import l2r.process.util.DotMultiply;

/**
 * The Class ModuleTest.
 */
public class ModuleTest {

    /**
     * The module.
     */
    private final Module module;

    /**
     * Instantiates a new module test.
     *
     * @param module the module
     */
    public ModuleTest(Module module) {
        this.module = module;
    }

    /**
     * Test.
     *
     * @param weights the weights
     * @return the double
     * @throws IllegalClassFormatException the illegal class format exception
     */
    public double test(double[] weights) throws IllegalClassFormatException {
        return DotMultiply.dotMutply(module.getWeights(), weights);
    }

    /**
     * Test.
     *
     * @param weights the weights
     * @return the double
     * @throws IllegalClassFormatException the illegal class format exception
     */
    public double test(ArrayList<Double> weights) throws IllegalClassFormatException {
        return DotMultiply.dotMutply(module.getWeights(), weights);
    }

    public static void main(String[] args) throws IOException, IllegalClassFormatException {
        Module m = ListNetModule.getInstance(new File("msdata.listnet.module"));
        ModuleTest tester = new ModuleTest(m);
        BufferedReader reader = new BufferedReader(new FileReader("/home/luolei/dmc/download_data/data/fold1/test.txt"));

        String line = null;
        while ((line = reader.readLine()) != null) {
            Document doc = MSDocument.parseDoc(line);
            doc = m.getNormalizer().normalize(doc);

            System.out.println("res = " + tester.test(doc.getFeatures()) + " ans = " + doc.getRelevance());
        }
    }
}
