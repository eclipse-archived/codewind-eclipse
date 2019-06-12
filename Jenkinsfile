#!groovy​

pipeline {
    agent any
    
    tools {
        maven 'apache-maven-latest'
        jdk 'oracle-jdk8-latest'
        gradle 'gradle-latest'
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
                        gradle -version
                        which gradle
                        mvn -version
                    '''
                    
                     dir('dev') { sh './gradlew --stacktrace' }
                }
            }
        }        
    }    
}