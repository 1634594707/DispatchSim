pipeline {
    agent any

    environment {
        REGISTRY = 'your-registry'
        BACKEND_IMAGE = "${env.REGISTRY}/dispatchsim-backend"
        FRONTEND_IMAGE = "${env.REGISTRY}/dispatchsim-frontend"
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        KUBECONFIG = credentials('kubeconfig-file')
        SONAR_HOST_URL = credentials('sonar-host-url')
        SONAR_TOKEN = credentials('sonar-token')
    }

    parameters {
        choice(name: 'DEPLOY_ENV', choices: ['dev', 'prod'], description: 'Target Kubernetes environment')
        booleanParam(name: 'DEPLOY', defaultValue: true, description: 'Apply manifests after pushing images')
        booleanParam(name: 'RUN_SONAR', defaultValue: true, description: 'Run SonarQube scan for backend')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Backend') {
            steps {
                dir('dispatch-sim-backend') {
                    bat 'mvnw.cmd test -q'
                    bat 'mvnw.cmd package -DskipTests'
                }
            }
        }

        stage('SonarQube Scan') {
            when {
                expression { return params.RUN_SONAR }
            }
            steps {
                dir('dispatch-sim-backend') {
                    bat 'mvnw.cmd -B sonar:sonar -Dsonar.host.url=%SONAR_HOST_URL% -Dsonar.token=%SONAR_TOKEN%'
                }
            }
        }

        stage('Build Frontend') {
            steps {
                dir('frontend') {
                    bat 'npm ci'
                    bat 'npm run build'
                }
            }
        }

        stage('Docker Build') {
            steps {
                bat "docker build -t %BACKEND_IMAGE%:%IMAGE_TAG% dispatch-sim-backend"
                bat "docker build -t %FRONTEND_IMAGE%:%IMAGE_TAG% frontend"
            }
        }

        stage('Docker Push') {
            steps {
                bat "docker push %BACKEND_IMAGE%:%IMAGE_TAG%"
                bat "docker push %FRONTEND_IMAGE%:%IMAGE_TAG%"
            }
        }

        stage('Deploy') {
            when {
                expression { return params.DEPLOY }
            }
            steps {
                script {
                    def overlay = params.DEPLOY_ENV == 'prod' ? 'prod' : 'dev'
                    def namespace = params.DEPLOY_ENV == 'prod' ? 'dispatchsim-prod' : 'dispatchsim-dev'
                    bat """
                    kubectl apply -k k8s\\overlays\\${overlay}
                    kubectl set image deployment/dispatchsim-backend backend=%BACKEND_IMAGE%:%IMAGE_TAG% -n ${namespace}
                    kubectl set image deployment/dispatchsim-frontend frontend=%FRONTEND_IMAGE%:%IMAGE_TAG% -n ${namespace}
                    """
                }
            }
        }
    }
}
