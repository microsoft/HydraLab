// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.performance.hprof;

import com.squareup.haha.perflib.ClassObj;
import com.squareup.haha.perflib.Instance;

import java.io.Serializable;

import static com.microsoft.hydralab.common.util.FileUtil.getSizeStringWithTagIfLarge;

public class ObjectInfo implements Serializable {
    public static final int SIZE_THRESHOLD = 500 * 1024;

    public int index;
    public int distanceToRoot;
    public String fieldName;
    public Instance instance;
    public Instance firstLevelLauncherRef;
    public long nativeSize;
    public long retainedSize;
    public long size;
    public boolean isStaticMember;
    public long id;
    public long uniqueId;
    public String className;

    public String getFieldChain(String lineEnding) {
        if (className != null && className.startsWith("dalvik.system.PathClassLoader")) {
            return null;
        } else {
            return HeapProfProcessor.generateFieldChainString(instance, lineEnding);
        }
    }

    protected String getRefClassName() {
        if (firstLevelLauncherRef == null) {
            return "null";
        }
        ClassObj classObj = firstLevelLauncherRef.getClassObj();
        if (classObj == null) {
            if (firstLevelLauncherRef instanceof ClassObj) {
                return ((ClassObj) firstLevelLauncherRef).getClassName();
            }
            return "null";
        }
        return classObj.getClassName();
    }

    protected String getSizeInfo() {
        String retainedSizeString = "RetainedSize: " + getSizeStringWithTagIfLarge(retainedSize, SIZE_THRESHOLD);
        String nativeSizeString = "";
        if (nativeSize != 0) {
            nativeSizeString = ", &nbsp;NativeSize: " + getSizeStringWithTagIfLarge(nativeSize, SIZE_THRESHOLD);
        }
        return retainedSizeString + nativeSizeString;
    }

    public String getFieldChainString() {
        String fieldChain = getFieldChain("<br>");
        if (fieldChain == null) {
            return null;
        }
        return "<font size=3>" + fieldChain + "</font>";
    }
}
