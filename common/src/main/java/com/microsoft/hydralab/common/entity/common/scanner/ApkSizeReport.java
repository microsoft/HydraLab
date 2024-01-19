package com.microsoft.hydralab.common.entity.common.scanner;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import javax.persistence.AttributeConverter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class ApkSizeReport implements Serializable {
    private long totalSize;
    private float totalSizeInMB;
    private long dexSize;
    private long arscSize;
    private long soSize;
    private long pngSize;
    private long xmlSize;
    private long webpSize;
    private long otherSize;
    private long downloadSize;
    private float downloadSizeInMB;
    public List<FileItem> unusedAssetsList = new ArrayList<>();
    public List<DuplicatedFile> duplicatedFileList = new ArrayList<>();
    public List<FileItem> bigSizeFileList = new ArrayList<>();

    public static class DuplicatedFile implements Serializable {
        public String md5;
        public long size;
        public List<String> fileList;
    }

    public static class FileItem implements Serializable {
        public long size;
        public String fileName;
    }

    public static class Converter implements AttributeConverter<ApkSizeReport, String> {
        @Override
        public String convertToDatabaseColumn(ApkSizeReport attribute) {
            return JSONObject.toJSONString(attribute);
        }

        @Override
        public ApkSizeReport convertToEntityAttribute(String dbData) {
            return JSONObject.parseObject(dbData, ApkSizeReport.class);
        }
    }
}
