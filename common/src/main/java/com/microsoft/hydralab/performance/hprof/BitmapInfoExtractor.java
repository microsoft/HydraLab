// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.performance.hprof;

import com.squareup.haha.perflib.Instance;

public class BitmapInfoExtractor extends Extractor {

    @Override
    public ObjectInfo extractInstanceInfo(int retainedSizeRanking, Instance instance) {
        return extractBitmapInfo(retainedSizeRanking, instance);
    }

    @Override
    public String getType() {
        return "bitmap";
    }
}
