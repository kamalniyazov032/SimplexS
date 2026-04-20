pipeline {
    agent any
    
    tools {
        jdk 'JDK-21'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                bat 'mvn clean package -DskipTests'
            }
        }

        stage('Deploy Jar') {
            steps {
                bat 'copy /Y "target\\SimplexS.jar" "C:\\app\\SimplexS.jar"'
            }
        }

        stage('Restart Service') {
            steps {
                bat 'net stop SImplexS'
                bat 'net start SImplexS'
            }
        }
    }
}