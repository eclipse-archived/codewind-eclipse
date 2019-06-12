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
                        ls -la ${JAVA_HOME}
                        export JAVA_HOME=${JAVA_HOME}/jre
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