# Contributing to Hydra Lab

We truly welcome any constructive feedback and community contributions to Hydra Lab.
Any interest in making testing better with Hydra Lab is much appreciated!

## Reporting issues and suggesting new features

If Hydra Lab is not working properly, please [create a GitHub Issues](https://github.com/microsoft/HydraLab/issues/new). 
We are happy to hear your ideas for the future of Hydra Lab. Check the [GitHub Issues](https://github.com/microsoft/HydraLab/issues) and see if others have submitted similar issue. You can upvote existing issue or submit a new suggestion.
We will regularly look at upvoted items in [issues](https://github.com/microsoft/HydraLab/issues) when we decide what to work on next, and we look forward to hearing your input. Remember that all community interactions must abide by the [Code of Conduct](https://github.com/microsoft/Hydra-Lab/blob/main/CODE_OF_CONDUCT.md).

## Making changes to the code

### Preparing your development environment

We recommend using [IntelliJ IDEA](https://www.jetbrains.com/idea/) as the Java/SpringBoot project IDE, and community edition would suffice. 
![img.png](docs/images/guide_screenshot_project_structure.png)

Select correto-11 JDK in project structure and click apply.  
Add-ons including JPA Buddy and PlantUML Integration are strongly recommended.  

For code design we leverage the [PlantUML](https://plantuml.com/) as UML tools. Therefore, you can install the [PlantUML Integration](https://plugins.jetbrains.com/plugin/7017-plantuml-integration) plugin to preview the design diagrams.
For instance, you can view agent side code design in folder: [agent/doc/UML](agent/doc/UML).

And for the front-end React powered Web UI, the code lives in [react](react) folder, you may take [Visual Studio Code](https://code.visualstudio.com/) as editor. Please refer to the [react/README.md](react/README.md) to understand the setup procedures.

To learn how to build the code and run tests, follow the instructions in the [README](https://github.com/microsoft/HydraLab/README.md).

### Git workflow

We use the [GitHub flow](https://guides.github.com/introduction/flow/) where most
development happens directly on the `main` branch. The `main` branch should always be in a
healthy state which is ready for release.

If your change is complex, please clean up the branch history before submitting a pull request.
You can use [git rebase](https://docs.microsoft.com/en-us/azure/devops/repos/git/rebase#squash-local-commits)
to group your changes into a small number of commits which we can review one at a time.

When completing a pull request, we will generally squash your changes into a single commit. Please
let us know if your pull request needs to be merged as separate commits.

## Coding style conventions

Under most cases, new file/folder name should follow the [snake case](https://en.wikipedia.org/wiki/Snake_case) style patterns. A fully capitalized name should come with a reasonable justification.

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
