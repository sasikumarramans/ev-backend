# CI/CD Pipeline Setup Guide

This guide explains how to set up and configure the CI/CD pipelines for the EV Booking Backend across all environments.

## Pipeline Overview

### 🔄 Available Pipelines

1. **CI Pipeline** (`ci.yml`) - Runs on every push/PR
   - Unit tests with PostgreSQL & Redis
   - Security scanning with Trivy
   - Docker image builds
   - Coverage reporting

2. **Staging Deploy** (`deploy-staging.yml`) - Auto-deploys `develop` branch
   - Deploys to staging environment
   - Smoke tests & health checks
   - Slack notifications

3. **Production Deploy** (`deploy-production.yml`) - Manual deployment from `main`
   - Requires manual confirmation
   - Blue-green deployment
   - Comprehensive health checks
   - Automatic rollback on failure

4. **Emergency Rollback** (`rollback.yml`) - Manual rollback capability
   - Supports staging and production
   - Backup restoration
   - Health verification

## Initial Setup

### 1. GitHub Repository Secrets

#### Required for All Environments
```bash
# Docker Hub
DOCKER_USERNAME=your-docker-username
DOCKER_PASSWORD=your-docker-password

# Slack Notifications
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/...
```

#### Staging Environment Secrets
```bash
# Server Access
STAGING_SSH_PRIVATE_KEY=-----BEGIN OPENSSH PRIVATE KEY-----...
STAGING_HOST=staging.yourdomain.com
STAGING_USER=ubuntu

# Database
STAGING_POSTGRES_PASSWORD=secure_staging_password
STAGING_DB_PASSWORD=secure_staging_password
STAGING_REDIS_PASSWORD=staging_redis_password

# Application
STAGING_JWT_SECRET_KEY=staging-jwt-secret-256-bits-minimum
STAGING_MAIL_USERNAME=staging@yourdomain.com
STAGING_MAIL_PASSWORD=staging_mail_password

# Payment (Stripe Test)
STAGING_STRIPE_PUBLIC_KEY=pk_test_...
STAGING_STRIPE_SECRET_KEY=sk_test_...

# Configuration
STAGING_CORS_ALLOWED_ORIGINS=https://staging.yourdomain.com
STAGING_WEBHOOK_URL=https://staging-webhook.yourdomain.com
```

#### Production Environment Secrets
```bash
# Server Access
PRODUCTION_SSH_PRIVATE_KEY=-----BEGIN OPENSSH PRIVATE KEY-----...
PRODUCTION_HOST=api.yourdomain.com
PRODUCTION_USER=ubuntu

# Database
PRODUCTION_POSTGRES_DB=ev_booking_prod_db
PRODUCTION_POSTGRES_USER=ev_prod_user
PRODUCTION_POSTGRES_PASSWORD=ultra_secure_production_password
PRODUCTION_REDIS_PASSWORD=ultra_secure_redis_password

# Application
PRODUCTION_JWT_SECRET_KEY=production-ultra-secure-jwt-key-256-bits-minimum
PRODUCTION_JWT_EXPIRATION=1800000
PRODUCTION_MAIL_USERNAME=noreply@yourdomain.com
PRODUCTION_MAIL_PASSWORD=production_mail_password

# Payment (Stripe Live)
PRODUCTION_STRIPE_PUBLIC_KEY=pk_live_...
PRODUCTION_STRIPE_SECRET_KEY=sk_live_...

# SSL
PRODUCTION_SSL_KEYSTORE_PASSWORD=secure_keystore_password

# Configuration
PRODUCTION_CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://admin.yourdomain.com
PRODUCTION_WEBHOOK_URL=https://webhook.yourdomain.com
PRODUCTION_GRAFANA_PASSWORD=secure_grafana_password
```

### 2. Server Preparation

#### Staging Server Setup
```bash
# Install Docker & Docker Compose
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Create application directories
mkdir -p ~/ev-booking-staging/{logs,uploads,ssl}
mkdir -p ~/ev-booking-staging/backups
```

#### Production Server Setup
```bash
# Same as staging, plus:
mkdir -p ~/ev-booking-production/{logs,uploads,ssl}
mkdir -p ~/ev-booking-production/backups
mkdir -p ~/ev-booking-production/monitoring

# Install SSL certificates
# Place your keystore.p12 in ~/ev-booking-production/ssl/
```

### 3. GitHub Environments

