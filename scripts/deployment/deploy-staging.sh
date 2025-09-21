#!/bin/bash

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}Starting EV Booking Backend Staging Deployment${NC}"

# CRITICAL FIX: Add Docker restart to prevent networking issues
echo -e "${YELLOW}Restarting Docker to prevent networking issues...${NC}"
systemctl restart docker
sleep 30

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
docker compose -f docker-compose.staging.yml down -v

echo -e "${YELLOW}Starting staging services...${NC}"
docker compose -f docker-compose.staging.yml up -d --remove-orphans

echo -e "${YELLOW}Waiting for PostgreSQL to initialize...${NC}"
sleep 20

# Check if postgres is ready
if ! docker compose -f docker-compose.staging.yml ps postgres | grep -q "healthy\|running"; then
    echo -e "${RED}PostgreSQL container failed to start properly${NC}"
    docker compose -f docker-compose.staging.yml logs postgres
    exit 1
fi

echo -e "${YELLOW}Waiting for services to be ready...${NC}"
until docker compose -f docker-compose.staging.yml exec postgres pg_isready -U ev_staging_user -d ev_booking_staging_db; do
  echo "Waiting for postgres..."
  sleep 5
done

echo -e "${YELLOW}Database ready, waiting for application...${NC}"
sleep 30

# FIXED: Use Swagger instead of health endpoint
echo -e "${YELLOW}Performing application readiness checks...${NC}"
SWAGGER_URL="http://localhost:8081/api/swagger-ui/index.html"
MAX_ATTEMPTS=20
ATTEMPT=1

while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
    if curl -f $SWAGGER_URL > /dev/null 2>&1; then
        echo -e "${GREEN}Application is ready!${NC}"
        break
    else
        echo "Attempt $ATTEMPT/$MAX_ATTEMPTS failed, retrying in 15 seconds..."
        if [ $ATTEMPT -eq 5 ]; then
            echo -e "${YELLOW}Checking application logs...${NC}"
            docker compose -f docker-compose.staging.yml logs --tail=30 app
        fi
        sleep 15
        ATTEMPT=$((ATTEMPT + 1))
    fi
done

if [ $ATTEMPT -gt $MAX_ATTEMPTS ]; then
    echo -e "${RED}Application readiness check failed after $MAX_ATTEMPTS attempts${NC}"
    echo "Final container status:"
    docker compose -f docker-compose.staging.yml ps
    echo "Application logs:"
    docker compose -f docker-compose.staging.yml logs app --tail=50
    exit 1
fi

echo -e "${GREEN}Staging deployment completed successfully!${NC}"
echo -e "${YELLOW}Application is running at: http://localhost:8081/api${NC}"
echo -e "${YELLOW}Swagger UI: http://localhost:8081/api/swagger-ui/index.html${NC}"

# Display running services
echo -e "${YELLOW}Running services:${NC}"
docker compose -f docker-compose.staging.yml ps