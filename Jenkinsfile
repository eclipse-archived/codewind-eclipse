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
				
					if (isUnix()) {
						def sys_info = sh(script: "uname -a", returnStdout: true).trim()
                    	println("System information: ${sys_info}")
                    	
                    	def sys_info_lsb = sh(script: "lsb_release -a", returnStdout: true).trim()
                    	println("System information lsb: ${sys_info_lsb}")
					
                        dir('dev') { sh './gradlew' }
		    		} 
		    		else {
		        		dir('dev') { bat 'gradlew.bat' }
		    		}
		    	}
			}
		}		
	}	
}