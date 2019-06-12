#!groovy​

pipeline {
    agent any
    
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
                        export JAVA_HOME=/opt/java/openjdk
                    '''
                    
                    println("JAVE_HOME: ${JAVA_HOME}")
                    
                     dir('dev') { sh './gradlew' }
                }
            }
        }        
    }    
}