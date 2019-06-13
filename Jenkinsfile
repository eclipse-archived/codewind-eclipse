#!groovy​

pipeline {
    agent any
    
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
                        ls -la ${JAVA_HOME}
                        java -version
                        which java    
                    '''
                    println("JAVE_HOME: ${JAVA_HOME}")
                    dir('dev') { sh './gradlew --stacktrace' }
                }
            }
        }        
    }    
}