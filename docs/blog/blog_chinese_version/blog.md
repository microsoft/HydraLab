# Hydra Lab ———为微软移动端产品线智能测试赋能

[Hydra Lab: 让智能云测试更简单](https://github.com/microsoft/HydraLab)

## 什么是 Hydra Lab?

下面的视频介绍了 Hydra Lab 的功能以及定位。

https://github.com/microsoft/HydraLab/assets/8344245/cefefe24-4e11-4cc7-a3af-70cb44974735

通过 Hydra Lab，用户可以构建一个私有的测试云平台，它可以基于 RESTful 接口快速集成 DevOps 系统，换句话说，就像部署了一个可管理的私有“Firebase Test Lab”。

## 我们为什么需要 Hydra Lab?

Hydra Lab 是一个开源的项目，它于 2022 年 12 月在 [github.com/microsoft](https://github.com/microsoft) 下发布。作为一个开源项目，它很好地确保了可控性，提供了定制化的能力，消除了在集成云测试平台可能遇到的以下问题：

- **可信度**：将调试应用程序包上传到第三方平台是否安全？我们能否完全信任第三方平台？使用第三方平台是否有合规性要求？
- **定制性**：第三方平台是否有与测试兼容的真实设备？是否支持跨平台场景？
- **成本**：这些平台的定价是否合理？
- **设备共享**：能否将我们的设备接入云端，并在不同地区，不同团队之间共享？

Hydra Lab 是一个基于 Appium, Espresso, Maestro, XCTest 等框架打造的一个免费的、可定制的、可信赖的框架。由于它具备集成不同测试框架的胶水代码（适配器）、可复用可扩展的设计以及软件测试生命周期的定义，所以可以支持新的测试框架和智能测试。我们始终致力于“让智能云测试更简单”，“打造个人的智能测试云”的目标，并在 Azure DevOps 中建立了标准化的 CI/CD 流程，借助单元测试、接口测试、跨平台用户验收测试等环节为产品质量把关，迄今为止已经发布了 27 个稳定版本。

## 在微软 Hydra Lab 如何为测试赋能？

在过去的两年中，我们已经将 Hydra Lab 集成到软件开发流程中，并通过搭建的内部平台为各个移动产品提供无感的自动化测试服务，例如 Phone Link, Link to Windows for Android and iOS, Office Union for Android, Teams Android 等产品。

在微软，我们基于这个框架搭建了一个 Hydra Lab 服务中心，它的协作模式以及架构如下图：

![架构图](1692864197387.jpg)

我们将 Hydra Lab 中心端的 docker 镜像部署在 Azure 上作为中心服务，它向内部用户提供了基于 AAD OAuth 的 RESTful API，这些用户可以使用 Azure DevOps Pipeline 来创建一个 Espresso 或 Appium 类型的测试任务，并推送至中心服务。在收到测试请求后，中心服务会选择合适的测试代理服务器和设备来运行测试任务。测试代理服务器会选择对应的测试框架运行任务，并在测试完成后将结果返回给中心服务。最终，用户可以在中心服务的网页上查看这些测试报告。

用户可以参考 [测试代理服务器部署手册](https://github.com/microsoft/HydraLab/wiki/Test-agent-setup) 配置测试服务器，并注册到 Hydra Lab 中心服务。

**名词定义：**

![名词解释](1692864513033.jpg)

**测试流程图：**

![Alt text](1692864702416.jpg)

Hydra Lab 通过 [Hydra Lab Azure DevOps 插件](https://marketplace.visualstudio.com/items?itemName=MaXESteam.hydra-lab-alter) 为无缝集成提供支持。这是 DevOps 流程中的关键组成部分，使整个测试流程完全自动化。同时我们也提供了 [Gradle 插件](https://github.com/microsoft/HydraLab/wiki/Trigger-a-test-task-run-in-the-Hydra-Lab-test-service)的集成模式，为更多的安卓开发者提供支持。

为了确保代理服务器，测试设备出异常能被及时发现，我们还基于 Prometheus + Grafana 搭建了一套监控告警系统。数据流向如下图：

![Alt text](1692864918378.jpg)

## 开始使用 Hydra Lab

如果用户想快速尝试并了解 Hydra Lab ，我们提供了一键部署的 docker 镜像，只要机器上安装了 docker 和 ADB，输入以下命令：

```
docker run -p 9886:9886 ghcr.io/microsoft/hydra-lab-uber:latest
```

然后就可以打开 **http://localhost:9886/portal** ，并开始使用 Hydra Lab 的基本功能：测试设备管理，测试任务管理，使用各种测试驱动，查看测试报告，测试视频等。

关于更多的使用细节，请参考 GitHub 文档：[如何部署 Hydra Lab 中心服务](https://github.com/microsoft/HydraLab/wiki/Deploy-Center-Docker-Container)。

## 基于 GPT/LLM 的智能测试

**如果有一百万只猴子在一百万个键盘上随机敲一百万年，就可以写出一部莎士比亚的著作。**

![DALL-E 2](1692865230943.jpg)

现阶段，我们的目标是将大型语言模型（LLMs）集成到我们的平台中，提高我们在测试结果分析、探索性测试和测试用例生成方面的能力。在软件测试的历史上，Monkey Test（在屏幕或 IO 设备上发起随机操作，模仿猴子的偶然行为）一直是一个广受欢迎的方法，来评估应用程序的可靠性。Monkey Test的优势在于它的简单性，即不需要维护测试用例，就可能在初始测试阶段发现问题。然而，它局限于随机操作的粗糙性。想象一下，如果我们能够引入一个更智能的 “Monkey”，它可以真正理解应用程序并像人类一样与之交互，这种测试方法是否可以得到改进？

![Alt text](1692865540488.jpg)

于是我们引入了 SEE（启动-提取-评估）探索模型，它的运行方式如下：首先，启动应用程序并配置环境。然后，确定并理解交互元素，将它们的描述转换为向量或其他计算机可处理的格式。利用这些处理过的数据，制定决策，执行操作，并评估结果。随后，回到提取阶段，获取新的状态。这个循环过程与马尔可夫奖励过程有着相似之处。

![Alt text](1692865572379.jpg)

探索之后，我们可以基于这些结果数据生成测试用例。生成黑盒测试用例的难点在于黑盒中庞大上下文，一般的模型很难分析如此庞大的数据量。因此，我们尝试将其转换为机器学习模型，或大型语言模型（LLM）可以处理的结构化数据，并在该方向上持续探索。

![Alt text](1692865645266.jpg)

## 开发实践

我们的核心贡献团队使用 [HydraLab/CONTRIBUTING.md](https://github.com/microsoft/HydraLab/blob/main/CONTRIBUTING.md) 作为开发规范。一般情况下，我们使用 PlantUML 来描述 Hydra Lab 的架构、绘制开发设计图、定义组件和类之间的关系。这样我们就可以轻松地审查一个功能的设计，并在 PR 中提供建议。

我们鼓励在每次新的代码变更中添加单元测试，并且我们也在 CI 中搭建了 BVT，在 CD 中进行了基于接口调用的用户验收测试。下图是我们用到的部分技术组件，如果您有兴趣参与开发，我们随时欢迎您加入团队。

![Alt text](1692866390077.jpg)

![Alt text](1692865803974.jpg)