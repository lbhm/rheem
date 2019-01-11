package org.qcri.rheem.experiment.tpch.query5;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.qcri.rheem.experiment.ExperimentController;
import org.qcri.rheem.experiment.ExperimentException;
import org.qcri.rheem.experiment.Implementation;
import org.qcri.rheem.experiment.tpch.Query;
import org.qcri.rheem.experiment.utils.parameters.RheemParameters;
import org.qcri.rheem.experiment.utils.results.RheemResults;
import org.qcri.rheem.experiment.utils.udf.UDFs;

public class Q5 extends Query {

    public Q5(ExperimentController controller) {
        super(controller);
    }

    @Override
    public void addOptions(Options options) {
        Option input_file_option = new Option(
                "i",
                "input_file",
                true,
                "The location of the input file that will use in the TPCH - Q5"
        );
        input_file_option.setRequired(true);
        options.addOption(input_file_option);

        Option output_file_option = new Option(
                "o",
                "output_file",
                true,
                "the location of the output file that will use for the TPCH - Q5"
        );
        output_file_option.setRequired(true);
        options.addOption(output_file_option);
    }

    @Override
    public Implementation buildImplementation(ExperimentController controller, RheemParameters parameters, RheemResults results) {
        String platform = controller.getValue("plat").toLowerCase();


        UDFs udfs = new UDFs();

        Implementation implementation;
        switch (platform){
            case Implementation.FLINK:
                implementation = new Q5FlinkImplementation(platform, parameters, results, udfs);
                break;
            case Implementation.SPARK:
                implementation = new Q5SparkImplementation(platform, parameters, results, udfs);
                break;
            case Implementation.JAVA:
                implementation = new Q5JavaImplementation(platform, parameters, results, udfs);
                break;
            case Implementation.RHEEM:
                implementation = new Q5RheemImplementation(platform, parameters, results, udfs);
                break;
            default:
                throw new ExperimentException(
                        String.format(
                                "The platform %s it's not valid for experiment %s",
                                platform,
                                this.getClass().getSimpleName()
                        )
                );
        }

        return implementation;
    }
}
