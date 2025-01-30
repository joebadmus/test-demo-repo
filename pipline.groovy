pipeline {
    agent {
        kubernetes {
            yamlFile 'pod-definition.yaml'  // Reference to YAML file
            defaultContainer 'azure-cli'    // Start in the Azure CLI container
        }
    }
    environment {
        ACR_NAME = 'youracrname'  // Your Azure Container Registry name
        ACR_REPO = 'youracrrepo'  // Your ACR repository
        IMAGE_TAG = 'latest'
    }
    stages {
        stage('Azure Login') {
            steps {
                container('azure-cli') {
                    script {
                        withCredentials([string(credentialsId: 'azure-service-principal', variable: 'AZURE_CREDENTIALS')]) {
                            def creds = readJSON(text: AZURE_CREDENTIALS)
                            sh """
                                # Authenticate to Azure
                                az login --service-principal -u ${creds.clientId} -p ${creds.clientSecret} --tenant ${creds.tenantId}

                                # Log in to ACR
                                az acr login --name ${ACR_NAME}

                                # Copy Docker auth config to shared volume for Kaniko
                                mkdir -p /shared/.docker
                                cp ~/.docker/config.json /shared/.docker/config.json
                                echo "Docker credentials copied to shared volume"
                            """
                        }
                    }
                }
            }
        }
        
        stage('Build and Push with Kaniko') {
            steps {
                container('kaniko') {
                    script {
                        sh """
                            /kaniko/executor \\
                            --context=/workspace \\
                            --dockerfile=/workspace/Dockerfile \\
                            --destination=${ACR_NAME}.azurecr.io/${ACR_REPO}:${IMAGE_TAG} \\
                            --docker-config=/shared/.docker/
                        """
                    }
                }
            }
        }
    }
}
