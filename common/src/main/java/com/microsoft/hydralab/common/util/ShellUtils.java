// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util;

import com.microsoft.hydralab.agent.runner.ITestRun;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.*;

public class ShellUtils {
    public static final String POWER_SHELL_PATH = "C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe";
    public static boolean isConnectedToWindowsOS = System.getProperty("os.name").startsWith("Windows");

    private static String[] getFullCommand(String command)
    {
        String shellProcess;
        String args;

        if (isConnectedToWindowsOS) {
            // Add execution policy to ensure powershell can run on most of Windows devices
            shellProcess = POWER_SHELL_PATH;
            args = "powershell -ExecutionPolicy Unrestricted -NoProfile -Command";
        } else {
            shellProcess = "sh";
            args = "-c";
        }

        return new String[]{shellProcess, args, command};
    }

    private static String[] getFullCommand(String command, String redirectOutputFullPath)
    {
        String shellProcess;
        String args;
        String newCommand;
        if (isConnectedToWindowsOS) {
            // Add execution policy to ensure powershell can run on most of Windows devices
            shellProcess = POWER_SHELL_PATH;
            args = "powershell -ExecutionPolicy Unrestricted -NoProfile -Command";
            newCommand = command + " | Out-File -FilePath " + redirectOutputFullPath + " -encoding utf8";
        } else {
            shellProcess = "sh";
            args = "-c";
            newCommand = command + " > " + redirectOutputFullPath + " 2>&1";
        }

        return new String[]{shellProcess, args, newCommand};
    }

    @Nullable
    public static Process execLocalCommand(String command, Logger classLogger) {
        return execLocalCommand(command, true, classLogger);
    }

    @Nullable
    public static Process execLocalCommand(String command, boolean needWait, Logger classLogger) {
        Process process = null;
        String[] fullCommand = getFullCommand(command);

        try {
            process = Runtime.getRuntime().exec(fullCommand);
            CommandOutputReceiver err = new CommandOutputReceiver(process.getErrorStream(), classLogger);
            CommandOutputReceiver out = new CommandOutputReceiver(process.getInputStream(), classLogger);
            err.start();
            out.start();
            if (needWait) {
                process.waitFor();
                process = null;
            }
            classLogger.info("Successfully run: " + command);
        } catch (Exception e) {
            classLogger.error("Fail to run: " + command, e);
        }
        return process;
    }

    @Nullable
    public static Process execLocalCommandWithRedirect(String command, File redirectTo, boolean needWait, Logger classLogger) {
        Process process = null;
        String[] fullCommand = getFullCommand(command, redirectTo.getAbsolutePath());

        try {
            process = Runtime.getRuntime().exec(fullCommand);
            CommandOutputReceiver err = new CommandOutputReceiver(process.getErrorStream(), classLogger);
            CommandOutputReceiver out = new CommandOutputReceiver(process.getInputStream(), classLogger);
            err.start();
            out.start();
            if (needWait) {
                process.waitFor();
                process = null;
            }
            classLogger.info("Successfully run: " + String.join(" ", fullCommand));
        } catch (Exception e) {
            classLogger.error("Fail to run: " + String.join(" ", fullCommand), e);
        }
        return process;
    }

    @Nullable
    public static String execLocalCommandWithResult(String command, Logger classLogger) {
        String[] fullCommand = getFullCommand(command);

        try {
            Process process = Runtime.getRuntime().exec(fullCommand);
            // Getting the results
            process.getOutputStream().close();
            InputStream is = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            return result.toString();
        } catch (IOException e) {
            classLogger.error("Fail to run: " + command, e);
        }

        return null;
    }

    public static void killProcessByCommandStr(String commandStr, Logger classLogger) {
        String shellProcess = "";
        String args = "";
        String command = "";
        if (isConnectedToWindowsOS) {
            String processName = commandStr.split(" ")[0];
            shellProcess = POWER_SHELL_PATH;
            args = "-Command";
            command = "\"Get-WmiObject Win32_Process -Filter {name like '%" + processName + "%' and CommandLine like '%" + commandStr.replace(" ", "%") + "%'} | Select-Object ProcessId -OutVariable pids; if(-not $pids -eq '' ) {stop-process -id $pids.ProcessId}\"";
        } else {
            shellProcess = "sh";
            args = "-c";
            command = "kill $(ps aux | grep \"" + commandStr + "\" | grep -v \"grep\" | awk '{print $2}')";
        }
        String[] fullCommand = {shellProcess, args, command};
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(fullCommand);
            CommandOutputReceiver err = new CommandOutputReceiver(process.getErrorStream(), classLogger);
            CommandOutputReceiver out = new CommandOutputReceiver(process.getInputStream(), classLogger);
            err.start();
            out.start();
            process.waitFor();
            process = null;

            classLogger.info("Successfully run: " + String.join(" ", fullCommand));
        } catch (Exception e) {
            classLogger.error("Fail to run: " + String.join(" ", fullCommand), e);
        }
    }

    public static String parseHydraLabVariable(String command, ITestRun testRun, DeviceInfo deviceInfo) {
        //  Available Hydra Lab Variables In Script:
        //  $HydraLab_TestResultFolderPath: The full path of the test result folder
        //  $HydraLab_deviceUdid: The UDID of mobile device. (For Android, it will be equal to the serial number)
        String outPathOnAgent = testRun.getResultFolder().getAbsolutePath() + "/";
        String udid = deviceInfo.getSerialNum();
        String newCommand = command.replace("$HydraLab_TestResultFolderPath", outPathOnAgent);
        newCommand = newCommand.replace("$HydraLab_deviceUdid", udid);
        return newCommand;
    }
}