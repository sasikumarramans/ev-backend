#!/bin/bash

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}Starting EV Booking Backend Staging Deployment${NC}"

# Check if .env.staging exists
if [ ! -f .env.staging ]; then
    echo -e "${RED}Error: .env.staging file not found${NC}"
    echo "Please create .env.staging with required environment variables"
    exit 1
fi

# Load environment variables
export $(cat .env.staging | grep -v '^#' | xargs)

echo -e "${YELLOW}Building staging images...${NC}"
docker compose -f docker-compose.staging.yml build --no-cache

echo -e "${YELLOW}Stopping existing staging containers...${NC}"
docker compose -f docker-compose.staging.yml down

echo -e "${YELLOW}Starting staging services...${NC}"
docker compose -f docker-compose.staging.yml up -d

echo -e "${YELLOW}Waiting for services to be ready...${NC}"
sleep 45

# Health check
echo -e "${YELLOW}Performing health checks...${NC}"
HEALTH_URL="http://localhost:8081/api/actuator/health"
MAX_ATTEMPTS=30
ATTEMPT=1

while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
    if curl -f $HEALTH_URL > /dev/null 2>&1; then
        echo -e "${GREEN}Health check passed!${NC}"
        break
    else
        echo "Attempt $ATTEMPT/$MAX_ATTEMPTS failed, retrying in 10 seconds..."
        if [ $ATTEMPT -eq 5 ]; then
            echo -e "${YELLOW}Checking application logs...${NC}"
            docker compose -f docker-compose.staging.yml logs --tail=30 app
        fi
        sleep 10
        ATTEMPT=$((ATTEMPT + 1))
    fi
done

if [ $ATTEMPT -gt $MAX_ATTEMPTS ]; then
    echo -e "${RED}Health check failed after $MAX_ATTEMPTS attempts${NC}"
    echo "Checking logs..."
    docker compose -f docker-compose.staging.yml logs app
    exit 1
fi

echo -e "${GREEN}Staging deployment completed successfully!${NC}"
echo -e "${YELLOW}Application is running at: http://localhost:8081/api${NC}"
echo -e "${YELLOW}Health check: http://localhost:8081/api/actuator/health${NC}"
echo -e "${YELLOW}API Documentation: http://localhost:8081/api/swagger-ui.html${NC}"

# Display running services
echo -e "${YELLOW}Running services:${NC}"
docker compose -f docker-compose.staging.yml ps