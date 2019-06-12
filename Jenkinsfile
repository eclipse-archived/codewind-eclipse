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

					sh 'echo "Starting codewind-eclipse build..."'
						
					def sys_info = sh(script: "uname -a", returnStdout: true).trim()
                    println("System information: ${sys_info}")
                    	
                  	dir('dev') { sh './gradlew' }
		    	}
			}
		}		
	}	
}