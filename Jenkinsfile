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
                script {

                    println("Starting codewind-eclipse build...")
                        
                    def sys_info = sh(script: "uname -a", returnStdout: true).trim()
                    println("System information: ${sys_info}")
                    
                    sh '''
                        java -version
                    '''
                        
                     dir('dev') { sh './gradlew' }
                }
            }
        }        
    }    
}