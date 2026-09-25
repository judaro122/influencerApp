pipeline {
    agent any

    environment {
        // Database & Storage credentials for ephemeral CI stack
        DB_PASSWORD = 'influencerapp_ci'
        MINIO_ROOT_PASSWORD = 'minioadmin_ci'
        JWT_SECRET = 'ci-super-secret-jwt-key-minimum-32-chars-long-12345'
        ENCRYPTION_KEY = 'ci-super-secret-jwt-key-minimum-32-chars-long-12345'
        GEMINI_API_KEY = 'ci-placeholder-key'
        
        // Target Base URL for the automated test suite
        TARGET_BASE_URL = 'http://localhost:8080'
    }

    stages {
        stage('Compile Application') {
            steps {
                echo 'Compiling InfluencerAPP backend source code...'
                sh 'mvn clean compile'
            }
        }

        stage('Run Unit & Contract Tests') {
            steps {
                echo 'Running unit and contract tests...'
                sh 'mvn test'
            }
        }

        stage('Spin Up Ephemeral Stack') {
            steps {
                echo 'Building and starting application stack via Docker Compose...'
                sh 'docker compose build app'
                sh 'docker compose up -d'
            }
        }

        stage('Wait For System Health') {
            steps {
                echo 'Polling http://localhost:8080/api/health until status reports UP...'
                timeout(time: 3, unit: 'MINUTES') {
                    sh '''
                        until curl -s -f http://localhost:8080/api/health | grep -q '"status":"UP"'; do
                            echo "Waiting for service to become healthy..."
                            sleep 5
                        done
                        echo "Service is healthy and ready for automated testing."
                    '''
                }
            }
        }

        stage('Execute Automated API Test Suite') {
            steps {
                echo 'Running REST Assured test suite against ephemeral environment...'
                dir('influencer-api-tests') {
                    sh 'mvn clean test -Dtarget.base.url=${TARGET_BASE_URL}'
                }
            }
        }
    }

    post {
        always {
            echo 'Cleaning up ephemeral Docker Compose containers and volumes...'
            sh 'docker compose down -v'
            junit '**/target/surefire-reports/*.xml'
            allure includeProperties: false, jdk: '', reportBuildPolicy: 'ALWAYS', results: [[path: 'influencer-api-tests/target/allure-results']]
        }
    }
}
