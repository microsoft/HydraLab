package com.microsoft.hydralab.agent.environment;

import com.microsoft.hydralab.common.entity.agent.EnvCapability;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class EnvCapabilityScanner {
    Logger logger = org.slf4j.LoggerFactory.getLogger(EnvCapabilityScanner.class);
    protected Map<String, String> systemEnv = System.getenv();
    static Pattern versionPattern = Pattern.compile("([0-9]+\\.[0-9]+\\.[0-9]+)");

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

        ArrayList<File> files = scanPathExecutables(getPathVariableName());
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
        Process process = Runtime.getRuntime().exec(new String[]{capability.getFile().getAbsolutePath(), capability.getKeyword().getFetchVersionParam()});
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
             BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            // combine this in case that some output is provided through stdout and some through stderr
            String versionOutput = String.format("Standard Output: %s\nError Output: %s",
                    StringUtils.trim(IOUtils.toString(reader)),
                    StringUtils.trim(IOUtils.toString(error)));
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

    protected abstract String getPathVariableName();

    public List<File> listExecutableFiles(String path) {
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null) {
            return null;
        }
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
