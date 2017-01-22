package org.qcri.rheem.core.monitor;


import org.json.JSONObject;
import org.qcri.rheem.core.api.Configuration;
import org.qcri.rheem.core.util.fs.FileSystem;
import org.qcri.rheem.core.util.fs.FileSystems;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: Rerwrite properly to be more generic, and register properly with application.
public class Monitor {
    private HashMap<String, Integer> progress = new HashMap<>();
    private List<Map> initialExecutionPlan;
    private String exPlanUrl;
    private String progressUrl;
    private String runId;

    public static final String DEFAULT_RUNS_DIR = "file:///var/tmp/rheem/runs";
    public static final String DEFAULT_RUNS_DIR_PROPERTY_KEY = "rheem.basic.runsdir";

    public void initialize(Configuration config, String runId, List<Map> initialExecutionPlan) throws IOException {
        this.initialExecutionPlan = initialExecutionPlan;
        this.runId = runId;
        String runsDir = config.getStringProperty(DEFAULT_RUNS_DIR_PROPERTY_KEY, DEFAULT_RUNS_DIR);
        final String path = runsDir + "/" + runId;
        this.exPlanUrl = path + "/execplan.json";
        this.progressUrl = path + "/progress.json";

        final FileSystem execplanFile = FileSystems.getFileSystem(exPlanUrl).get();
        try (final OutputStreamWriter writer = new OutputStreamWriter(execplanFile.create(exPlanUrl, true))) {
            HashMap<String, Object> jsonPlanMap = new HashMap<>();
            jsonPlanMap.put("stages", initialExecutionPlan);
            jsonPlanMap.put("run_id", runId);
            JSONObject jsonPlan = new JSONObject(jsonPlanMap);
            writer.write(jsonPlan.toString());
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }

        HashMap<String, Integer> initialProgress = new HashMap<>();
        for (Map stage: initialExecutionPlan) {
            for (Map operator: (List<Map>)stage.get("operators")) {
                initialProgress.put((String)operator.get("name"), 0);
            }
        }
        updateProgress(initialProgress);

    }

    public void updateProgress(HashMap<String, Integer> partialProgress) throws IOException {
        HashMap<String, Object> progressBar = new HashMap<>();
        Integer overall = 0;
        for (String operatorName : partialProgress.keySet()) {
            this.progress.put(operatorName, partialProgress.get(operatorName));
        }

        for (String operatorName: this.progress.keySet()) {
            overall = overall + this.progress.get(operatorName);
        }

        if (this.progress.size()>0)
            overall = overall/this.progress.size();

        final FileSystem progressFile = FileSystems.getFileSystem(progressUrl).get();
        try (final OutputStreamWriter writer = new OutputStreamWriter(progressFile.create(progressUrl, true))) {
            progressBar.put("overall", overall);
            progressBar.put("details", progress);

            JSONObject jsonProgress = new JSONObject(progressBar);
            writer.write(jsonProgress.toString());
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }
}