package com.microsoft.hydralab.common.entity.common;

import lombok.Data;

@Data
public class AppComponent {
    private String name;
    private String type;

    public AppComponent(String name) {
        this.name = name;
        this.type = Type.UI;
    }
    public AppComponent(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public interface Type {
        String UI = "ui";
        String SERVICE = "service";
        String DATASOURCE = "datasource";
    }
}
