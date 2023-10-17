package com.microsoft.hydralab.agent.environment;

import com.microsoft.hydralab.common.entity.agent.EnvCapability;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class EnvCapabilityScanner {
    Logger logger = org.slf4j.LoggerFactory.getLogger(EnvCapabilityScanner.class);
    protected Map<String, String> systemEnv = System.getenv();
    static Pattern versionPattern = Pattern.compile("([0-9]+\\.[0-9]+\\.[0-9]+)");
    static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();

    @SuppressWarnings("checkstyle:InterfaceIsType")
    public interface VariableNames {
        String JAVA_HOME = "JAVA_HOME";
        String ANDROID_HOME = "ANDROID_HOME";
        String PATH_WINDOWS = "Path";
        String PATH_LINUX = "PATH";
        String TEMP_FOLDER = "TEMP";
        String TMP_FOLDER = "TMP";
        String[] SCAN_VARIABLES = {JAVA_HOME, ANDROID_HOME, TEMP_FOLDER, TMP_FOLDER};
    }

    public List<EnvCapability> scan() throws IOException {
        for (String scanVariable : VariableNames.SCAN_VARIABLES) {
            if (!systemEnv.containsKey(scanVariable)) {
                continue;
            }
            logger.info("Scan system variable {} with value {}", scanVariable, systemEnv.get(scanVariable));
        }

        logger.info("start scanning capabilities");
        ArrayList<File> files = scanPathExecutables(getPathVariableName());
        logger.info("Completed scanning capabilities, {}", files);
        List<EnvCapability> capabilities = createCapabilities(files);
        scanCapabilityVersion(capabilities);
        return capabilities;
    }

    @NotNull
    private List<EnvCapability> createCapabilities(ArrayList<File> files) {
        Map<String, EnvCapability.CapabilityKeyword> capabilityKeywordFiles = getCapabilityKeywordFiles();
        List<EnvCapability> capabilities = new ArrayList<>();
        for (File file : files) {
            String fileName = file.getName();
            if (capabilityKeywordFiles.containsKey(fileName)) {
                capabilities.add(new EnvCapability(capabilityKeywordFiles.get(fileName), file));
            }
        }
        return capabilities;
    }

    private void scanCapabilityVersion(List<EnvCapability> capabilities) throws IOException {
        for (EnvCapability capability : capabilities) {
            extractAndParseVersionOutput(capability);
        }
    }

    private void extractAndParseVersionOutput(EnvCapability capability) throws IOException {
        logger.info("Will extractAndParseVersionOutput for {}, {}, {}", capability, capability.getFile().getAbsolutePath(), capability.getKeyword().getFetchVersionParam());
        Process process = Runtime.getRuntime().exec(new String[]{capability.getFile().getAbsolutePath(), capability.getKeyword().getFetchVersionParam()});
        long maxWaitTime = 15;
        try (InputStream stdStream = process.getInputStream();
             InputStream errorStream = process.getErrorStream()) {
            // combine this in case that some output is provided through stdout and some through stderr
            String stdIO = null;
            try {
                stdIO = readInputStreamWithTimeout(stdStream, maxWaitTime, TimeUnit.SECONDS);
            } catch (ExecutionException | TimeoutException e) {
                logger.warn("extractAndParseVersionOutput Exception when getting stdIO of " + capability.getKeyword().name(), e);
            }
            String stdError = null;
            try {
                stdError = readInputStreamWithTimeout(errorStream, maxWaitTime, TimeUnit.SECONDS);
            } catch (ExecutionException | TimeoutException e) {
                logger.warn("extractAndParseVersionOutput Exception when getting stdError of " + capability.getKeyword().name());
            }

            String versionOutput = String.format("Standard Output: %s\nError Output: %s",
                    StringUtils.trim(stdIO),
                    StringUtils.trim(stdError));

            logger.info("extractAndParseVersionOutput versionOutput: {}", versionOutput);

            boolean exited = process.waitFor(5, TimeUnit.SECONDS);
            if (!exited) {
                logger.warn("Failed to get version of " + capability.getKeyword().name());
            }
            capability.getKeyword().setVersionOutput(versionOutput);

            Matcher matcher = versionPattern.matcher(versionOutput);
            if (matcher.find()) {
                capability.setVersion(matcher.group());
            } else {
                logger.warn("Failed to get version of " + capability.getKeyword().name() + " in " + versionOutput);
            }
        } catch (InterruptedException e) {
            logger.error("Failed to get version of " + capability.getKeyword().name() + " at " + capability.getFile().getAbsolutePath(), e);
        } finally {
            process.destroy();
        }
    }

    public static String readInputStreamWithTimeout(InputStream is, long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        Callable<String> readTask = () -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line).append("\n");
                }
                return result.toString();
            }
        };

        FutureTask<String> futureTask = new FutureTask<>(readTask);
        EXECUTOR_SERVICE.execute(futureTask);

        try {
            return futureTask.get(timeout, unit);
        } catch (Exception e) {
            futureTask.cancel(true);
            throw e;
        }
    }

    protected abstract String getPathVariableName();

    public List<File> listExecutableFiles(String path) {
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null) {
            return null;
        }
        logger.info("Scanning path {} with a count of {}", path, listOfFiles.length);
        ArrayList<File> files = new ArrayList<>();
        for (File file : listOfFiles) {
            if (isExecutable(file)) {
                files.add(file);
            }
        }
        return files;
    }

    protected abstract List<String> getExecutableSuffixOptions();

    protected boolean isExecutable(File file) {
        if (!file.isFile() || !file.canExecute()) {
            return false;
        }
        for (String executableSuffixOption : getExecutableSuffixOptions()) {
            if (file.getName().endsWith(executableSuffixOption)) {
                return true;
            }
        }
        return false;
    }

    protected ArrayList<File> scanPathExecutables(String pathVarName) {
        String path = systemEnv.get(pathVarName);
        if (path == null) {
            return null;
        }
        String[] paths = path.split(getPathVariableSeparator());
        // System.out.println(JSON.toJSONString(Arrays.asList(paths)));
        logger.info("Scanning paths with a count of {}: {}", paths.length, Arrays.asList(paths));
        ArrayList<File> files = new ArrayList<>();
        for (String p : paths) {
            List<File> executableFiles = listExecutableFiles(p);
            if (executableFiles == null) {
                continue;
            }
            files.addAll(executableFiles);
        }
        return files;
    }

    protected abstract String getPathVariableSeparator();

    protected Map<String, EnvCapability.CapabilityKeyword> getCapabilityKeywordFiles() {
        Map<String, EnvCapability.CapabilityKeyword> capabilityKeywordFiles = new HashMap<>();
        for (EnvCapability.CapabilityKeyword value : EnvCapability.CapabilityKeyword.values()) {
            for (String executableSuffixOption : getExecutableSuffixOptions()) {
                capabilityKeywordFiles.put(value.name() + executableSuffixOption, value);
            }
        }
        return capabilityKeywordFiles;
    }

    public static class WindowsScanner extends EnvCapabilityScanner {

        private final List<String> options = Arrays.asList(".exe", ".bat", ".cmd");

        @Override
        protected String getPathVariableName() {
            return VariableNames.PATH_WINDOWS;
        }

        @Override
        protected List<String> getExecutableSuffixOptions() {
            return options;
        }

        @Override
        protected String getPathVariableSeparator() {
            return ";";
        }
    }

    public static class LinuxScanner extends EnvCapabilityScanner {
        private final List<String> options = Arrays.asList("", ".sh", ".py");

        @Override
        protected String getPathVariableName() {
            return VariableNames.PATH_LINUX;
        }

        @Override
        protected List<String> getExecutableSuffixOptions() {
            return options;
        }

        @Override
        protected String getPathVariableSeparator() {
            return ":";
        }
    }

    public static class MacOSScanner extends EnvCapabilityScanner {
        private final List<String> options = Arrays.asList("", ".sh", ".py");

        @Override
        protected String getPathVariableName() {
            return VariableNames.PATH_LINUX;
        }

        @Override
        protected List<String> getExecutableSuffixOptions() {
            return options;
        }

        @Override
        protected String getPathVariableSeparator() {
            return ":";
        }
    }
}
