# EV Booking Backend - Environment Setup & Deployment

This document outlines the environment setup and deployment process for the EV Booking Backend across development, staging, and production environments.

## Environment Overview

### Development Environment
- **Profile**: `dev`
- **Port**: `8080`
- **Database**: Local PostgreSQL
- **Features**: Hot reload, detailed logging, H2 console access

### Staging Environment
- **Profile**: `staging`
- **Port**: `8081`
- **Database**: Staging PostgreSQL
- **Features**: Production-like setup, testing environment

### Production Environment
- **Profile**: `prod`
- **Port**: `8082`
- **Database**: Production PostgreSQL
- **Features**: SSL enabled, monitoring, high performance

## Quick Start

### Development
```bash
# Start with Docker Compose
docker-compose up -d

# Or run locally
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Staging Deployment
```bash
# 1. Copy and configure environment file
cp .env.staging.example .env.staging
# Edit .env.staging with your values

# 2. Deploy
./scripts/deployment/deploy-staging.sh
```

### Production Deployment
```bash
# 1. Copy and configure environment file
cp .env.production.example .env.production
# Edit .env.production with your secure values

# 2. Ensure SSL certificates are in place
# Place keystore.p12 in ssl/ directory

# 3. Deploy
./scripts/deployment/deploy-production.sh
```

## Environment Configuration

### Required Environment Variables

#### Staging (.env.staging)
- `POSTGRES_PASSWORD`: Database password
- `REDIS_PASSWORD`: Redis password
- `JWT_SECRET_KEY`: JWT signing secret
- `MAIL_USERNAME`: Email service username
- `MAIL_PASSWORD`: Email service password
- `STRIPE_PUBLIC_KEY_STAGING`: Stripe test public key
- `STRIPE_SECRET_KEY_STAGING`: Stripe test secret key

#### Production (.env.production)
- `POSTGRES_PASSWORD`: Secure database password
- `REDIS_PASSWORD`: Secure Redis password
- `JWT_SECRET_KEY`: Strong JWT signing secret (min 256 bits)
- `SSL_KEYSTORE_PASSWORD`: SSL keystore password
- `STRIPE_PUBLIC_KEY`: Stripe live public key
- `STRIPE_SECRET_KEY`: Stripe live secret key
- `GRAFANA_PASSWORD`: Grafana admin password

### SSL Certificate Setup (Production)
```bash
# Generate SSL certificate (example with Let's Encrypt)
certbot certonly --standalone -d yourdomain.com

# Convert to PKCS12 format
openssl pkcs12 -export -in cert.pem -inkey privkey.pem -out ssl/keystore.p12
```

## Deployment Scripts

### Staging Deployment (`deploy-staging.sh`)
- Builds staging Docker images
- Performs health checks
- Displays service status
- Rollback on failure

### Production Deployment (`deploy-production.sh`)
- Pre-deployment checks
- Environment validation
- SSL certificate verification
- Blue-green deployment simulation
- Comprehensive health checks
- Automatic rollback on failure
- Database and Redis connectivity tests
- Deployment notifications

## Monitoring & Health Checks

### Health Endpoints
- Development: `http://localhost:8080/api/actuator/health`
- Staging: `http://localhost:8081/api/actuator/health`
- Production: `https://localhost:8082/api/actuator/health`

### Monitoring (Production)
- **Grafana**: `http://localhost:3000`
- **Prometheus**: `http://localhost:9090`
- **Application Metrics**: `/api/actuator/metrics`

## Database Management

### Running Migrations
```bash
# Development
./gradlew update

# Staging/Production
# Migrations run automatically on startup via Liquibase
```

### Environment-Specific Data
- Development: Test data loaded automatically
- Staging: Staging-specific test data
- Production: Production data only

## Security Considerations

### Production Security Features
- SSL/TLS encryption
- JWT token authentication
- Rate limiting
- Security headers (HSTS, XSS protection)
- Non-root container execution
- Secret management via environment variables
- Database connection pooling with security settings

### Network Security
- Internal Docker networks
- Exposed ports only where necessary
- Nginx reverse proxy with security headers

## Troubleshooting

### Common Issues

#### Health Check Failures
```bash
# Check application logs
docker-compose -f docker-compose.production.yml logs app

# Check database connectivity
docker-compose -f docker-compose.production.yml exec postgres pg_isready
```

#### SSL Certificate Issues
```bash
# Verify certificate
openssl pkcs12 -info -in ssl/keystore.p12
```

#### Performance Issues
```bash
# Check resource usage
docker stats

# Application metrics
curl http://localhost:8082/api/actuator/metrics
```

### Log Locations
- Development: Console output
- Staging: `/app/logs/ev-booking-staging.log`
- Production: `/app/logs/ev-booking-production.log`

## Backup & Recovery

### Database Backups
```bash
# Automated backups are configured in production
# Manual backup
docker-compose -f docker-compose.production.yml exec postgres pg_dump -U ev_prod_user ev_booking_prod_db > backup.sql
```

## Support

For deployment issues or questions:
1. Check application logs
2. Verify environment configuration
3. Ensure all required services are running
4. Review security settings and certificates