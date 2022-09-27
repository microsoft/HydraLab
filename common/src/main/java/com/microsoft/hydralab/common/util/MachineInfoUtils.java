// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util;

import java.net.InetAddress;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public class MachineInfoUtils {


    public static String property() {
        StringBuilder builder = new StringBuilder();
        try {
            Runtime r = Runtime.getRuntime();
            Properties props = System.getProperties();
            InetAddress addr;
            addr = InetAddress.getLocalHost();
            String ip = addr.getHostAddress();
            Map<String, String> map = System.getenv();
            String userName = map.get("USERNAME");
            String computerName = map.get("COMPUTERNAME");
            String userDomain = map.get("USERDOMAIN");
            builder.append("user name:    " + userName).append("\n");
            builder.append("machine name:    " + computerName).append("\n");
            builder.append("计算机域名:    " + userDomain).append("\n");
            builder.append("本地ip地址:    " + ip).append("\n");
            builder.append("本地主机名:    " + addr.getHostName()).append("\n");
            builder.append("JVM可以使用的总内存:    " + r.totalMemory()).append("\n");
            builder.append("JVM可以使用的剩余内存:    " + r.freeMemory()).append("\n");
            builder.append("JVM可以使用的处理器个数:    " + r.availableProcessors()).append("\n");
            builder.append("Java的运行环境版本：    " + props.getProperty("java.version")).append("\n");
            builder.append("Java的运行环境供应商：    " + props.getProperty("java.vendor")).append("\n");
            //builder.append("Java供应商的URL：    " + props.getProperty("java.vendor.url")).append("\n");
            builder.append("Java的安装路径：    " + props.getProperty("java.home")).append("\n");
            builder.append("Java的虚拟机规范版本：    " + props.getProperty("java.vm.specification.version")).append("\n");
            builder.append("Java的虚拟机规范供应商：    " + props.getProperty("java.vm.specification.vendor")).append("\n");
            builder.append("Java的虚拟机规范名称：    " + props.getProperty("java.vm.specification.name")).append("\n");
            builder.append("Java的虚拟机实现版本：    " + props.getProperty("java.vm.version")).append("\n");
            builder.append("Java的虚拟机实现供应商：    " + props.getProperty("java.vm.vendor")).append("\n");
            builder.append("Java的虚拟机实现名称：    " + props.getProperty("java.vm.name")).append("\n");
            builder.append("Java运行时环境规范版本：    " + props.getProperty("java.specification.version")).append("\n");
            //builder.append("Java运行时环境规范供应商：    " + props.getProperty("java.specification.vender")).append("\n");
            builder.append("Java运行时环境规范名称：    " + props.getProperty("java.specification.name")).append("\n");
            builder.append("Java的类格式版本号：    " + props.getProperty("java.class.version")).append("\n");
            //builder.append("Java的类路径：    " + props.getProperty("java.class.path")).append("\n");
            //builder.append("加载库时搜索的路径列表：    " + props.getProperty("java.library.path")).append("\n");
            builder.append("默认的临时文件路径：    " + props.getProperty("java.io.tmpdir")).append("\n");
            //builder.append("一个或多个扩展目录的路径：    " + props.getProperty("java.ext.dirs")).append("\n");
            builder.append("操作系统的名称：    " + props.getProperty("os.name")).append("\n");
            builder.append("操作系统的构架：    " + props.getProperty("os.arch")).append("\n");
            builder.append("操作系统的版本：    " + props.getProperty("os.version")).append("\n");
            builder.append("文件分隔符：    " + props.getProperty("file.separator")).append("\n");
            builder.append("路径分隔符：    " + props.getProperty("path.separator")).append("\n");
            builder.append("行分隔符：    " + props.getProperty("line.separator")).append("\n");
            builder.append("用户的账户名称：    " + props.getProperty("user.name")).append("\n");
            builder.append("用户的主目录：    " + props.getProperty("user.home")).append("\n");
            builder.append("用户的当前工作目录：    " + props.getProperty("user.dir")).append("\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    public static boolean isOnMacOS() {
        Properties props = System.getProperties();
        String osName = props.getProperty("os.name");
        return osName.contains("Mac");
    }

    public static boolean isOnWindows() {
        Properties props = System.getProperties();
        String osName = props.getProperty("os.name");
        return osName.toLowerCase(Locale.US).contains("windows");
    }

    public static String getCountryNameFromCode(String code) {
        if (code == null) {
            return null;
        }
        for (Locale availableLocale : Locale.getAvailableLocales()) {
            if (availableLocale == null) {
                continue;
            }
            if (code.toUpperCase().equalsIgnoreCase(availableLocale.getCountry())) {
                return availableLocale.getDisplayCountry();
            }
        }
        return null;
    }
}
