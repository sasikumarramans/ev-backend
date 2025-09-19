#!/bin/bash

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}Starting Production Backup${NC}"

# Create backup directory with timestamp
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
BACKUP_DIR="backups/backup-$TIMESTAMP"
mkdir -p $BACKUP_DIR

echo -e "${YELLOW}Backup directory: $BACKUP_DIR${NC}"

# Load environment variables if available
if [ -f .env.production ]; then
    export $(cat .env.production | grep -v '^#' | xargs)
fi

# Database backup
echo -e "${YELLOW}Creating database backup...${NC}"
if docker-compose -f docker-compose.production.yml ps postgres | grep -q "Up"; then
    docker-compose -f docker-compose.production.yml exec -T postgres pg_dump \
        -U ${POSTGRES_USER:-ev_prod_user} \
        ${POSTGRES_DB:-ev_booking_prod_db} > $BACKUP_DIR/database_backup.sql

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Database backup completed successfully${NC}"

        # Compress database backup
        gzip $BACKUP_DIR/database_backup.sql
        echo -e "${GREEN}Database backup compressed${NC}"
    else
        echo -e "${RED}Database backup failed${NC}"
        exit 1
    fi
else
    echo -e "${YELLOW}PostgreSQL container not running, skipping database backup${NC}"
fi

# Redis backup
echo -e "${YELLOW}Creating Redis backup...${NC}"
if docker-compose -f docker-compose.production.yml ps redis | grep -q "Up"; then
    docker-compose -f docker-compose.production.yml exec -T redis redis-cli \
        --rdb /data/dump_backup.rdb BGSAVE

    # Wait for backup to complete
    sleep 5

    docker cp $(docker-compose -f docker-compose.production.yml ps -q redis):/data/dump_backup.rdb $BACKUP_DIR/redis_backup.rdb

    if [ -f "$BACKUP_DIR/redis_backup.rdb" ]; then
        echo -e "${GREEN}Redis backup completed successfully${NC}"
    else
        echo -e "${YELLOW}Redis backup may have failed${NC}"
    fi
else
    echo -e "${YELLOW}Redis container not running, skipping Redis backup${NC}"
fi

# Application uploads backup
echo -e "${YELLOW}Creating uploads backup...${NC}"
if [ -d "uploads" ] && [ "$(ls -A uploads)" ]; then
    tar -czf $BACKUP_DIR/uploads_backup.tar.gz uploads/
    echo -e "${GREEN}Uploads backup completed successfully${NC}"
else
    echo -e "${YELLOW}No uploads directory or empty, skipping uploads backup${NC}"
fi

# Configuration backup
echo -e "${YELLOW}Creating configuration backup...${NC}"
cp docker-compose.production.yml $BACKUP_DIR/
cp -r nginx/ $BACKUP_DIR/ 2>/dev/null || echo "No nginx directory found"
cp -r monitoring/ $BACKUP_DIR/ 2>/dev/null || echo "No monitoring directory found"

# Create backup manifest
cat > $BACKUP_DIR/backup_manifest.txt << EOF
Backup created: $(date)
Backup directory: $BACKUP_DIR
Components backed up:
- Database: $([ -f "$BACKUP_DIR/database_backup.sql.gz" ] && echo "✓" || echo "✗")
- Redis: $([ -f "$BACKUP_DIR/redis_backup.rdb" ] && echo "✓" || echo "✗")
- Uploads: $([ -f "$BACKUP_DIR/uploads_backup.tar.gz" ] && echo "✓" || echo "✗")
- Configuration: ✓

Backup size: $(du -sh $BACKUP_DIR | cut -f1)
EOF

echo -e "${GREEN}Backup manifest created${NC}"

# Cleanup old backups (keep last 30 days)
echo -e "${YELLOW}Cleaning up old backups...${NC}"
find backups/ -name "backup-*" -type d -mtime +30 -exec rm -rf {} \; 2>/dev/null || true

echo -e "${GREEN}Backup completed successfully!${NC}"
echo -e "${YELLOW}Backup location: $BACKUP_DIR${NC}"
echo -e "${YELLOW}Backup size: $(du -sh $BACKUP_DIR | cut -f1)${NC}"

# Display backup contents
echo -e "${YELLOW}Backup contents:${NC}"
ls -la $BACKUP_DIR/