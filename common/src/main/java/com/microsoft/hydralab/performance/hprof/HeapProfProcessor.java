// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.performance.hprof;

import com.android.tools.perflib.captures.DataBuffer;
import com.android.tools.perflib.captures.MemoryMappedFileBuffer;
import com.squareup.haha.perflib.*;
import gnu.trove.THashMap;
import gnu.trove.TObjectProcedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.microsoft.hydralab.common.util.FileUtil.getSizeStringWithTagIfLarge;

public class HeapProfProcessor {
    public static final int MAX_FIELD_CHAIN_DEPTH = 10;
    //The max length that channel can display
    public static final int HTML_MAX_LENGTH = 18000;

    private final File heapDumpFile;
    private static Logger logger = LoggerFactory.getLogger(HeapProfProcessor.class.getSimpleName());
    private HashMap<String, Extractor> extractorMap = new HashMap<>();

    public HeapProfProcessor(File heapFile) {
        heapDumpFile = heapFile;
    }

    private static String generateRootKey(RootObj root) {
        return String.format("%s@0x%08x", root.getRootType().getName(), root.getId());
    }

    /**
     * TODO
     *
     * @param instance
     * @return
     */
    public static String getObjectRefTrace(Instance instance) {
        return "todo";
    }

    public void registerExtractor(Extractor extractor) {
        extractorMap.put(extractor.getName(), extractor);
    }


    public void loadAndExtract() throws IOException {
        if (!heapDumpFile.exists()) {
            return;
        }

        DataBuffer buffer = new MemoryMappedFileBuffer(heapDumpFile);

        Snapshot snapshot = Snapshot.createSnapshot(buffer);
        logger.info("start createSnapshot: " + heapDumpFile.getName());

        deduplicateGcRoots(snapshot);
        logger.info("deduplicateGcRoots: " + heapDumpFile.getName());

        snapshot.computeDominators();
        logger.info("compute dominators finished: " + heapDumpFile.getName());

        List<Instance> dominatorList = snapshot.getReachableInstances();
        if (dominatorList.isEmpty()) {
            logger.warn("no gReachable Instances: in file: {}", heapDumpFile.getName());
            return;
        }
        dominatorList.sort((instance1, instance2) -> -Long.compare(instance1.getTotalRetainedSize(), instance2.getTotalRetainedSize()));
        logger.info("sortByRetainedSize finish start listing dominatorList: {}, in file: {}", dominatorList.size(), heapDumpFile.getName());

        Iterator<Instance> instanceIterator = dominatorList.iterator();

        int index = 0;
        while (instanceIterator.hasNext()) {
            Instance instance = instanceIterator.next();
            if (instance.getClassObj() == null) {
                continue;
            }
            processInstance(index++, instance);
        }

        // sort all extracted object info by retained size
        for (Map.Entry<String, Extractor> entry : extractorMap.entrySet()) {
            entry.getValue().onExtractComplete();
        }
    }

