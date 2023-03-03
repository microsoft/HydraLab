// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.performance.hprof;

public class BitmapInfo extends ObjectInfo implements Serializable {

    public int width;
    public int height;
    public int density;
    public boolean recycled;
    public int pixelsCount;
    public long nativePtr;
    public float perPixelSize;


    public void computePerPixelSize() {
        perPixelSize = nativeSize * 1f / height / width;
    }

    @Override
    public String getSizeInfo() {
        return super.getSizeInfo() + ", &nbsp;BitmapSize: " + width + " &times; " + height;
    }
}
