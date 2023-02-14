# HydraLabClient
This is the Gradle plugin of Hydra Lab.
In order to simplify the onboarding procedure to Hydra Lab for any app, this project packaged the client util and made it an easy way for any app to leverage the cloud testing service of Hydra Lab.


## Usage
To trigger Hydra Lab testing using Gradle command, simply follow below steps:
- Step 1: go to [template](https://github.com/microsoft/HydraLab/tree/main/gradle_plugin/template) page, selectively leverage the following files to your repo and modify the content:
    - To introduce dependency on this plugin, please copy according content in [build.gradle](https://github.com/microsoft/HydraLab/tree/main/gradle_plugin/template/build.gradle) to your project/module.
      - See [release notes](https://github.com/microsoft/HydraLab/wiki/Release-Notes) for version info and version number.
      - Update **${plugin_version}** with your selected version.
    - According to your project structure, apply one or combination of the following configuration approaches to configure the input parameters of gradle plugin task (see detailed explanation for parameters in [gradle.properties](https://github.com/microsoft/HydraLab/tree/main/gradle_plugin/template/gradle.properties))
      - **Parameter priority: inline command > gradle.properties > yaml**
      - Inline gradle command, set parameters with "-Pxxx=yyy".
        - Sample: **gradle [:${MODULE_NAME}:]requestHydraLabTest -PappPath="${PATH_TO_APP}" -PtestAppPath=...**
      - [gradle.properties](https://github.com/microsoft/HydraLab/tree/main/gradle_plugin/template/gradle.properties)
        - Usage: 
          - Fill in the file, keep only the parameters needed for your test, and remove the redundant ones.
          - Keep this file in the same directory as your build.gradle, gradle task will read this file automatically.
      - [testSpec.yml](https://github.com/microsoft/HydraLab/tree/main/gradle_plugin/template/testSpec.yml)
        - Usage:
          - Fill in the file, keep only the parameters needed for your test, and remove the redundant ones.
          - Specific the yml file path by inline command "-PymlConfigFile=${PATH_TO_YML}" following the gradle task command.
        - Sample: **gradle [:${MODULE_NAME}:]requestHydraLabTest -PymlConfigFile=${PATH_TO_YML} ...**
- Step 2: Build your project/module to enable the gradle plugin and task
- Step 3: Run gradle task requestHydraLabTest

## Known issue
- Hard-coded with Azure DevOps embedded variable names, currently may not be compatible to other CI tools when fetching commit related information.