    /**
     * Pruning duplicates reduces memory pressure from hprof bloat added in Marshmallow.
     */
    private void deduplicateGcRoots(Snapshot snapshot) {
        // THashMap has a smaller memory footprint than HashMap.
        final THashMap<String, RootObj> uniqueRootMap = new THashMap<>();

        final Collection<RootObj> gcRoots = snapshot.getGCRoots();
        for (RootObj root : gcRoots) {
            String key = generateRootKey(root);
            if (!uniqueRootMap.containsKey(key)) {
                uniqueRootMap.put(key, root);
            }
        }

        // Repopulate snapshot with unique GC roots.
        gcRoots.clear();
        uniqueRootMap.forEach(new TObjectProcedure<String>() {
            @Override
            public boolean execute(String key) {
                return gcRoots.add(uniqueRootMap.get(key));
            }
        });
    }

//    public File generateReport(File csvFile) {
//        if (extractorResultsMap == null || extractorResultsMap.isEmpty()) {
//            return null;
//        }
//        try {
//            CsvWriter csvWriter = new CsvWriter(csvFile.getAbsolutePath(), ',', Charset.forName("utf-8"));
//
//            for (Map.Entry<String, List<ObjectInfo>> entry : extractorResultsMap.entrySet()) {
//                String key = entry.getKey();
//                List<ObjectInfo> value = entry.getValue();
//                if (value == null) {
//                    continue;
//                }
//                if (value.isEmpty()) {
//                    continue;
//                }
//                csvWriter.writeRecord(new String[]{key});
//                ObjectInfo objectInfo = value.get(0);
//                csvWriter.writeRecord(("index," + objectInfo.getColumnsTitle()).split(","));
//                int i = 1;
//                for (ObjectInfo info : value) {
//                    csvWriter.writeRecord((i++ + "," + info.toDataString()).split(","));
//                }
//            }
//            csvWriter.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return csvFile;
//    }

//    public File generateMarkdownReport(File mdFile, int maxRowCount) {
//        if (extractorResultsMap == null || extractorResultsMap.isEmpty()) {
//            return null;
//        }
//        StringBuilder builder = new StringBuilder();
//        String endLine = "\n\n";
//        FileWriter fileWriter = null;
//        try {
//            fileWriter = new FileWriter(mdFile);
//            for (Map.Entry<String, List<ObjectInfo>> entry : extractorResultsMap.entrySet()) {
//                String title = entry.getKey();
//                if (extractorTitleMap.containsKey(entry.getKey())) {
//                    title = extractorTitleMap.get(entry.getKey());
//                }
//                builder.append(endLine).append("# ").append(title).append(endLine);
//                builder.append(getHtmlReport(entry.getKey(), maxRowCount, false));
//            }
//            fileWriter.write(builder.toString());
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                if (fileWriter != null) {
//                    fileWriter.flush();
//                    fileWriter.close();
//                }
//            } catch (IOException ex) {
//                ex.printStackTrace();
//            }
//        }
//
//        return mdFile;
//    }

//    public String getHtmlReport(String extractorName, int maxRowCount, boolean isLimit) {
//        if (extractorResultsMap == null || extractorResultsMap.isEmpty()) {
//            return null;
//        }
//        if (extractorName == null) {
//            return null;
//        }
//        StringBuilder stringBuilder = new StringBuilder();
//        String endLine = "<br>";
//        List<ObjectInfo> value = extractorResultsMap.get(extractorName);
//        if (value == null || value.isEmpty()) {
//            return null;
//        }
//        int i = 0;
//        for (ObjectInfo info : value) {
//            if (i >= maxRowCount) {
//                break;
//            }
//            StringBuilder nextLineBuilder = new StringBuilder();
//            nextLineBuilder.append("<li>").append("<b>").append("# ").append(i + 1).append(": ").append("</b>").append(endLine).append(info.toHtmlDataStringOCV()).append("</li>");
//            if (isLimit && (nextLineBuilder.length() + stringBuilder.length() > HTML_MAX_LENGTH)) {
//                break;
//            }
//            stringBuilder.append(nextLineBuilder);
//            i++;
//        }
//        stringBuilder.append("</ol>");
//        return stringBuilder.toString();
//    }

//    public String getHtmlReport(String extractorName, int maxRowCount) {
//        return getHtmlReport(extractorName, maxRowCount, true);
//    }

    private void processInstance(int index, Instance instance) {
        for (Map.Entry<String, Extractor> entry : extractorMap.entrySet()) {
            entry.getValue().onExtractInfo(index, instance);
        }
    }

    private static void findRelatedInstances(Instance instance, List<Instance> founds, Instance visited, List<String> fieldNames) {
        if (instance == null) {
            return;
        }
        if (visited != null) {
            if (instance instanceof ClassInstance) {
                ClassInstance classInstance = (ClassInstance) instance;
                List<ClassInstance.FieldValue> values = classInstance.getValues();
                for (ClassInstance.FieldValue value : values) {
                    if (Objects.equals(value.getField().getType(), Type.OBJECT)) {
                        if (Objects.equals(value.getValue(), visited)) {
                            founds.add(instance);
                            fieldNames.add(value.getField().getName());
                            break;
                        }
                    }
                }
            } else if (instance instanceof ClassObj) {
                Map<Field, Object> staticFieldValues = ((ClassObj) instance).getStaticFieldValues();
                for (Map.Entry<Field, Object> fieldObjectEntry : staticFieldValues.entrySet()) {
                    Field key = fieldObjectEntry.getKey();
                    if (Objects.equals(key.getType(), Type.OBJECT)) {
                        if (Objects.equals(staticFieldValues.get(key), visited)) {
                            founds.add(instance);
                            fieldNames.add(key.getName());
                            break;
                        }
                    }
                }
            } else if (instance instanceof ArrayInstance) {
                final ArrayInstance arrayInstance = (ArrayInstance) instance;
                int i = 0;
                for (Object object : arrayInstance.getValues()) {
                    if (Objects.equals(object, visited)) {
                        founds.add(instance);
                        fieldNames.add(i + "");
                        break;
                    }
                    i++;
                }
            }
        }
        findRelatedInstances(instance.getNextInstanceToGcRoot(), founds, instance, fieldNames);
    }

    private static List<FieldChain> getPathToGCRoot(Instance instance) {
        List<FieldChain> gcRoots = new ArrayList<>();
        List<Instance> founds = new ArrayList<>();
        List<String> fieldNames = new ArrayList<>();
        findRelatedInstances(instance, founds, null, fieldNames);
        for (int i = founds.size() - 1; i >= 0; i--) {
            Instance foundInstance = founds.get(i);
            String fieldName = fieldNames.get(i);
            ClassObj classObj = foundInstance.getClassObj();
            if (classObj == null) {
                if (!(foundInstance instanceof ClassObj)) {
                    continue;
                }
                classObj = (ClassObj) foundInstance;
            }
            String message = "";
            message += String.format("%s %s.%s",
                    getSizeStringWithTagIfLarge(foundInstance.getTotalRetainedSize(), ObjectInfo.SIZE_THRESHOLD),
                    classObj.getClassName(), fieldName);
            gcRoots.add(new FieldChain(message, foundInstance));

        }
        return gcRoots;
    }

    public static String generateFieldChainString(Instance instance, String lineEnding) {
        if (instance == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        List<FieldChain> gcRoots = getPathToGCRoot(instance);
        for (FieldChain fieldChain : gcRoots) {
            String className;
            if (fieldChain.instance.getClassObj() == null) {
                className = ((ClassObj) fieldChain.instance).getClassName();
            } else {
                className = fieldChain.instance.getClassObj().getClassName();
            }
            if (className != null && (className.startsWith("android.graphics.Bitmap") || className.startsWith("java.util.HashMap$Node"))) {
                continue;
            }
            if (className != null && className.contains(".launcher")) {
                builder.append("<b>").append("&#8625;&nbsp;").append(fieldChain.message).append("</b>").append(lineEnding);
            } else {
                builder.append("&#8625;&nbsp;").append(fieldChain.message).append(lineEnding);
            }
        }


        if (instance.getClassObj() != null) {
            builder.append(instance.getClassObj().getClassName()).append(" ");
        }

        List<FieldChain> fieldChains = generateDominatedFieldChain(instance, 0, new HashSet<Long>());
        if (!fieldChains.isEmpty()) {
            builder.append(lineEnding);

            for (FieldChain fieldChain : fieldChains) {
                String className = fieldChain.instance.getClassObj().getClassName();
                if (className != null && (className.startsWith("android.graphics.Bitmap") || className.startsWith("java.util.HashMap$Node"))) {
                    continue;
                }
                if (className != null && className.contains(".launcher")) {
                    builder.append("<b>").append("&#8627;&nbsp;").append(fieldChain.message).append("</b>").append(lineEnding);
                } else {
                    builder.append("&#8627;&nbsp;").append(fieldChain.message).append(lineEnding);
                }
            }
        }
        builder.append(lineEnding);
        return builder.toString();
    }

    private static List<FieldChain> generateDominatedFieldChain(Instance instance, int indent, Set<Long> chainInstanceSet) {
        final List<FieldChain> fieldChains = new ArrayList<>();
        if (indent > MAX_FIELD_CHAIN_DEPTH) {
            // Limit recursion depth
            return fieldChains;
        }

        if (instance instanceof ArrayInstance) {
            Instance maxRetainedSize = null;
            int maxIndex = -1;
            final ArrayInstance arrayInstance = (ArrayInstance) instance;
            int i = 0;
            for (Object object : arrayInstance.getValues()) {
                if (object == null) {
                    continue;
                }

                if (object instanceof Instance) {
                    Instance instance1 = (Instance) object;

                    if (chainInstanceSet.contains(instance1.getId())) {
                        continue;
                    }

                    if (maxRetainedSize == null
                            || instance1.getTotalRetainedSize() > maxRetainedSize.getTotalRetainedSize()) {
                        maxRetainedSize = instance1;
                        maxIndex = i;
                    }
                }
                ++i;
            }

            if (maxRetainedSize != null) {
                String message = "";
                message += String.format("%s %s%d",
                        getSizeStringWithTagIfLarge(instance.getTotalRetainedSize(), ObjectInfo.SIZE_THRESHOLD),
                        instance.getClassObj().getClassName(),
                        maxIndex);
                logger.debug(message);
                fieldChains.add(new FieldChain(message, instance));
                chainInstanceSet.add(maxRetainedSize.getId());
                fieldChains.addAll(generateDominatedFieldChain(maxRetainedSize, indent + 1, chainInstanceSet));
            }
        } else if (instance instanceof ClassInstance) {
            ClassInstance classInstance = (ClassInstance) instance;
            List<ClassInstance.FieldValue> fieldValues = classInstance.getValues();
            Instance maxRetainedSize = null;
            ClassInstance.FieldValue maxFieldValue = null;
            for (ClassInstance.FieldValue fieldValue : fieldValues) {
                Object value = fieldValue.getValue();
                if (value == null) {
                    continue;
                }

                if (value instanceof Instance) {
                    Instance instance1 = (Instance) value;

                    if (chainInstanceSet.contains(instance1.getId())) {
                        continue;
                    }

                    if (maxRetainedSize == null
                            || instance1.getTotalRetainedSize() > maxRetainedSize.getTotalRetainedSize()) {
                        maxRetainedSize = instance1;
                        maxFieldValue = fieldValue;
                    }
                } else {
                    logger.debug("ignore unknown field value type " + value.getClass().getName());
                }
            }

            if (maxRetainedSize != null) {
                String message = "";
                message += String.format("%s %s.%s",
                        getSizeStringWithTagIfLarge(instance.getTotalRetainedSize(), ObjectInfo.SIZE_THRESHOLD),
                        instance.getClassObj().getClassName(),
                        maxFieldValue.getField().getName());
                logger.debug(message);
                fieldChains.add(new FieldChain(message, instance));
                chainInstanceSet.add(maxRetainedSize.getId());
                fieldChains.addAll(generateDominatedFieldChain(maxRetainedSize, indent + 1, chainInstanceSet));
            }
        } else {
            logger.debug("ignore unknown instance type " + instance.getClass().getName());
        }

        return fieldChains;
    }

    private static class FieldChain {
        public final String message;
        public final Instance instance;

        FieldChain(String message, Instance instance) {
            this.message = message;
            this.instance = instance;
        }
    }
}
