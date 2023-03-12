package com.microsoft.hydralab.agent.environment;

import com.alibaba.fastjson.JSON;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class EnvCapabilityScanner {

    protected Map<String, String> systemEnv = System.getenv();

    @SuppressWarnings("checkstyle:InterfaceIsType")
    public interface VariableNames {
        String JAVA_HOME = "JAVA_HOME";
        String ANDROID_HOME = "ANDROID_HOME";
        String PATH_WINDOWS = "Path";
        String PATH_LINUX = "PATH";
        String TEMP_FOLDER = "TEMP";
        String TMP_FOLDER = "TMP";
    }

    public List<EnvCapability> scan() {
        ArrayList<File> files = scanPathExecutables(getPathVariableName());
        List<String> capabilityKeywordFiles = getCapabilityKeywordFiles();
        List<EnvCapability> capabilities = new ArrayList<>();
        for (File file : files) {
            String fileName = file.getName();
            if (capabilityKeywordFiles.contains(fileName)) {
                capabilities.add(new EnvCapability(fileName, file));
            }
        }
        return capabilities;
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
        String[] paths = path.split(";");
        System.out.println(JSON.toJSONString(Arrays.asList(paths)));
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

    protected List<String> getCapabilityKeywordFiles() {
        List<String> capabilityKeywordFiles = new ArrayList<>();
        for (EnvCapability.CapabilityKeyword value : EnvCapability.CapabilityKeyword.values()) {
            for (String executableSuffixOption : getExecutableSuffixOptions()) {
                capabilityKeywordFiles.add(value.name() + executableSuffixOption);
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
    }
}
