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
          - name: php
            image: php:7.2.10-alpine
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
                script {

                    sh 'echo "Starting codewind-eclipse build..."'
                        
                    def sys_info = sh(script: "uname -a", returnStdout: true).trim()
                    println("System information: ${sys_info}")
                        
                      dir('dev') { sh './gradlew' }
                }
            }
        }        
    }    
}