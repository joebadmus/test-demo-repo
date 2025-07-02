#!/bin/bash

# Configuration variables (replace with your values)
TENANT_ID="your-tenant-id"
CLIENT_ID="your-client-id"
CLIENT_SECRET="your-client-secret"
DCE_URI="https://your-dce-name.region.ingest.monitor.azure.com"
DCR_IMMUTABLE_ID="dcr-your-dcr-id"
STREAM_NAME="Custom-MyTableRawData"
API_VERSION="2023-01-01"

# Function to get Entra ID access token
get_access_token() {
  local token_response=$(curl -s -X POST \
    -d "grant_type=client_credentials&client_id=$CLIENT_ID&client_secret=$CLIENT_SECRET&resource=https://monitor.azure.com/" \
    "https://login.microsoftonline.com/$TENANT_ID/oauth2/token")
  
  echo "$token_response" | jq -r '.access_token'
}

# Function to send JSON logs to Log Analytics
send_logs() {
  local json_data="$1"
  local access_token="$2"
  
  # Send the JSON data to the Logs Ingestion API
  response=$(curl -s -X POST \
    -H "Authorization: Bearer $access_token" \
    -H "Content-Type: application/json" \
    -d "$json_data" \
    "$DCE_URI/dataCollectionRules/$DCR_IMMUTABLE_ID/streams/$STREAM_NAME?api-version=$API_VERSION")
  
  # Check the response
  if [ -z "$response" ]; then
    echo "Successfully sent logs to Azure Log Analytics (HTTP 204)"
  else
    echo "Error sending logs: $response"
    exit 1
  fi
}

# Main script
# Read JSON input from stdin
read -r json_input

# Validate JSON input
if ! echo "$json_input" | jq . >/dev/null 2>&1; then
  echo "Error: Invalid JSON input"
  exit 1
fi

# Get access token
access_token=$(get_access_token)
if [ -z "$access_token" ]; then
  echo "Error: Failed to obtain access token"
  exit 1
fi

# Send logs to Azure Log Analytics
send_logs "$json_input" "$access_token"

exit 0