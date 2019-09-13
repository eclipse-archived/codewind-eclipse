# Codewind for Eclipse

[![License](https://img.shields.io/badge/License-EPL%202.0-red.svg?label=license&logo=eclipse)](https://www.eclipse.org/legal/epl-2.0/)
[![Build Status](https://ci.eclipse.org/codewind/buildStatus/icon?job=Codewind%2Fcodewind-eclipse%2Fmaster)](https://ci.eclipse.org/codewind/job/Codewind/job/codewind-eclipse/job/master/)
[![Chat](https://img.shields.io/static/v1.svg?label=chat&message=mattermost&color=145dbf)](https://mattermost.eclipse.org/eclipse/channels/eclipse-codewind)

## Installing Codewind for Eclipse
Prerequisites
- Download and install the latest [Eclipse IDE for Java EE Developers](https://www.eclipse.org/downloads/packages/release/) or use an existing installation. The earliest supported version of the Eclipse IDE for Codewind for Eclipse is 4.11.0 (2019-03).
- Install Docker.
- If you use Linux, you also need to install Docker Compose.

Complete the installation:
1. Install [Codewind from the Eclipse Marketplace](https://marketplace.eclipse.org/content/codewind).
2. Open the **Codewind Explorer** view.
3. Double-click the **Codewind** entry in the view to finish installing Codewind. The download is approximately 1 GB. For more information, see [Installing Codewind for Eclipse](https://www.eclipse.org/codewind/mdteclipseinstallinfo.html).

## Using Codewind for Eclipse
Right-click the **Local Projects** entry in the view to create new projects or add existing projects to Codewind. After a project is created or added, it displays in the **Codewind Explorer** view. Right-click the project to see the available actions.

Features:</br>
- Create new projects from application templates or add existing container-ready projects to Codewind.
- View Codewind projects, including application and build statuses.
- Debug **Microprofile/Java EE** and **Spring** projects in their containers.
- Set up a Chrome debug session for **Node.js** projects.
- View application and build logs in the **Console** view.
- View and edit project deployment information.
- Open a shell session into a Codewind application container.
- Toggle project auto build and manually initiate project builds.
- Integrate Codewind validation errors into the **Markers** view.
- Disable, enable, and remove projects.

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
2. [Optional] To get a test build, copy the `codewind-eclipse` folder to the `build` directory to keep your source folder intact.
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
- The extension bundles dependency executables. These are gitignored, but should be kept up-to-date on your local system with the same versions used in the `Jenkinsfile` `parameters` section. Run `dev/org.eclipse.codewind.core/binaries/pull.sh` to download the dependencies. Also see `dev/org.eclipse.codewind.core/binaries/README.txt`.
- Open the **Git Repositories** view in Eclipse and click on the **Add an existing local Git Repository to this view** toolbar button.
- Fill in the location of your repository clone and finish the wizard.
- Right click on the repository you just created, select **Import Projects** and import all of the `org.eclipse.codewind.*` projects.
- Modify the code as desired and then use the Eclipse self-hosting feature to test and debug your changes:
    1. Click on **Run** > **Debug Configurations** and create a new debug launch configuration of type **Eclipse Application**.
    2. Modify the workspace location if desired.
    3. For **Program to Run** choose **Run a product** and select **org.eclipse.epp.package.jee.product** from the drop down list.
    4. You can work with different versions of the Codewind images by adding the **CW_TAG** environment variable in the **Environment** tab. Set it to **latest** to get the latest images or to a specific tag such as **0.3**.
    5. Click the **Debug** button when you are finished to launch the self-hosted Eclipse and begin your testing.
    
## Developing Codewind images
- Make sure to use the **Codewind Explorer** view in Eclipse to uninstall any current Codewind images before starting. If Codewind is installed, right click on **Codewind** in the view and select **Uninstall**.
- To work with the Codewind images, clone this repository to your system:
 ```
 git clone https://github.com/eclipse/codewind
 ```
- Use an editor of your choice to make changes to the code if desired.
- Build and run Codewind using [`run.sh`](https://github.com/eclipse/codewind/blob/master/run.sh).
- When it is running you can right click on **Codewind** in the **Codewind Explorer** view in Eclipse and select **Refresh** to pick up the newly running Codewind images.

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
