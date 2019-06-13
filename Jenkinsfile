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
            - name: fedora
              image: eclipsecbi/ufedora-gtk3-mutter:29-gtk3.24
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
    
    tools {
        maven 'apache-maven-latest'
        jdk 'oracle-jdk8-latest'
    }
    
    options {
        timestamps() 
        skipStagesAfterUnstable()
    }
    
    stages {

        stage('Build') {
            steps {
                script {

                    println("Starting codewind-eclipse build...")
                        
                    def sys_info = sh(script: "uname -a", returnStdout: true).trim()
                    println("System information: ${sys_info}")
                    println("JAVE_HOME: ${JAVA_HOME}")                   
                    sh '''
                        java -version
                        which java    
                        mvn -v
                    '''
                    dir('dev') { sh './gradlew --stacktrace' }
                }
            }
        }        
    }    
}