Create the following environments in GitHub:
- **staging** - Auto-deploy protection rules
- **production** - Required reviewers, manual approval

## Pipeline Workflows

### Development Workflow
```mermaid
graph LR
    A[Push to develop] --> B[CI Pipeline]
    B --> C[Tests Pass]
    C --> D[Build Images]
    D --> E[Deploy to Staging]
    E --> F[Smoke Tests]
    F --> G[Slack Notification]
```

### Production Workflow
```mermaid
graph LR
    A[Push to main] --> B[CI Pipeline]
    B --> C[Manual Approval]
    C --> D[Pre-deployment Checks]
    D --> E[Backup Production]
    E --> F[Blue-Green Deploy]
    F --> G[Health Checks]
    G --> H{Health OK?}
    H -->|Yes| I[Success Notification]
    H -->|No| J[Auto Rollback]
    J --> K[Alert Team]
```

## Usage

### Staging Deployment
```bash
# Automatic on push to develop
git push origin develop

# Or manual trigger from GitHub Actions tab
```

### Production Deployment
```bash
# Push to main branch
git push origin main

# Then manually trigger from GitHub Actions:
# 1. Go to GitHub Actions tab
# 2. Select "Deploy to Production"
# 3. Click "Run workflow"
# 4. Type "deploy-to-production" for confirmation
# 5. Click "Run workflow"
```

### Emergency Rollback
```bash
# From GitHub Actions tab:
# 1. Select "Emergency Rollback"
# 2. Choose environment (staging/production)
# 3. Type "ROLLBACK-CONFIRMED"
# 4. Optionally specify backup timestamp
# 5. Click "Run workflow"
```

## Monitoring & Notifications

### Slack Integration
Notifications are sent to these channels:
- `#deployments` - Staging deployments
- `#production-deployments` - Production deployments
- `#production-alerts` - Failures and rollbacks

### Health Check Endpoints
- Staging: `http://your-staging-host:8081/api/actuator/health`
- Production: `https://your-production-host:8082/api/actuator/health`

### Monitoring (Production)
- Grafana: `http://your-production-host:3000`
- Prometheus: `http://your-production-host:9090`

## Troubleshooting

### Common Issues

#### Pipeline Fails at Docker Build
```bash
# Check Docker Hub credentials
# Verify secrets: DOCKER_USERNAME, DOCKER_PASSWORD
```

#### SSH Connection Fails
```bash
# Verify SSH key format (should be OpenSSH format)
# Check server accessibility
# Verify user has Docker permissions
```

#### Health Checks Fail
```bash
# Check application logs on server
docker-compose -f docker-compose.production.yml logs app

# Verify database connectivity
docker-compose -f docker-compose.production.yml exec postgres pg_isready
```

#### SSL Issues (Production)
```bash
# Verify keystore file exists
ls -la ~/ev-booking-production/ssl/keystore.p12

# Check keystore password
openssl pkcs12 -info -in ssl/keystore.p12
```

### Emergency Procedures

#### Complete System Failure
1. Trigger emergency rollback workflow
2. Check backup integrity
3. Verify service restoration
4. Investigate root cause
5. Plan fixes for next deployment

#### Database Issues
1. Stop application containers
2. Restore from backup manually
3. Verify data integrity
4. Restart services
5. Monitor closely

## Security Best Practices

### Secret Management
- Rotate secrets regularly
- Use separate secrets for each environment
- Never commit secrets to repository
- Use GitHub's secret scanning

### Server Security
- Keep servers updated
- Use SSH key authentication only
- Implement proper firewall rules
- Monitor access logs
- Regular security audits

### Application Security
- Use HTTPS everywhere
- Implement rate limiting
- Regular dependency updates
- Security scanning in CI
- Proper error handling (no sensitive data leaks)

## Maintenance

### Regular Tasks
- Weekly backup verification
- Monthly secret rotation
- Quarterly security reviews
- Dependency updates
- Performance monitoring review

### Backup Strategy
- Automated daily backups
- Weekly backup verification
- Monthly backup archiving
- Disaster recovery testing

## Support

### Pipeline Issues
1. Check GitHub Actions logs
2. Verify all secrets are set
3. Check server connectivity
4. Review application logs

### Getting Help
- Check this documentation first
- Review pipeline logs in GitHub Actions
- Check server logs via SSH
- Contact DevOps team for infrastructure issues