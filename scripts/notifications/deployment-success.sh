#!/bin/bash

ENVIRONMENT=$1

if [ -z "$ENVIRONMENT" ]; then
    echo "Usage: $0 <environment>"
    exit 1
fi

# Get commit information
COMMIT_SHA=$(git rev-parse HEAD 2>/dev/null || echo "unknown")
COMMIT_MESSAGE=$(git log -1 --pretty=%B 2>/dev/null || echo "No commit message")
BRANCH_NAME=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")
DEPLOYED_BY=$(git log -1 --pretty=%an 2>/dev/null || echo "unknown")

# Environment specific settings
case $ENVIRONMENT in
    "staging")
        URL="https://staging.evbooking.com"
        COLOR="warning"
        ;;
    "production")
        URL="https://api.evbooking.com"
        COLOR="good"
        ;;
    *)
        URL="N/A"
        COLOR="good"
        ;;
esac

# Create timestamp
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S UTC')

echo "Sending deployment notification for $ENVIRONMENT environment..."
echo "Deployment completed successfully at $TIMESTAMP"
echo "Environment: $ENVIRONMENT"
echo "URL: $URL"
echo "Branch: $BRANCH_NAME"
echo "Commit: $COMMIT_SHA"
echo "Deployed by: $DEPLOYED_BY"