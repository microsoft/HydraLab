# HydraLabClient
This is the Gradle plugin of Hydra Lab.
In order to simplify the onboarding procedure to Hydra Lab for any app, this project packaged the client util and made it an easy way for any app to leverage the cloud testing service of Hydra Lab.

## Prerequisite
### TODO

## Usage
To trigger gradle task for Hydra Lab testing, simply follow below steps:
- Step 1: go to [template](link to template) page, copy the following files to your repo and modify the content:
    - [build.gradle](link to template/build.gradle)
        - To introduce dependency on this plugin, please copy all content to repository/module you would like to use the plugin in.
    - [gradle.properties](link to template/gradle.properties)
        - According to the comment inline and the running type you choose for your test, you should keep all required parameters and fill in them with correct values.
- Step 2: Build your project/module to enable the Gradle plugin and task
- Step 3: Run gradle task requestHydraLabTest
    - Use gradle command to trigger the task.
    - Override any value in gradle.properties by specify command param "-PXXX=xxx".
    - Example command: **gradle requestHydraLabTest -PappApkPath="D:\Test Folder\app.apk"**

## Known issue
- Hard-coded with Azure DevOps embedded variable names, currently may not be compatible to other CI tools when fetching commit related information.
