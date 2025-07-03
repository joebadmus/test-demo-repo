#!/bin/sh
# Read password from file or environment variable
if [ -f "$SSL_PASSWORD_FILE" ]; then
    SSL_PASSWORD=$(cat "$SSL_PASSWORD_FILE")
else
    echo "Error: SSL password file not found"
    exit 1
fi

# Decrypt the private key
openssl rsa -in /etc/nginx/certs/server.key -out /etc/nginx/certs/server_decrypted.key -passin pass:"$SSL_PASSWORD"

# Start Nginx
exec "$@"