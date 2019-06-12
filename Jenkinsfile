#!groovy​

pipeline {
    agent {
    
    kubernetes {        
      label 'codewind-agent-pod'
      yaml """
      apiVersion: v1
      kind: Pod
      spec:
      containers:
          - name: ubuntu
            image: eclipsecbi/ubuntu-gtk3-metacity:18.10-gtk3.24
            command:
            - cat
            tty: true
            resources:
              limits:
                memory: "2Gi"
                cpu: "1"
              requests:
                memory: "2Gi"
                cpu: "1"
        """
        }
    }
    
    options {
        timestamps() 
        skipStagesAfterUnstable()
    }
    
    stages {

        stage('Build') {
            steps {
                def javaHome
                javaHome = tool 'oracle-jdk8-latest'
                script {

                    sh 'echo "Starting codewind-eclipse build..."'
                    sh 'export JAVA_HOME=${javaHome}'
                    sh 'export PATH=$JAVA_HOME/bin:$PATH'
                        
                    def sys_info = sh(script: "uname -a", returnStdout: true).trim()
                    println("System information: ${sys_info}")
                        
                      dir('dev') { sh './gradlew' }
                }
            }
        }        
    }    
}