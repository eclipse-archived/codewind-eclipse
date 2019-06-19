# Codewind for Eclipse

[![License](https://img.shields.io/badge/License-EPL%202.0-red.svg)](https://www.eclipse.org/legal/epl-2.0/)

## Installing Codewind for Eclipse
Prerequisites
- Download and install the latest [Eclipse IDE for Java EE Developers](https://www.eclipse.org/downloads/packages/release/) or use an existing installation. The earliest supported version of the Eclipse IDE for Codewind for Eclipse is 4.11.0 (2019-03).
- Install Docker.
- If you use Linux, you also need to install Docker Compose.

Complete the installation:
1. Install [Codewind from the Eclipse Marketplace](https://marketplace.eclipse.org/content/codewind).
2. Open the **Codewind Explorer** view.
3. Double-click the **Codewind** entry in the view to finish installing Codewind. The download is approximately 1 GB.

## Using Codewind for Eclipse
Right-click the **Local Projects** entry in the view to create new projects or add existing projects to Codewind. After a project is created or added, it displays in the **Codewind Explorer** view. Right-click the project to see the available actions.

Features:</br>
- Create new projects from application templates or add existing Docker-ready projects to Codewind.
- View Codewind projects, including application and build statuses.
- Debug **Microprofile/Java EE** and **Spring** projects in their Docker containers.
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
