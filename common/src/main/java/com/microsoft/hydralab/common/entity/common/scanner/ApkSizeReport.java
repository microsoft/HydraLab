package com.microsoft.hydralab.common.entity.common.scanner;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ApkSizeReport {
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
    ;

    public static class DuplicatedFile {
        public String md5;
        public long size;
        public List<String> fileList;
    }

    public static class FileItem {
        public long size;
        public String fileName;
    }
}
