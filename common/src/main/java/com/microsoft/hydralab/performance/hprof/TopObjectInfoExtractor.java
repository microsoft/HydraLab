// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.performance.hprof;

import com.squareup.haha.perflib.ClassInstance;
import com.squareup.haha.perflib.ClassObj;
import com.squareup.haha.perflib.Instance;

public class TopObjectInfoExtractor extends Extractor {
    private final int count;

    public TopObjectInfoExtractor(int count) {
        this.count = count;
    }

    @Override
    public ObjectInfo extractInstanceInfo(int retainedSizeRanking, Instance instance) {
        if (retainedSizeRanking > count) {
            return null;
        }

        ClassObj classObj = instance.getClassObj();
        String className = classObj.getClassName();
        if (className.equals("android.graphics.Bitmap")) {
            return extractBitmapInfo(retainedSizeRanking, instance);
        }

        if (instance instanceof ClassInstance) {
            ObjectInfo objectInfo = new ObjectInfo();
            mapSetBaseAttr(instance, objectInfo);
            return objectInfo;
        }
        return null;
    }

    @Override
    public String getType() {
        return "top" + count;
    }
}
