#!/bin/bash

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${GREEN}Starting EV Booking Backend Production Deployment${NC}"

# Pre-deployment checks
echo -e "${BLUE}Performing pre-deployment checks...${NC}"

# Check if .env.production exists
if [ ! -f .env.production ]; then
    echo -e "${RED}Error: .env.production file not found${NC}"
    echo "Please create .env.production with required environment variables"
    exit 1
fi

# Check required environment variables
REQUIRED_VARS=(
    "POSTGRES_PASSWORD"
    "REDIS_PASSWORD"
    "JWT_SECRET_KEY"
    "STRIPE_SECRET_KEY"
    "MAIL_PASSWORD"
    "SSL_KEYSTORE_PASSWORD"
    "GRAFANA_PASSWORD"
)

export $(cat .env.production | grep -v '^#' | xargs)

for var in "${REQUIRED_VARS[@]}"; do
    if [ -z "${!var}" ]; then
        echo -e "${RED}Error: Required environment variable $var is not set${NC}"
        exit 1
    fi
done

# Check SSL certificates
if [ ! -f "ssl/keystore.p12" ]; then
    echo -e "${RED}Error: SSL keystore not found at ssl/keystore.p12${NC}"
    echo "Please generate SSL certificates before deployment"
    exit 1
fi

echo -e "${GREEN}Pre-deployment checks passed${NC}"

# Backup existing data
echo -e "${YELLOW}Creating backup...${NC}"
./scripts/backup/backup-production.sh

# Build production images
echo -e "${YELLOW}Building production images...${NC}"
docker-compose -f docker-compose.production.yml build --no-cache

# Run tests
echo -e "${YELLOW}Running tests...${NC}"
docker run --rm -v "$(pwd)":/app -w /app openjdk:17-jdk-slim ./gradlew test

if [ $? -ne 0 ]; then
    echo -e "${RED}Tests failed! Deployment aborted.${NC}"
    exit 1
fi

echo -e "${GREEN}Tests passed!${NC}"

# Blue-green deployment simulation
echo -e "${YELLOW}Starting blue-green deployment...${NC}"

# Stop old containers gracefully
echo -e "${YELLOW}Gracefully stopping old containers...${NC}"
docker-compose -f docker-compose.production.yml stop app || true

# Start new containers
echo -e "${YELLOW}Starting new production services...${NC}"
docker-compose -f docker-compose.production.yml up -d

echo -e "${YELLOW}Waiting for services to be ready...${NC}"
sleep 60

# Health check with retry logic
echo -e "${YELLOW}Performing comprehensive health checks...${NC}"
HEALTH_URL="https://localhost:8082/api/actuator/health"
MAX_ATTEMPTS=60
ATTEMPT=1

while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
    if curl -k -f $HEALTH_URL > /dev/null 2>&1; then
        echo -e "${GREEN}Health check passed!${NC}"
        break
    else
        echo "Attempt $ATTEMPT/$MAX_ATTEMPTS failed, retrying in 10 seconds..."
        sleep 10
        ATTEMPT=$((ATTEMPT + 1))
    fi
done

if [ $ATTEMPT -gt $MAX_ATTEMPTS ]; then
    echo -e "${RED}Health check failed after $MAX_ATTEMPTS attempts${NC}"
    echo -e "${RED}Rolling back deployment...${NC}"

    # Rollback
    docker-compose -f docker-compose.production.yml down

    echo "Checking logs..."
    docker-compose -f docker-compose.production.yml logs app
    exit 1
fi

# Additional service checks
echo -e "${YELLOW}Checking database connection...${NC}"
DB_CHECK=$(docker-compose -f docker-compose.production.yml exec -T postgres pg_isready -U ${POSTGRES_USER:-ev_prod_user} -d ${POSTGRES_DB:-ev_booking_prod_db})
if [[ $DB_CHECK == *"accepting connections"* ]]; then
    echo -e "${GREEN}Database connection: OK${NC}"
else
    echo -e "${RED}Database connection: FAILED${NC}"
    exit 1
fi

echo -e "${YELLOW}Checking Redis connection...${NC}"
REDIS_CHECK=$(docker-compose -f docker-compose.production.yml exec -T redis redis-cli -a ${REDIS_PASSWORD} ping)
if [[ $REDIS_CHECK == "PONG" ]]; then
    echo -e "${GREEN}Redis connection: OK${NC}"
else
    echo -e "${RED}Redis connection: FAILED${NC}"
    exit 1
fi

# Clean up old images
echo -e "${YELLOW}Cleaning up old Docker images...${NC}"
docker image prune -f

echo -e "${GREEN}Production deployment completed successfully!${NC}"
echo -e "${YELLOW}Application is running at: https://localhost:8082/api${NC}"
echo -e "${YELLOW}Health check: https://localhost:8082/api/actuator/health${NC}"
echo -e "${YELLOW}Monitoring: http://localhost:3000 (Grafana)${NC}"
echo -e "${YELLOW}Metrics: http://localhost:9090 (Prometheus)${NC}"

# Display running services
echo -e "${YELLOW}Running services:${NC}"
docker-compose -f docker-compose.production.yml ps

# Send deployment notification
echo -e "${BLUE}Sending deployment notification...${NC}"
./scripts/notifications/deployment-success.sh "production"

echo -e "${GREEN}Deployment notification sent!${NC}"