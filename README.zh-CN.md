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

Hydra Lab 使用 [Azure Blob 存储](https://azure.microsoft.com/en-us/products/storage/blobs/) 作为云文件存储解决方案，以持久化存储日志文件、视频、应用包等。请访问你的 Azure 门户，打开一个 Azure Blob 存储账户，获取 [connection string](https://learn.microsoft.com/en-us/azure/storage/common/storage-configure-connection-string) 。
并将其放入环境变量中，名称为 BLOB_CONNECTION_STR。

Hydra Lab 提供了一个名为 Uber 开箱即用的 docker 镜像。在简单地配置环境变量 BLOB_CONNECTION_STR 后，你可以按照下面的步骤，启动内置了一个 center 实例和一个 agent 实例的 docker 容器：

**第1步. 从容器注册中心获取 Docker 镜像**
> docker pull ghcr.io/microsoft/hydra-lab-uber:latest

**第2步. 在你的机器上运行，并使用 BLOB_CONNECTION_STR 作为参数**
> docker run [-p 9886:9886] [--name=hydra-lab] -e BLOB_CONNECTION_STR=${BLOB_CONNECTION_STR} ghcr.io/microsoft/hydra-lab-uber:latest

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
### For Hydra Lab User:

- [Trigger a test task run in the Hydra Lab test service](https://github.com/microsoft/HydraLab/wiki/Trigger-a-test-task-run-in-the-Hydra-Lab-test-service)
- [Deploy a test agent service](https://github.com/microsoft/HydraLab/wiki/Deploy-a-test-agent-service)
- [Create an Appium UI Test Automation Project](https://github.com/microsoft/HydraLab/wiki/Create-an-Appium-UI-Test-Automation-Project)

> Note: If you are a Microsoft FTE and want to onboard to the internal Hydra Lab testing service, please visit [our SharePoint site](https://microsoftapc.sharepoint.com/teams/MMXDocument/SitePages/Hydra-Lab-test-automation-service-onboarding-guideline.aspx) to learn more about the internal service instance.

<span id="for-contributor"></span>
### 参与贡献Hydra Lab:

- [Contribute to the Hydra Lab GitHub Project](https://github.com/microsoft/HydraLab/wiki/Contribute-to-the-Hydra-Lab-GitHub-Project)

<span id="who-use-it"></span>
## 谁在使用Hydra Lab?

It's already powering the UI test automation of the following Microsoft products:
- Microsoft Phone Link (Windows UWP app) and Link to Windows (Android app)
- Microsoft Launcher (Android)
- Microsoft Outlook/Edge (Android/iOS)
- Microsoft Fluent UI Android/Yammer Android

<span id="contribute"></span>
## Contribute

Your contribution to Hydra Lab will make a difference for the entire test automation ecosystem. Please refer to **[CONTRIBUTING.md](CONTRIBUTING.md)** for contribution instructions.

### Contributor Hero Wall:

<a href="https://github.com/Microsoft/hydralab/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=Microsoft/hydralab" />
</a>

<span id="contact"></span>
## Contact Us

Feel free to dive in! If you have questions about Hydra Lab, or you would like to reach out to us about an issue you're having, you can reach us as follows:
- [Open an issue](https://github.com/microsoft/HydraLab/issues/new) or submit PRs.
- Email us: [hydra_lab_support@microsoft.com](mailto:hydra_lab_support@microsoft.com).

<span id="links"></span>
## Links

- [Hydra Lab Release Notes](https://github.com/microsoft/HydraLab/wiki/Release-Notes)
- [Secure a Java web app using the Spring Boot Starter for Azure Active Directory.](https://docs.microsoft.com/en-us/azure/developer/java/spring-framework/configure-spring-boot-starter-java-app-with-azure-active-directory) 
- [Appium: Cross-platform automation framework for all kinds of your apps built on top of W3C WebDriver protocol.](https://github.com/appium/appium)
- [Google Android Tools Ddmlib: A ddmlib jar that provides APIs for talking with Dalvik VM.](https://android.googlesource.com/platform/tools/base/+/master/ddmlib/)

<span id="ms-give"></span>
## Microsoft Give Sponsors

Thank you for your contribution to [Microsoft employee giving program](https://aka.ms/msgive) in the name of Hydra Lab:

[@Germey(崔庆才)](https://github.com/Germey), [@SpongeOnline(王创)](https://github.com/SpongeOnline), [@ellie-mac(陈佳佩)](https://github.com/ellie-mac), [@Yawn(刘俊钦)](https://github.com/Aqinqin48), [@White(刘子凡)](https://github.com/jkfhklh), [@597(姜志鹏)](https://github.com/JZP1996)

![Microsoft Give](docs/images/Give_WebBanner.png)

<span id="license-trademarks"></span>
## License & Trademarks

The entire codebase is under [MIT license](https://github.com/microsoft/HydraLab/blob/main/LICENSE).

This project may contain trademarks or logos for projects, products, or services. Authorized use of Microsoft trademarks or logos is subject to and must follow [Microsoft’s Trademark & Brand Guidelines](https://www.microsoft.com/en-us/legal/intellectualproperty/trademarks/usage/general). Use of Microsoft trademarks or logos in modified versions of this project must not cause confusion or imply Microsoft sponsorship. Any use of third-party trademarks or logos are subject to those third-party’s policies.

