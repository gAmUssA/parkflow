#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to print status
print_status() {
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ $1${NC}"
    else
        echo -e "${RED}✗ $1${NC}"
        exit 1
    fi
}

# Check if required tools are installed
echo -e "\n${YELLOW}Checking required tools...${NC}"
for tool in docker docker-compose curl kafkacat jq; do
    if command_exists $tool; then
        echo -e "${GREEN}✓ $tool is installed${NC}"
    else
        echo -e "${RED}✗ $tool is not installed${NC}"
        if [ "$tool" = "kafkacat" ]; then
            echo "Please install kafkacat: brew install kcat"
        elif [ "$tool" = "jq" ]; then
            echo "Please install jq: brew install jq"
        fi
        exit 1
    fi
done

# Start services if not running
echo -e "\n${YELLOW}Starting services...${NC}"
docker-compose up -d
print_status "Services started"

# Wait for services to be healthy
echo -e "\n${YELLOW}Waiting for services to be healthy...${NC}"
timeout=90
elapsed=0
while [ $elapsed -lt $timeout ]; do
    if docker-compose ps | grep -q "unhealthy\|starting"; then
        echo -n "."
        sleep 5
        elapsed=$((elapsed + 5))
    else
        echo
        break
    fi
done

if [ $elapsed -ge $timeout ]; then
    echo -e "${RED}Timeout waiting for services to be healthy${NC}"
    exit 1
fi

# Check Kafka connectivity
echo -e "\n${YELLOW}Checking Kafka connectivity...${NC}"
echo "test message" | kcat -b localhost:9092 -t test-topic -P
print_status "Can produce messages to Kafka"

kcat -b localhost:9092 -t test-topic -C -c 1 -e
print_status "Can consume messages from Kafka"

# Check Schema Registry
echo -e "\n${YELLOW}Checking Schema Registry...${NC}"
curl -s http://localhost:8081/subjects | jq . >/dev/null
print_status "Schema Registry is accessible"

# List Kafka topics
echo -e "\n${YELLOW}Available Kafka topics:${NC}"
kcat -b localhost:9092 -L | grep topic

# Print connection details
echo -e "\n${YELLOW}Connection Details:${NC}"
echo "Kafka Bootstrap Servers: localhost:9092"
echo "Schema Registry URL: http://localhost:8081"
echo "DuckDB Port: 3000"

echo -e "\n${GREEN}All services are running and accessible!${NC}"
