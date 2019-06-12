#!groovy​

pipeline {
    agent any
    
    tools {
        maven 'apache-maven-latest'
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
                        ls -la /opt/java
                        java -version
                        which java
                    '''
                    
                     dir('dev') { sh './gradlew --stacktrace' }
                }
            }
        }        
    }    
}