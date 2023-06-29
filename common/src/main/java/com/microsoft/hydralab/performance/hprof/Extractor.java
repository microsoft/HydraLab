// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.performance.hprof;

import com.squareup.haha.perflib.ClassInstance;
import com.squareup.haha.perflib.ClassObj;
import com.squareup.haha.perflib.Field;
import com.squareup.haha.perflib.Instance;
import com.squareup.haha.perflib.Type;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class Extractor {
    /**
     * min size in byte
     */
    private int minNativeSize = 1000;

    protected List<ObjectInfo> resultList = new ArrayList<>();

    public void setMinNativeSize(int minNativeSize) {
        this.minNativeSize = minNativeSize;
    }

    public String getName() {
        return this.getClass().getSimpleName();
    }

    void onExtractInfo(int retainedSizeRanking, Instance instance) {
        ObjectInfo objectInfo = extractInstanceInfo(retainedSizeRanking, instance);
        if (objectInfo == null) {
            return;
        }
        resultList.add(objectInfo);
    }

    abstract ObjectInfo extractInstanceInfo(int retainedSizeRanking, Instance instance);

    public List<ObjectInfo> getResultList() {
        return resultList;
    }

    protected void mapSetBaseAttr(Instance instance, ObjectInfo bitmapInfo) {
        List<Instance> founds = new ArrayList<>();
        ClassObj classObj = instance.getClassObj();
        String className = classObj.getClassName();
        String[] names = new String[1];
        findRelatedInstances(instance, null, founds, names);
        bitmapInfo.instance = instance;
        bitmapInfo.firstLevelLauncherRef = founds.size() > 0 ? founds.get(0) : null;
        bitmapInfo.className = className;
        if (bitmapInfo.firstLevelLauncherRef instanceof ClassObj) {
            bitmapInfo.isStaticMember = true;
        }
        bitmapInfo.fieldName = names[0];
        bitmapInfo.nativeSize = instance.getNativeSize();
        bitmapInfo.distanceToRoot = instance.getDistanceToGcRoot();
        bitmapInfo.retainedSize = instance.getTotalRetainedSize();
        bitmapInfo.size = instance.getSize();
        bitmapInfo.id = instance.getId();
        bitmapInfo.uniqueId = instance.getUniqueId();
    }

    protected void findRelatedInstances(Instance instance, Instance visited, List<Instance> founds, String[] names) {
        if (instance == null) {
            return;
        }
        ClassObj classObj = instance.getClassObj();
        if (classObj == null) {
            if (!(instance instanceof ClassObj)) {
                return;
            }
            classObj = (ClassObj) instance;
        }
        String className = classObj.getClassName();
        if (className.contains(".launcher")) {
            founds.add(instance);
            if (instance instanceof ClassInstance) {
                ClassInstance classInstance = (ClassInstance) instance;
                List<ClassInstance.FieldValue> values = classInstance.getValues();
                for (ClassInstance.FieldValue value : values) {
                    if (Objects.equals(value.getField().getType(), Type.OBJECT)) {
                        if (Objects.equals(value.getValue(), visited)) {
                            if (visited != null) {
                                founds.add(visited);
                                names[0] = value.getField().getName();
                            }
                        }
                    }
                }
                // static case
            } else if (instance instanceof ClassObj) {
                if (instance.getNextInstanceToGcRoot() == null) {
                    founds.add(instance);
                    Map<Field, Object> staticFieldValues = ((ClassObj) instance).getStaticFieldValues();
                    for (Map.Entry<Field, Object> fieldObjectEntry : staticFieldValues.entrySet()) {
                        Field key = fieldObjectEntry.getKey();
                        if (Objects.equals(key.getType(), Type.OBJECT)) {
                            if (Objects.equals(staticFieldValues.get(key), visited)) {
                                if (visited != null) {
                                    founds.add(visited);
                                    names[0] = key.getName();
                                }
                            }
                        }
                    }
                }
            }
            return;
        }
        findRelatedInstances(instance.getNextInstanceToGcRoot(), instance, founds, names);
    }

    public ObjectInfo extractBitmapInfo(int retainedSizeRanking, Instance instance) {
        ClassObj classObj = instance.getClassObj();
        String className = classObj.getClassName();
        if (className.equals("android.graphics.Bitmap")) {
            if (instance instanceof ClassInstance) {
                ClassInstance classInstance = (ClassInstance) instance;
                List<ClassInstance.FieldValue> values = classInstance.getValues();
                BitmapInfo bitmapInfo = new BitmapInfo();
                if (instance.getNativeSize() < minNativeSize) {
                    return null;
                }
                mapSetBaseAttr(instance, bitmapInfo);
                for (ClassInstance.FieldValue value : values) {
                    Field field = value.getField();
                    String name = field.getName();
                    switch (name) {
                        case "mWidth":
                            bitmapInfo.width = (int) value.getValue();
                            break;
                        case "mHeight":
                            bitmapInfo.height = (int) value.getValue();
                            break;
                        case "mDensity":
                            bitmapInfo.density = (int) value.getValue();
                            break;
                        case "mNativePtr":
                            bitmapInfo.nativePtr = (long) value.getValue();
                            break;
                        case "mRecycled":
                            bitmapInfo.recycled = (boolean) value.getValue();
                            break;
                    }
                }
                bitmapInfo.computePerPixelSize();
                return bitmapInfo;

            }
        }
        return null;
    }

    public abstract String getType();

    public void onExtractComplete() {
        resultList.sort((o1, o2) -> Long.compare(o2.retainedSize, o1.retainedSize));
        Iterator<ObjectInfo> iterator = resultList.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            ObjectInfo next = iterator.next();
            if (next.distanceToRoot == 0 || next.getFieldChainString() == null) {
                iterator.remove();
                continue;
            }
            next.index = i + 1;
            i++;
        }
    }
}
