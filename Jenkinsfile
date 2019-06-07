#!groovy​

pipeline {
	agent any
	
    options {
        skipStagesAfterUnstable()
    }
    
	stages {
		stage('Build') {
			steps {
				script {
					sh 'echo "Starting codewind-eclipse build..."'
				
					if (isUnix()) {
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