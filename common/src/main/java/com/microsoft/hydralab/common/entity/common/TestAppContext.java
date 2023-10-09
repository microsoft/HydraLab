package com.microsoft.hydralab.common.entity.common;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TestAppContext {
    private String packageName;
    private long packageSize;
    private String appName;
    private String appVersion;
    private List<AppComponent> appComponents = new ArrayList<>();
}
