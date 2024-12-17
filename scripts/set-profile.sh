#!/bin/bash

# Function to print usage
print_usage() {
    echo "Usage: source ./set-profile.sh [local|cloud]"
    echo ""
    echo "Profiles:"
    echo "  local  - Set environment variables for local development"
    echo "  cloud  - Set environment variables for cloud deployment"
    echo ""
    echo "Note: This script must be sourced, not executed"
    echo "Example: source ./set-profile.sh local"
}

# Check if the script is being sourced
if [ "$0" = "$BASH_SOURCE" ]; then
    echo "Error: This script must be sourced, not executed"
    echo "Usage: source ./set-profile.sh [local|cloud]"
    exit 1
fi

# Check if profile argument is provided
if [ -z "$1" ]; then
    print_usage
    return 1
fi

# Set environment variables based on profile
case "$1" in
    "local")
        export KAFKA_BOOTSTRAP_SERVERS="localhost:29092"
        export SCHEMA_REGISTRY_URL="http://localhost:8081"
        export PORT="8085"
        export HOST="0.0.0.0"
        export KAFKA_TOPIC="parking.entry.events"
        echo "Local profile environment variables set:"
        ;;
    "cloud")
        export KAFKA_BOOTSTRAP_SERVERS="cloud-kafka:9092"
        export SCHEMA_REGISTRY_URL="http://cloud-schema-registry:8081"
        export PORT="8080"
        export HOST="0.0.0.0"
        export KAFKA_TOPIC="parking.entry.events"
        echo "Cloud profile environment variables set:"
        ;;
    *)
        echo "Error: Invalid profile '$1'"
        print_usage
        return 1
        ;;
esac

# Print current environment variables
echo "KAFKA_BOOTSTRAP_SERVERS=$KAFKA_BOOTSTRAP_SERVERS"
echo "SCHEMA_REGISTRY_URL=$SCHEMA_REGISTRY_URL"
echo "PORT=$PORT"
echo "HOST=$HOST"
echo "KAFKA_TOPIC=$KAFKA_TOPIC"
