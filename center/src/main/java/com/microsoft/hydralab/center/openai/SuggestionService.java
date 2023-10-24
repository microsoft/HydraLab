package com.microsoft.hydralab.center.openai;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.center.openai.data.ExceptionSuggestion;
import com.microsoft.hydralab.center.openai.data.SimplifiedPerformanceDataSet;
import com.microsoft.hydralab.center.openai.data.SimplifiedPerformanceResult;
import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.file.StorageServiceClientProxy;
import com.microsoft.hydralab.common.file.impl.AzureOpenaiConfig;
import com.microsoft.hydralab.common.repository.StorageFileInfoRepository;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import com.microsoft.hydralab.performance.PerformanceInspectionResult;
import com.microsoft.hydralab.performance.PerformanceResultParser;
import com.microsoft.hydralab.performance.PerformanceTestResult;
import com.microsoft.hydralab.performance.entity.AndroidBatteryInfo;
import com.microsoft.hydralab.performance.entity.AndroidHprofMemoryInfo;
import com.microsoft.hydralab.performance.entity.AndroidMemoryInfo;
import com.microsoft.hydralab.performance.entity.IOSEnergyGaugeInfo;
import com.microsoft.hydralab.performance.entity.IOSMemoryPerfInfo;
import com.microsoft.hydralab.performance.entity.WindowsBatteryParsedData;
import com.microsoft.hydralab.performance.entity.WindowsMemoryParsedData;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import static com.microsoft.hydralab.center.util.CenterConstant.CENTER_TEMP_FILE_DIR;

@Service
public class SuggestionService {
    @Resource
    StorageFileInfoRepository storageFileInfoRepository;
    @Resource
    StorageServiceClientProxy storageServiceClientProxy;
    private final AzureOpenaiConfig openaiConfig;
    private AzureOpenAIServiceClient oaiClient = null;
    private final Map<PerformanceResultParser.PerformanceResultParserType, Class> performanceTypeMap = Map.ofEntries(
        Map.entry(PerformanceResultParser.PerformanceResultParserType.PARSER_ANDROID_BATTERY_INFO, AndroidBatteryInfo.class),
        Map.entry(PerformanceResultParser.PerformanceResultParserType.PARSER_WIN_MEMORY, WindowsMemoryParsedData.class),
        Map.entry(PerformanceResultParser.PerformanceResultParserType.PARSER_WIN_BATTERY, WindowsBatteryParsedData.class),
        Map.entry(PerformanceResultParser.PerformanceResultParserType.PARSER_ANDROID_MEMORY_INFO, AndroidMemoryInfo.class),
        Map.entry(PerformanceResultParser.PerformanceResultParserType.PARSER_ANDROID_MEMORY_DUMP, AndroidHprofMemoryInfo.class),
        Map.entry(PerformanceResultParser.PerformanceResultParserType.PARSER_IOS_ENERGY, IOSEnergyGaugeInfo.class),
        Map.entry(PerformanceResultParser.PerformanceResultParserType.PARSER_IOS_MEMORY, IOSMemoryPerfInfo.class)
    );

    public SuggestionService(ApplicationContext applicationContext) {
        this.openaiConfig = applicationContext.getBean(Const.AzureOpenaiConfig.AZURE_OPENAI_CONFIG, AzureOpenaiConfig.class);
        if (openaiConfig.getApiKey() != null && openaiConfig.getDeployment() != null && openaiConfig.getEndpoint() != null) {
            this.oaiClient = new AzureOpenAIServiceClient(
                    openaiConfig.getApiKey(),
                    openaiConfig.getDeployment(),
                    openaiConfig.getEndpoint());
        }
    }

    public void performanceAnalyze(TestRun testRun) {
        List<PerformanceTestResult> performanceResults = getPerformanceResult(testRun);
        if (performanceResults == null) {
            return;
        }
        String perfsString = convertPerformanceTestToJsonString(performanceResults);
        String perfSuggestion = getOpenaiPerformanceSuggestion(perfsString);
        if (perfSuggestion != null) {
            testRun.setSuggestion(perfSuggestion);
        }
    }

    public ExceptionSuggestion exceptionAnalyze(TestRun testRun) {
        ExceptionSuggestion suggestion = new ExceptionSuggestion();
        return suggestion;
    }

