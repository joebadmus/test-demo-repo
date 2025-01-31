pipeline {
    agent {
        kubernetes {
            yamlFile 'pod-definition.yaml'  // Reference to YAML file
            defaultContainer 'azure-cli'    // Start in Azure CLI container
        }
    }
    environment {
        ACR_NAME = 'youracrname'  // Your Azure Container Registry name
        ACR_REPO = 'youracrrepo'  // Your ACR repository
        IMAGE_TAG = 'latest'
    }
    stages {
        stage('Azure Login & Prepare Credentials') {
            steps {
                container('azure-cli') {
                    script {
                        withCredentials([string(credentialsId: 'azure-service-principal', variable: 'AZURE_CREDENTIALS')]) {
                            def creds = readJSON(text: AZURE_CREDENTIALS)
                            sh """
                                # Authenticate to Azure
                                az login --service-principal -u ${creds.clientId} -p ${creds.clientSecret} --tenant ${creds.tenantId}

                                # Retrieve ACR login token
                                TOKEN=\$(az acr login --name ${ACR_NAME} --expose-token --output tsv --query accessToken)

                                # Create .docker directory in shared volume
                                mkdir -p /shared/.docker

                                # Create Docker config.json manually for Kaniko

                                  # Use printf to properly format JSON and escape special characters
                                printf '{\n  "auths": {\n    "https://%s.azurecr.io": {\n      "identitytoken": "%s"\n    }\n  }\n}\n' \
                                "${ACR_NAME}" "\$TOKEN" > /shared/.docker/config.json

                                
                                // echo '{' | tee /shared/.docker/config.json
                                // echo '  "auths": {' | tee -a /shared/.docker/config.json
                                // echo '    "https://${ACR_NAME}.azurecr.io": {' | tee -a /shared/.docker/config.json
                                // echo '      "identitytoken": "'"\$TOKEN"'"' | tee -a /shared/.docker/config.json
                                // echo '    }' | tee -a /shared/.docker/config.json
                                // echo '  }' | tee -a /shared/.docker/config.json
                                // echo '}' | tee -a /shared/.docker/config.json


                                // cat <<EOF > /shared/.docker/config.json
                                // {
                                //     "auths": {
                                //         "https://${ACR_NAME}.azurecr.io": {
                                //             "auth": "$(echo -n "00000000:${TOKEN}" | base64 -w0)"
                                //         }
                                //     }
                                // }
                                // EOF
                                
                                // echo "Docker config.json created successfully!"
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
