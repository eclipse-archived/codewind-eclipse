[![License](https://img.shields.io/badge/License-EPL%202.0-red.svg?label=license&logo=eclipse)](https://www.eclipse.org/legal/epl-2.0/)
[![Build Status](https://ci.eclipse.org/codewind/buildStatus/icon?job=Codewind%2Fcodewind-eclipse%2Fmaster)](https://ci.eclipse.org/codewind/job/Codewind/job/codewind-eclipse/job/master/)
[![Chat](https://img.shields.io/static/v1.svg?label=chat&message=mattermost&color=145dbf)](https://mattermost.eclipse.org/eclipse/channels/eclipse-codewind)

# Codewind for Eclipse
Create and develop cloud-native, containerized web applications from Eclipse.

## Installing Codewind for Eclipse
You can install Codewind locally in Eclipse. For more information about installing Codewind, see [Getting started: Codewind for Eclipse](https://www.eclipse.org/codewind/mdteclipsegettingstarted.html).

Prerequisites
- Download and install the latest [Eclipse IDE for Java EE Developers](https://www.eclipse.org/downloads/packages/release/) or use an existing installation.
    - Install Eclipse IDE Version 2019-09 R (4.13.0) or later to avoid [Bug 541220](https://bugs.eclipse.org/bugs/show_bug.cgi?id=541220).
    - However, the earliest supported version of the Eclipse IDE is Version 2019-03 (4.11).
- Install Docker 17.06 or later.
- If you use Linux, you also need to install Docker Compose.

Complete the installation:
1. Install [Codewind from the Eclipse Marketplace](https://marketplace.eclipse.org/content/codewind).
2. Open the **Codewind Explorer** view. Go to **Window**>**Show View**>**Other…**>**Codewind**>**Codewind Explorer**.
3. Double-click the **Local** entry in the view to finish installing Codewind. The download is approximately 1 GB. For more information, see [Installing Codewind for Eclipse](https://www.eclipse.org/codewind/mdt-eclipse-installinfo.html).

## Using Codewind for Eclipse
Right-click the **Local** entry in the view to create new projects or add existing projects to Codewind. After a project is created or added, it displays in the **Codewind Explorer** view. Right-click the project to see the available actions.

[Features:](https://www.eclipse.org/codewind/mdteclipsemanagingprojects.html)</br>
- **Open Application**: Opens the application in the default Eclipse browser. This action is only available when the application is running or debugging.
- **Open Project Overview**: Opens the overview page for a project. You can use this action to see information about the project and edit project settings.
- **Open Container Shell**: Opens a shell into your application container. This action is available only when the container is active.
- **Open Application Monitor**: Opens the application monitor in the default Eclipse browser. Use this action to monitor the activity and health of your application. This action is available only when the application is running or debugging.
- **Open Performance Dashboard**: Opens the performance dashboard in the default Eclipse browser. This action is available only when the application is running or debugging.
- **Import Project**: Imports your project into the Eclipse workspace.
- **Disable/Enable Project**: Disables or enables the project.
- **Show Log Files**: If log files are available, this action displays a list of log files. Click **Show All** or an individual log file toggle action to open the log file in the Eclipse **Console** view. Click the log file again to remove the log file, or click **Hide All** to remove all log files from the **Console** view.
- **Restart in Run Mode**: Restarts the application in run mode.
- **Restart in Debug Mode**: Restarts the application in debug mode and attaches the debugger. Only MicroProfile/Java EE, Spring, and Node.js projects can be debugged. For more information, see [Debugging Codewind projects](https://www.eclipse.org/codewind/mdteclipsedebugproject.html).
- **Attach Debugger**: If you detached the debugger accidentally or restarted Eclipse, use this action to re-attach the debugger to an application in debug mode. For more information, see [Debugging Codewind projects](https://www.eclipse.org/codewind/mdteclipsedebugproject.html).
- **Build**: Initiate a build of your project. This action is not available if a build is already running. For more information, see [Building Codewind projects](https://www.eclipse.org/codewind/mdteclipsebuildproject.html).
- **Disable Auto Build**: Use this to disable automatic builds if you are making a lot of changes and don't want builds to be triggered until you are done. This action is available only when auto build is enabled.
- **Enable Auto Build**: Use this to re-enable automatic builds whenever a change is made. This action is available only when auto build is disabled.
- **Remove**: Removes a project. This action removes the project from Codewind and optionally deletes the project files from the file system.
- **Refresh**: If the project gets out of sync, use this option to refresh it. To refresh all projects, right-click the **Local** item in the **Codewind Explorer** view and select **Refresh**.

## Enabling debug logs
1. Create an `.options` file in your Eclipse install directory, the same directory with the `eclipse` executable. Include the following content in the new file:
```
org.eclipse.codewind.core/debug/info=true
```
2. Launch Eclipse with the `-debug` flag.
3. The logs are written to the Eclipse workspace directory to `.metadata/.log`.

## Building
1. Clone the repository to your system:
 ```
 git clone https://github.com/eclipse/codewind-eclipse
 ```
2. (Optional:) To get a test build, copy the `codewind-eclipse` folder to the `build` directory to keep your source folder intact.
3. Run a Gradle build:
```
cd build/dev
./gradlew
```
4. Test the driver built from the Gradle build:
```
build/dev/ant_build/artifacts/codewind-[Version].vYYYYMMDD_hhmm.zip
```

## Developing Codewind for Eclipse
- Download and install the latest [Eclipse IDE for Java EE Developers](https://www.eclipse.org/downloads/packages/release/) or use an existing installation. The earliest supported version of the Eclipse IDE for Codewind for Eclipse is 4.11.0 (2019-03).
- Clone the repository to your system:
 ```
 git clone https://github.com/eclipse/codewind-eclipse
 ```
- From the `codewind-eclipse/dev` directory run `gradlew getDependencies` to download the dependency jars.
- The extension bundles dependency executables. These are gitignored, but should be kept up-to-date on your local system with the same versions used in the `Jenkinsfile` `parameters` section. Run `dev/org.eclipse.codewind.core/binaries/meta-pull.sh` to pull down the required scripts from the codewind-vscode repository, then run `dev/org.eclipse.codewind.core/binaries/pull.sh` to download the dependencies. Also see `dev/org.eclipse.codewind.core/binaries/README.txt`, and the downloaded `dev/org.eclipse.codewind.core/binaries/README-pull.txt`.
- Open the **Git Repositories** view in Eclipse and click on the **Add an existing local Git Repository to this view** toolbar button.
- Fill in the location of your repository clone and finish the wizard.
- Right-click on the repository you just created, select **Import Projects**, and import all of the `org.eclipse.codewind.*` projects.
- Modify the code and then use the Eclipse self-hosting feature to test and debug your changes:
    1. Click **Run** > **Debug Configurations** and create a new debug launch configuration of type **Eclipse Application**.
    2. Modify the workspace location if you want.
    3. For **Program to Run**, choose **Run a product** and select **org.eclipse.epp.package.jee.product** from the drop down list.
    4. You can work with different versions of the Codewind images by adding the **CW_TAG** environment variable in the **Environment** tab. Set it to **latest** to get the latest images or to a specific tag such as **0.3**.
    5. To launch the self-hosted Eclipse and begin your testing, click the **Debug** button when you are finished.

## Running Tests on Codewind for Eclipse
- The tests are located in the `org.eclipse.codewind.test` project and are JUnit tests.
- Tests can be run individually or there are two test lists:
    - **AllTests**: This includes all of the tests.
    - **BuildVerificationTests**: This is the subset of tests that are run with the build.
- To run the tests create a new run configuration in Eclipse of type **JUnit Plug-in Test**:
    - Set **Test runner** to `JUnit 4`. Do this first otherwise the **Search** button for the test class will not work.
    - Set **Project** to `org.eclipse.codewind.test`.
    - Set **Test class** to the test case or test list you want to run.
    - (Optional) Un-check **Run in UI thread**. This allows you to interact with the Eclipse instance running the test.
    - Click the **Run** button.
    
## Developing Codewind images
- Make sure to use the **Codewind Explorer** view in Eclipse to uninstall any current Codewind images before starting. If Codewind is installed, right click on **Codewind** in the view and select **Uninstall**.
- To work with the Codewind images, clone this repository to your system:
 ```
 git clone https://github.com/eclipse/codewind
 ```
- Use an editor of your choice to make changes to the code.
- Build and run Codewind using [`run.sh`](https://github.com/eclipse/codewind/blob/master/run.sh).
- When it is running you can right-click **Codewind** in the **Codewind Explorer** view in Eclipse and select **Refresh** to pick up the newly running Codewind images.

## Dependencies
| Dependency | License |
| ---------- | ------- |
| [socket.io-client-1.0.0.jar](https://mvnrepository.com/artifact/io.socket/socket.io-client/1.0.0) | [MIT](http://opensource.org/licenses/mit-license) |
| [engine.io-client-1.0.0.jar](https://mvnrepository.com/artifact/io.socket/engine.io-client/1.0.0) | [MIT](https://opensource.org/licenses/mit-license) |
| [org.json_1.0.0.v201011060100.jar](http://download.eclipse.org/tools/orbit/downloads/drops/R20181102183712/repository/plugins/org.json_1.0.0.v201011060100.jar) | [EPL-1.0](https://www.eclipse.org/legal/epl-v10.html) |
| [okhttp-3.8.1.jar](https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp/3.8.1) | [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| [okio-1.13.0.jar](https://mvnrepository.com/artifact/com.squareup.okio/okio/1.13.0) | [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |

## Contributing
To contribute to Codewind, see [CONTRIBUTING.md](https://github.com/eclipse/codewind-eclipse/tree/master/CONTRIBUTING.md).