    @SuppressWarnings("unchecked")
    private List<PerformanceTestResult> getPerformanceResult(TestRun testRun) {
        List<PerformanceTestResult> performanceTestResult = null;

        String fileId = "";
        StorageFileInfo perfBlobFile = null;
        for (StorageFileInfo f : testRun.getAttachments()) {
            if (f.getFileName().contains(Const.PerformanceConfig.DEFAULT_FILE_NAME)) {
                fileId = f.getFileId();
                perfBlobFile = f;
                break;
            }
        }
        if (fileId.isEmpty()) {
            return null;
        }

        if (perfBlobFile == null) {
            throw new HydraLabRuntimeException("Graph zip file not exist!");
        }
        File perfFile = new File(CENTER_TEMP_FILE_DIR, perfBlobFile.getBlobPath());
        if (!perfFile.exists()) {
            storageServiceClientProxy.download(perfFile, perfBlobFile);
        }
        if (!perfFile.exists()) {
            return null;
        }

        try (FileReader reader = new FileReader(perfFile)) {
            BufferedReader br = new BufferedReader(reader);
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            String jsonString = sb.toString();

            List<PerformanceTestResult> results = JSON.parseArray(jsonString, PerformanceTestResult.class);
            JSONArray ja = JSON.parseArray(jsonString);
            for (int i = 0; i < ja.size(); i++) {
                PerformanceTestResult result = results.get(i);
                Class classType = this.performanceTypeMap.get(result.parserType);
                JSONObject jo = (JSONObject)ja.get(i);
                List<Object> inspects = new ArrayList<Object>();
                Object performanceInspectionResults = jo.get("performanceInspectionResults");
                if (performanceInspectionResults instanceof List<?>) {
                    inspects = (List<Object>)performanceInspectionResults;
                }
                for (int j = 0; j < inspects.size(); j++) {
                    JSONObject inspect = (JSONObject)inspects.get(j);
                    if (inspect == null) {
                        continue;
                    }
                    JSONObject parsedData = (JSONObject)inspect.get("parsedData");
                    if (parsedData == null) {
                        continue;
                    }
                    result.performanceInspectionResults.get(j).parsedData = parsedData.toJavaObject(classType);
                }
            }
            performanceTestResult = results;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return performanceTestResult;
    }

    private String convertPerformanceTestToJsonString(List<PerformanceTestResult> results) {
        List<SimplifiedPerformanceResult> sResults = new ArrayList<>();
        for (PerformanceTestResult result : results) {
            SimplifiedPerformanceResult sResult = new SimplifiedPerformanceResult();
            List<SimplifiedPerformanceDataSet> dataSetArr = new ArrayList<>();
            for (PerformanceInspectionResult ins : result.performanceInspectionResults) {
                SimplifiedPerformanceDataSet ds = new SimplifiedPerformanceDataSet();
                if (ins.parsedData instanceof AndroidMemoryInfo) {
                    AndroidMemoryInfo info = (AndroidMemoryInfo)ins.parsedData;
                    insertPerformanceData(ds, "CodePss", info.getCodePss());
                    insertPerformanceData(ds, "CodeRss", info.getCodeRss());
                    insertPerformanceData(ds, "GraphicsPss", info.getGraphicsPss());
                    insertPerformanceData(ds, "GraphicsRss", info.getGraphicsRss());
                    insertPerformanceData(ds, "StackPss", info.getStackPss());
                    insertPerformanceData(ds, "StackRss", info.getStackRss());
                    insertPerformanceData(ds, "HeapPss", info.getJavaHeapPss());
                    insertPerformanceData(ds, "HeapRss", info.getJavaHeapRss());
                    insertPerformanceData(ds, "SystemPss", info.getSystemPss());
                    insertPerformanceData(ds, "SystemRss", info.getSystemRss());
                } else if (ins.parsedData instanceof AndroidBatteryInfo) {
                    AndroidBatteryInfo info = (AndroidBatteryInfo)ins.parsedData;
                    insertPerformanceData(ds, "Cpu", info.getCpu());
                    insertPerformanceData(ds, "Ratio", info.getRatio());
                    insertPerformanceData(ds, "AppUsage", info.getAppUsage());
                    insertPerformanceData(ds, "WakeLock", info.getWakeLock());
                } else {
                    continue;
                }
                ds.setTimestamp(ins.timestamp);
                dataSetArr.add(ds);
            }

            sResult.setType(result.inspectorType.toString());
            sResult.setDataset(dataSetArr);
            sResults.add(sResult);
        }
        return JSON.toJSONString(sResults);
    }

    private void insertPerformanceData(SimplifiedPerformanceDataSet ds, String name, Object data) {
        if (ds.getInspect() == null) {
            ds.setInspect(new HashMap<String, Object>());
        }
        ds.getInspect().put(name, data);
    }

    private String getOpenaiPerformanceSuggestion(String perfs) {
        if (this.oaiClient != null) {
            String prompt = "Here is a a json-based performance data. It is Automation Test for a mobile app. Analyze these data information and provide a general conclusion.";
            prompt += "\n\n```";
            prompt += perfs;
            prompt += "\n```";
            return oaiClient.completion(prompt);
        }
        return null;
    }
}
