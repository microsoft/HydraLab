# HydraLabClient
This is the Gradle plugin of Hydra Lab.
In order to simplify the onboarding procedure to Hydra Lab for any app, this project packaged the client util and made it an easy way for any app to leverage the cloud testing service of Hydra Lab.

## Prerequisite
Include Hydra Lab plugin dependency in build.gradle of your project:
- Using the plugins DSL:
```
plugins {
  id "com.microsoft.hydralab.client-util" version "${plugin_version}"
}
```
- Using legacy plugin application:
```
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "com.microsoft.hydralab:gradle_plugin:${plugin_version}"
  }
}

apply plugin: "com.microsoft.hydralab.client-util"
```
See [Release Notes](https://github.com/microsoft/HydraLab/wiki/Release-Notes) for latest and stable versions.

## Usage
To trigger gradle task for Hydra Lab testing, simply follow below steps:
- Step 1: go to [template](https://github.com/microsoft/HydraLab/tree/main/gradle_plugin/src/main/resources/template) page, copy the following files to your repo and modify the content:
    - [build.gradle](https://github.com/microsoft/HydraLab/blob/main/gradle_plugin/src/main/resources/template/build.gradle)
        - To introduce dependency on this plugin, please copy all content to repository/module you would like to use the plugin in.
    - [gradle.properties](https://github.com/microsoft/HydraLab/blob/main/gradle_plugin/src/main/resources/template/gradle.properties)
        - According to the comment inline and the running type you choose for your test, you should keep all required parameters and fill in them with correct values.
- Step 2: Build your project/module to enable the Gradle plugin and task
- Step 3: Run gradle task requestHydraLabTest
    - Use gradle command to trigger the task.
    - Override any value in gradle.properties by specify command param "-PXXX=xxx".
    - Example command: **gradle requestHydraLabTest -PappApkPath="D:\Test Folder\app.apk"**

## Known issue
- Hard-coded with Azure DevOps embedded variable names, currently may not be compatible to other CI tools when fetching commit related information.

## TODO
**- Add yml configuration file for task param setup.**
