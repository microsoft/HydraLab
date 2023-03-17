<h1 align="center">Hydra Lab</h1>
<p align="center">搭建属于你的智能移动云测平台</p>
<div align="center">

[![Build Status](https://dlwteam.visualstudio.com/Next/_apis/build/status/HydraLab-CI?branchName=main)](https://dlwteam.visualstudio.com/Next/_build/latest?definitionId=743&branchName=main)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-v2.2.5-blue)
![Appium](https://img.shields.io/badge/Appium-v8.0.0-yellow)
![License](https://img.shields.io/badge/license-MIT-green)
![visitors](https://visitor-badge.glitch.me/badge?page_id=microsoft.hydralab&left_color=gray&right_color=red)
</div>

---

![HydraLabFeaturesPreview](docs/images/HydraLabFeaturesPreview.gif)

[What is Hydra Lab?](#what-is) | [Get Started](#get-started) | [Who are using Hydra Lab?](#who-use-it) | [Contribute](#contribute) | [Contact Us](#contact) | [Links](#links) | [Wiki](https://github.com/microsoft/HydraLab/wiki)

<span id="what-is"></span>
## Hydra Lab 是什么？

Hydra Lab 是一个基于 Spring Boot & React 构建的服务框架，帮你快速构建一套集测试运行部署、测试设备管理、低代码测试等功能于一身的跨平台云测服务，开箱即用。
它使开发团队能够快速建立一个可自我管理的智能云测试基础设施。在 Hydra Lab 的帮助下，你可以：

- 搭建：创建一个新的云测试网络。
- 加入：以最小的代价将你的测试设备部署到现有的网络上。

Hydra Lab 的特性包括：
- center-agent 分布式设计下的可扩展测试设备管理；测试任务管理和测试结果可视化。
- 支持 [Android Espresso Test](https://developer.android.com/training/testing/espresso)。
- 支持在不同平台上进行 Appium(Java) 测试：Windows/iOS/Android/浏览器/跨平台。
- 无用例的自动化测试：Monkey test，智能探索测试

更多细节，请参见 [什么是 Hydra Lab？](https://github.com/microsoft/HydraLab/wiki)

<span id="get-started"></span>
## 入门

请访问我们的 **[GitHub 项目 Wiki](https://github.com/microsoft/HydraLab/wiki)** 以了解开发环境的配置流程： [贡献指南](https://github.com/microsoft/HydraLab/wiki/Contribute-to-the-Hydra-Lab-GitHub-Project)

**Hydra Lab agent 支持的环境**：Windows, Mac OSX, 和Linux ([Docker](https://github.com/microsoft/HydraLab/blob/main/agent/README.md#run-agent-in-docker)).

**支持的平台和框架**:

|  | Appium(Java) | Espresso | 
| ---- |--------------|---- |
|Android| &#10004;     | &#10004; |
|iOS| &#10004;     | x | x |
|Windows| &#10004;     | x | 
|Web (浏览器)| &#10004;     | x | 

<span id="quick-start"></span>
### 开箱即用的 Uber docker 镜像快速指南

Hydra Lab 提供了一个名为 Uber 开箱即用的 docker 镜像。在简单地配置环境变量 BLOB_CONNECTION_STR 后，你可以按照下面的步骤，启动内置了一个 center 实例和一个 agent 实例的 docker 容器：

**第1步. 从容器注册中心获取 Docker 镜像**

```bash
docker pull ghcr.io/microsoft/hydra-lab-uber:latest
```

**第2步. 在你的机器上运行**

只需选择以下其中一个命令，即可开始您在 Hydra Lab 的体验：

**1. 使用本地存储服务**

Hydra Lab 默认使用本地文件系统作为存储，不需要额外的环境变量：

```bash
docker run -p 9886:9886 --name=hydra-lab ghcr.io/microsoft/hydra-lab-uber:latest
```

**2. 使用第三方存储服务**

Hydra Lab 目前支持 [Azure Blob 存储](https://azure.microsoft.com/en-us/products/storage/blobs/) 作为云文件存储解决方案，以持久化存储多种文件类型，例如日志文件、视频、应用程序包等。欢迎为集成其他第三方存储服务做出贡献。

根据存储服务的类型，你需要在命令中额外指定一些环境变量。

如果你想要使用 Azure Blob 存储，请访问你的 Azure 门户，创建一个 Azure Blob 存储账户，并获取 [connection string](https://learn.microsoft.com/en-us/azure/storage/common/storage-configure-connection-string)。简要步骤：[登录 Azure](https://azure.microsoft.com/) -> [Portal](https://portal.azure.com/#home) -> [Storage Accounts](https://portal.azure.com/#view/HubsExtension/BrowseResource/resourceType/Microsoft.Storage%2FStorageAccounts) -> 创建新的存储账户（你可以限制容器的公共读写） -> 在创建的存储账户中, 找到 `Access Keys` 页面 -> 复制 `Connection string`。
![image](https://user-images.githubusercontent.com/8344245/216729523-387dc162-54d8-41dd-b136-f2e3c780b10a.png)

你可以将以下内容写入一个配置文件（例如 env.properties）:
```
STORAGE_TYPE=AZURE
BLOB_CONNECTION_STR=${YOUR_BLOB_CONNECTION_STR}
```

然后将文件路径传给 docker container：

```bash
docker run --env-file env.properties -p 9886:9886 --name=hydra-lab ghcr.io/microsoft/hydra-lab-uber:latest
```

或者设置环境参数 -e 直接运行：

```bash
docker run -e STORAGE_TYPE=AZURE -e BLOB_CONNECTION_STR=${YOUR_BLOB_CONNECTION_STR} -p 9886:9886 --name=hydra-lab ghcr.io/microsoft/hydra-lab-uber:latest
```

**第3步. 访问前端页面并查看你的已连接设备**

> Url: http://localhost:9886/portal/index.html#/ (或自定义的端口号).

开始享受你的探索之旅吧!

**注意：Uber 现在只提供安卓系统的Espresso测试功能，更多的功能请参考本节：[对于 Hydra Lab 用户](#for-user)** 

### 构建和运行的快速指南

你也可以用以下命令单独运行中心 java Spring Boot 服务（一个可运行的 Jar）：

> 构建和运行过程需要用到 JDK11+ | NPM | Android SDK 平台工具。

**第1步. 构建并运行 Hydra Lab center 服务。**

```bash
# 在项目根目录，切换到 react 文件夹来构建 Web 前端文件。
cd react
npm ci
npm run pub
# 回到项目根目录，构建 center 可运行的 Jar 。
cd ..
# 对于 gradlew 命令，如果你使用的是 Windows 系统，请用`./gradlew`或`./gradlew.bat`替换。
gradlew :center:bootJar
# 运行并访问 http://localhost:9886/portal/index.html#/
java -jar center/build/libs/center.jar
# 然后访问 http://localhost:9886/portal/index.html#/auth 来生成新的 agent ID 和 agent secret 。
```

> 如果你遇到了以下错误: `Error: error:0308010C:digital envelope routines::unsupported`, 设置环境变量 `NODE_OPTIONS` 的值为 `--openssl-legacy-provider` 并重启命令行。

**第2步. 构建并运行 Hydra Lab agent 服务。**

```bash
# 在项目根目录下，复制示例配置文件并更新：
# YOUR_AGENT_NAME, YOUR_REGISTERED_AGENT_ID 和 YOUR_REGISTERED_AGENT_SECRET 。
cp agent/application-sample.yml application.yml
# 然后构建 agent jar 并运行它
gradlew :agent:bootJar
java -jar agent/build/libs/agent.jar
```

**第3步. 访问 http://localhost:9886/portal/index.html#/ 并查看你的已连接设备**

**Technical design overview:**

![Tech Architecture](docs/images/technical_architecture.png)

<span id="for-user"></span>
### 对于Hydra Lab的使用者:

- [在Hydra Lab测试服务中触发一次测试任务](https://github.com/microsoft/HydraLab/wiki/Trigger-a-test-task-run-in-the-Hydra-Lab-test-service)
- [部署测试代理服务](https://github.com/microsoft/HydraLab/wiki/Deploy-a-test-agent-service)
- [创建Appium UI测试自动化项目](https://github.com/microsoft/HydraLab/wiki/Create-an-Appium-UI-Test-Automation-Project)

> 注意：如果您是Microsoft FTE并希望加入内部Hydra Lab测试服务，请访问 [我们的SharePoint网站](https://microsoftapc.sharepoint.com/teams/MMXDocument/SitePages/Hydra-Lab-test-automation-service-onboarding-guideline.aspx) 以了解有关内部服务实例的详细信息。

<span id="for-contributor"></span>
### 参与贡献Hydra Lab:

- [如何参与并为Hydra Lab GitHub项目做出贡献？](CONTRIBUTING.md)

<span id="who-use-it"></span>
## 谁在使用Hydra Lab?

它已经支持以下Microsoft产品的UI测试自动化：
- Microsoft Phone Link（Windows UWP应用程序）和 Link to Windows（Android应用程序）
- Microsoft Launcher (Android)
- Microsoft Outlook/Edge (Android/iOS)
- Microsoft Fluent UI Android/Yammer Android

<span id="contribute"></span>
## 贡献

您对Hydra Lab的贡献将为整个测试自动化生态系统带来改变。请参阅贡献指引 **[CONTRIBUTING.md](CONTRIBUTING.md)** 。
### 贡献者英雄榜：

<a href="https://github.com/Microsoft/hydralab/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=Microsoft/hydralab" />
</a>

<span id="contact"></span>
## 联系我们

如果您对Hydra Lab有任何疑问，您可以通过以下方式联系我们：
- [创建 issue](https://github.com/microsoft/HydraLab/issues/new) 或提交 PRs.
- 发送邮件到: [hydra_lab_support@microsoft.com](mailto:hydra_lab_support@microsoft.com).

<span id="links"></span>
## 链接

- [Secure a Java web app using the Spring Boot Starter for Azure Active Directory.](https://docs.microsoft.com/en-us/azure/developer/java/spring-framework/configure-spring-boot-starter-java-app-with-azure-active-directory) 
- [Appium: Cross-platform automation framework for all kinds of your apps built on top of W3C WebDriver protocol.](https://github.com/appium/appium)
- [Google Android Tools Ddmlib: A ddmlib jar that provides APIs for talking with Dalvik VM.](https://android.googlesource.com/platform/tools/base/+/master/ddmlib/)

<span id="ms-give"></span>
## 微软Give员工捐赠活动

感谢您以Hydra Lab的名义为 [微软员工捐赠计划](https://aka.ms/msgive) 做出的贡献：

[@Germey(崔庆才)](https://github.com/Germey), [@SpongeOnline(王创)](https://github.com/SpongeOnline), [@ellie-mac(陈佳佩)](https://github.com/ellie-mac), [@Yawn(刘俊钦)](https://github.com/Aqinqin48), [@White(刘子凡)](https://github.com/jkfhklh), [@597(姜志鹏)](https://github.com/JZP1996)

![Microsoft Give](docs/images/Give_WebBanner.png)

<span id="license-trademarks"></span>
## 许可证和商标

整个代码库遵循 [MIT许可协议](https://github.com/microsoft/HydraLab/blob/main/LICENSE)。

该项目可能包含一些项目、产品或服务的商标或标识。使用 Microsoft 商标或标识需遵循 [Microsoft的商标和品牌准则](https://www.microsoft.com/en-us/legal/intellectualproperty/trademarks/usage/general)并经授权。在此项目的修改版本中使用 Microsoft 商标或标识不得混淆或暗示 Microsoft 的赞助。任何使用第三方商标或标识的行为均需遵守相关第三方政策。

