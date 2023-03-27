package com.microsoft.hydralab.performance.inspectors;

import com.microsoft.hydralab.common.util.IOSPerfTestHelper;
import com.microsoft.hydralab.common.util.ShellUtils;
import com.microsoft.hydralab.performance.PerformanceInspection;
import com.microsoft.hydralab.performance.PerformanceInspectionResult;
import com.microsoft.hydralab.performance.PerformanceInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.File;

public class IOSEnergyGaugeInspector implements PerformanceInspector {
    private static final String RAW_RESULT_FILE_NAME_FORMAT = "%s_%s_%s_energy.txt";
    protected Logger classLogger = LoggerFactory.getLogger(getClass());
    @Override
    public PerformanceInspectionResult inspect(PerformanceInspection performanceInspection) {
        IOSPerfTestHelper helper = IOSPerfTestHelper.getInstance();
        String inspectionKey = performanceInspection.inspectionKey;
        File rawResultFile;
        if (helper.isRunning(inspectionKey)) {
            rawResultFile = helper.getResultFile(inspectionKey);
        } else {
            File rawResultFolder = new File(performanceInspection.resultFolder, performanceInspection.appId);
            Assert.isTrue(rawResultFolder.exists() || rawResultFolder.mkdir(), "rawResultFolder.mkdirs() failed in" + rawResultFolder.getAbsolutePath());
            rawResultFile = new File(rawResultFolder, String.format(RAW_RESULT_FILE_NAME_FORMAT,
                    getClass().getSimpleName(), performanceInspection.deviceIdentifier, performanceInspection.appId));
            Process p = ShellUtils.execLocalCommandWithRedirect(String.format("tidevice -u %s energy %s",
                    performanceInspection.deviceIdentifier, performanceInspection.appId), rawResultFile, false, classLogger);
            helper.add(inspectionKey, rawResultFile, p);
        }
        return new PerformanceInspectionResult(rawResultFile, performanceInspection);
    }
}
