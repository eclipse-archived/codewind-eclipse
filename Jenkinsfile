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
                    
                    sh '''
                        java -version
                        which java
                    '''
                        
                     dir('dev') { sh './gradlew' }
                }
            }
        }        
    }    
}