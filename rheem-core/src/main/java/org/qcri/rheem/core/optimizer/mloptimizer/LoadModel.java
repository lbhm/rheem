package org.qcri.rheem.core.optimizer.mloptimizer;

import org.qcri.rheem.core.api.Configuration;
import org.qcri.rheem.core.api.exception.RheemException;

import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Load saved learned model
 */
public class LoadModel {

    private static Configuration configuration = new Configuration();

    /**
     * Created on demand an can be closed as well.
     */
    private static BufferedWriter writer;

    private static List<double[]> featureVectors = new ArrayList<>();

    public static void loadModel(List<double[]> featurevectors) throws IOException {

        featureVectors = featurevectors;

        // save features vectors
        featureVectors.stream()
                .forEach(f -> {
                    try {
                        storeVector(f, 0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        // run model loader in python
        Process p = Runtime.getRuntime().exec(configuration.getStringProperty("rheem.core.optimizer.mloptimizer.modelLocation"));

    }

    private static void storeVector(double[] logs, long executionTime) throws IOException {

        try {
            File file = new File(configuration.getStringProperty("rheem.core.optimizer.mloptimizer.saveVectorLocation"));
            final File parentFile = file.getParentFile();
            if (!parentFile.exists() && !file.getParentFile().mkdirs()) {
                throw new RheemException("Could not initialize cardinality repository.");
            }
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), "UTF-8"));

//            // create 2Dlog file directory
//            File file2dLog = new File(configuration.getStringProperty("rheem.core.log.2Dlogs"));
//            //file2dLog.mkdir();
//            if (!parentFile.exists() && !file2dLog.getParentFile().mkdirs()) {
//                throw new RheemException("Could not initialize 2d log repository.");
//            }
        } catch (RheemException e) {
            throw e;
        } catch (Exception e) {
            throw new RheemException(String.format("Cannot write to %s.", configuration.getStringProperty("rheem.core.optimizer.mloptimizer.saveVectorLocation")), e);
        }

        // Handle 1D log storage
        NumberFormat nf = new DecimalFormat("##.#");

        for(int i=0;i<logs.length;i++){
            writer.write( nf.format( logs[i]) + " ");
        }
        writer.write(Long.toString(0));
        writer.write("\n");
        writer.close();
    }
}
