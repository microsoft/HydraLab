package com.microsoft.hydralab.center.openai;

import com.alibaba.fastjson.JSON;
import com.microsoft.hydralab.center.openai.data.ChatMessage;
import com.microsoft.hydralab.center.openai.data.ChatRequest;
import com.microsoft.hydralab.center.openai.data.ExceptionSuggestion;
import com.microsoft.hydralab.center.openai.data.PerformanceSuggestion;
import com.microsoft.hydralab.center.openai.data.SimplifiedPerformanceData;
import com.microsoft.hydralab.center.openai.data.SimplifiedPerformanceDataSet;
import com.microsoft.hydralab.center.openai.data.SimplifiedPerformanceResult;
import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.file.StorageServiceClientProxy;
import com.microsoft.hydralab.common.repository.StorageFileInfoRepository;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import com.microsoft.hydralab.performance.PerformanceInspectionResult;
import com.microsoft.hydralab.performance.PerformanceTestResult;
import com.microsoft.hydralab.performance.entity.AndroidBatteryInfo;
import com.microsoft.hydralab.performance.entity.AndroidMemoryInfo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.microsoft.hydralab.center.util.CenterConstant.CENTER_TEMP_FILE_DIR;

@Service
public class SuggestionService {
    @Resource
    StorageFileInfoRepository storageFileInfoRepository;
    @Resource
    StorageServiceClientProxy storageServiceClientProxy;
    private final AzureOpenAIServiceClient oaiClient;

    public SuggestionService() {
        this.oaiClient = new AzureOpenAIServiceClient("", "", "");
    }

    public PerformanceSuggestion performanceAnalyze(TestRun testRun) {
        PerformanceSuggestion suggestion = new PerformanceSuggestion();
        List<PerformanceTestResult> performanceResults = getPerformanceResult(testRun);
        if (performanceResults == null) {
            return suggestion;
        }
        String perfsString = convertPerformanceTestToJsonString(performanceResults);
        String perfSuggestion = getOpenaiPerformanceSuggestion(perfsString);
        suggestion.setContent(perfSuggestion);
        return suggestion;
    }

    public ExceptionSuggestion exceptionAnalyze(TestRun testRun) {
        ExceptionSuggestion suggestion = new ExceptionSuggestion();
        return suggestion;
    }

    private List<PerformanceTestResult> getPerformanceResult(TestRun testRun) {
        List<PerformanceTestResult> performanceTestResult = null;

        String fileId = "";
        for (StorageFileInfo f : testRun.getAttachments()) {
            if (f.getFileName().contains(Const.PerformanceConfig.DEFAULT_FILE_NAME)) {
                fileId = f.getFileId();
                break;
            }
        }
        if (fileId.isEmpty()) {
            return null;
        }

        StorageFileInfo perfBlobFile = storageFileInfoRepository.findById(fileId).orElse(null);
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
            performanceTestResult = JSON.parseArray(jsonString, PerformanceTestResult.class);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
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

                List<SimplifiedPerformanceData> dataArr = new ArrayList<>();
                SimplifiedPerformanceData d = new SimplifiedPerformanceData();
                if (ins.parsedData instanceof AndroidMemoryInfo) {
                    AndroidMemoryInfo info = (AndroidMemoryInfo)ins.parsedData;
                    insertPerformanceData(dataArr, "CodePss", info.getCodePss());
                    insertPerformanceData(dataArr, "CodeRss", info.getCodeRss());
                    insertPerformanceData(dataArr, "GraphicsPss", info.getGraphicsPss());
                    insertPerformanceData(dataArr, "GraphicsRss", info.getGraphicsRss());
                    insertPerformanceData(dataArr, "StackPss", info.getStackPss());
                    insertPerformanceData(dataArr, "StackRss", info.getStackRss());
                    insertPerformanceData(dataArr, "HeapPss", info.getJavaHeapPss());
                    insertPerformanceData(dataArr, "HeapRss", info.getJavaHeapRss());
                    insertPerformanceData(dataArr, "SystemPss", info.getSystemPss());
                    insertPerformanceData(dataArr, "SystemRss", info.getSystemRss());
                } else if (ins.parsedData instanceof AndroidBatteryInfo) {
                    AndroidBatteryInfo info = (AndroidBatteryInfo)ins.parsedData;
                    insertPerformanceData(dataArr, "Cpu", info.getCpu());
                    insertPerformanceData(dataArr, "Ratio", info.getRatio());
                    insertPerformanceData(dataArr, "AppUsage", info.getAppUsage());
                    insertPerformanceData(dataArr, "WakeLock", info.getWakeLock());
                } else {
                    continue;
                }
                ds.setDataSet(dataArr);
                ds.setTimestamp(ins.timestamp);
            }

            sResult.setType(result.inspectorType.toString());
            sResult.setDataset(dataSetArr);
            sResults.add(sResult);
        }
        return JSON.toJSONString(sResults);
    }

    private void insertPerformanceData(List<SimplifiedPerformanceData> dataArr, String name, long data) {
        if (data > 0) {
            insertPerformanceData(dataArr, name, Long.toString(data));
        }
    }

    private void insertPerformanceData(List<SimplifiedPerformanceData> dataArr, String name, float data) {
        if (data > 0) {
            insertPerformanceData(dataArr, name, Float.toString(data));
        }
    }

    private void insertPerformanceData(List<SimplifiedPerformanceData> dataArr, String name, String data) {
        SimplifiedPerformanceData d = new SimplifiedPerformanceData();
        d.setName(name);
        d.setValue(data);
        dataArr.add(d);
    }

    private String getOpenaiPerformanceSuggestion(String perfSuggestiong) {
        List<ChatMessage> msgs = new ArrayList<>();
        ChatMessage msg = new ChatMessage("user", "");
        msgs.add(msg);

        ChatRequest req = new ChatRequest();
        req.setMessages(msgs);
        return oaiClient.chatCompletion(req);
    }
}
