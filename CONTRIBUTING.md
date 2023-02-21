# Contributing to Hydra Lab

We truly welcome any constructive feedback and community contributions to Hydra Lab.
Any interest in making testing better with Hydra Lab is much appreciated!

## Reporting issues and suggesting new features

If Hydra Lab is not working properly, please [create a GitHub Issues](https://github.com/microsoft/HydraLab/issues/new). 
We are happy to hear your ideas for the future of Hydra Lab. Check the [GitHub Issues](https://github.com/microsoft/HydraLab/issues) and see if others have submitted similar issue. You can upvote existing issue or submit a new suggestion.
We will regularly look at upvoted items in [issues](https://github.com/microsoft/HydraLab/issues) when we decide what to work on next, and we look forward to hearing your input. Remember that all community interactions must abide by the [Code of Conduct](https://github.com/microsoft/Hydra-Lab/blob/main/CODE_OF_CONDUCT.md).

## Making changes to the code

Please turn to the wiki to set up your dev environment: [Dev Environment Setup](https://github.com/microsoft/HydraLab/wiki/Dev-Environment-Setup)

### Git workflow

We use the [GitHub flow](https://guides.github.com/introduction/flow/) where most
development happens directly on the `main` branch. The `main` branch should always be in a
healthy state which is ready for release.

If your change is complex, please clean up the branch history before submitting a pull request.
You can use [git rebase](https://docs.microsoft.com/en-us/azure/devops/repos/git/rebase#squash-local-commits)
to group your changes into a small number of commits which we can review one at a time.

When completing a pull request, we will generally squash your changes into a single commit. Please
let us know if your pull request needs to be merged as separate commits.

### Technical design practice

We use the [PlantUML](https://github.com/plantuml/plantuml) generation tool to provide planning solution and clarity for the architecture design of Hydra Lab, and most of the designs are documented in `*.puml` files, and running the following [gradle task]([build.gradle](https://github.com/microsoft/HydraLab/blob/bc1471a9f1385664adb9bc26a204670e24917751/build.gradle#L94)) will generate the UML images in folder [docs/images/UML](docs/images/UML):

```bash
gradlew generateUMLImage
```

## Coding style conventions

Under most cases, new file/folder name should follow the [snake case](https://en.wikipedia.org/wiki/Snake_case) style patterns. A fully capitalized name should come with a reasonable justification.

## Handling exceptions in code

We are aligned on the [Fail Fast](https://www.techtarget.com/whatis/definition/fail-fast) principles for the project, and will avoid any inappropriate practice that will bury an exception in long log or ignoed code comment, please refer to [Java-Exception-handling-best-practices](https://www.theserverside.com/blog/Coffee-Talk-Java-News-Stories-and-Opinions/Java-Exception-handling-best-practices)|[中文版](https://xie.infoq.cn/article/e1acf36fa0655c321f673c230) to learn about it first.

And please leverage the `Assert` API to save if conditions, for example: [Spring Assert API](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/util/Assert.html)

## Review Process

After submitting a pull request, members of the Hydra Lab team will review your code. We will assign the request to an appropriate reviewer. Any member of the community may participate in the review, but at least one member of the Hydra Lab team will ultimately approve the request.
Try looking at [past pull requests](https://github.com/microsoft/HydraLab/pulls?q=is%3Apr+is%3Aclosed) to see what the experience might be like.

## Contributor License Agreement

This project welcomes contributions and suggestions.  Most contributions require you to agree to a
Contributor License Agreement (CLA) declaring that you have the right to, and actually do, grant us
the rights to use your contribution. For details, visit https://cla.opensource.microsoft.com.

When you submit a pull request, a CLA bot will automatically determine whether you need to provide
a CLA and decorate the PR appropriately (e.g., status check, comment). Simply follow the instructions
provided by the bot. You will only need to do this once across all repos using our CLA.
