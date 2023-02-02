// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util;

import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.*;

public class ShellUtils {
    public static final String POWER_SHELL_PATH = "C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe";
    public static boolean isConnectedToWindowsOS = System.getProperty("os.name").startsWith("Windows");

    private static String[] getFullCommand(String command)
    {
        String shellProcess = "";
        String argName = "";

        if (isConnectedToWindowsOS) {
            shellProcess = POWER_SHELL_PATH;
            argName = "-Command";
        } else {
            shellProcess = "sh";
            argName = "-c";
        }

        return new String[]{shellProcess, argName, command};
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
        String[] fullCommand = getFullCommand(command + " | Out-File -FilePath " + redirectTo.getAbsolutePath());

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
        } catch (Exception e) {
            classLogger.error("Fail to run: " + command, e);
        }

        return null;
    }

    public static void killProcessByCommandStr(String commandStr, Logger classLogger) {
        String shellProcess = "";
        String argName = "";
        String command = "";
        if (isConnectedToWindowsOS) {
            String processName = commandStr.split(" ")[0];
            shellProcess = POWER_SHELL_PATH;
            argName = "-Command";
            command = "\"Get-WmiObject Win32_Process  -Filter \\\"name like '%" + processName + "%' and CommandLine like '%" + commandStr.replace(" ", "%") + "%'\\\" | Select-Object ProcessId -OutVariable pids; if(-not $pids -eq '' ) {stop-process -id $pids.ProcessId}\"";
        } else {
            shellProcess = "sh";
            argName = "-c";
            command = "kill $(ps aux | grep \"" + commandStr + "\" | grep -v \"grep\" | awk '{print $2}')";
        }
        String[] fullCommand = {shellProcess, argName, command};
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
}
