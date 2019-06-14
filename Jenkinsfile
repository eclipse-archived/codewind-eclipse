#!groovy​

pipeline {
    agent any
    
    tools {
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
                    '''
     
                    dir('dev') { sh './gradlew --stacktrace' }
 
 					
                }
            }
        } 
        
        stage('Deploy') {
          steps {
            sshagent ( ['projects-storage.eclipse.org-bot-ssh']) {
              println("Deploying codewind-eclipse to downoad area...")
              sh '''
              		ssh genie.codewind@projects-storage.eclipse.org rm -rf /home/data/httpd/download.eclipse.org/codewind/codewind-eclipse/snapshots
           			  ssh genie.codewind@projects-storage.eclipse.org mkdir -p /home/data/httpd/download.eclipse.org/codewind/codewind-eclipse/snapshots
         			    scp -r /home/jenkins/workspace/ewind-eclipse_enableJenkinsBuild/dev/ant_build/artifacts/* genie.codewind@projects-storage.eclipse.org:/home/data/httpd/download.eclipse.org/codewind/codewind-eclipse/snapshots
              '''
            }
          }
        }       
    }    
}