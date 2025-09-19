# EV Booking Authentication API Testing Guide

This guide shows how to test the authentication system with static OTP 1234.

## Prerequisites

1. Start the application:
```bash
./gradlew bootRun
# OR
docker-compose up -d
```

2. Application should be running on: `http://localhost:8080`

## Static Client Token

Use this static client token for all auth endpoints:
```
X-Client-Token: ev-booking-client-2024-v1-static-token
```

## API Testing Steps

### 1. Generate Client Token (Optional - for development)

```bash
curl -X GET http://localhost:8080/api/auth/generate-client-token \
  -H "Content-Type: application/json"
```

### 2. Send OTP

```bash
curl -X POST http://localhost:8080/api/auth/send-otp \
  -H "Content-Type: application/json" \
  -H "X-Client-Token: ev-booking-client-2024-v1-static-token" \
  -d '{
    "mobileNumber": "9876543210"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "otpId": 1,
    "message": "OTP sent successfully",
    "expiresAt": "2024-09-16T18:15:00"
  }
}
```

### 3. Verify OTP & Login (Use OTP: 1234)

```bash
curl -X POST http://localhost:8080/api/auth/verify-otp \
  -H "Content-Type: application/json" \
  -H "X-Client-Token: ev-booking-client-2024-v1-static-token" \
  -d '{
    "mobileNumber": "9876543210",
    "otpCode": "1234"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "userId": 1,
    "phoneNumber": "9876543210",
    "email": null,
    "firstName": null,
    "lastName": null,
    "role": "CUSTOMER",
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
  }
}
```

### 4. Use JWT Token for Authenticated Requests

Save the `accessToken` from step 3 and use it for authenticated endpoints:

```bash
# Example: Logout from all devices
curl -X POST http://localhost:8080/api/auth/logout-all \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN_HERE"
```

### 5. Refresh Token

```bash
curl -X POST http://localhost:8080/api/auth/refresh-token \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "YOUR_REFRESH_TOKEN_HERE"
  }'
```

### 6. Logout

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN_HERE" \
  -d '{
    "refreshToken": "YOUR_REFRESH_TOKEN_HERE"
  }'
```

## Test Different Mobile Number Formats

The system accepts these formats:
- `9876543210` (10 digits)
- `919876543210` (13 digits with country code)

## Error Testing

### Invalid OTP
```bash
curl -X POST http://localhost:8080/api/auth/verify-otp \
  -H "Content-Type: application/json" \
  -H "X-Client-Token: ev-booking-client-2024-v1-static-token" \
  -d '{
    "mobileNumber": "9876543210",
    "otpCode": "0000"
  }'
```

### Missing Client Token
```bash
curl -X POST http://localhost:8080/api/auth/send-otp \
  -H "Content-Type: application/json" \
  -d '{
    "mobileNumber": "9876543210"
  }'
```

Should return 401 Unauthorized.

### Rate Limiting Test
Send OTP request 6 times in quick succession to test rate limiting (5 requests per hour limit).

## Authentication Flow Summary

1. **Initial Setup**: Get/use static client token
2. **Send OTP**: Mobile number → OTP sent (static 1234)
3. **Verify OTP**: Mobile + OTP → JWT tokens + User creation
4. **Use JWT**: Access token for authenticated endpoints
5. **Refresh**: Use refresh token to get new access token
6. **Logout**: Revoke tokens

## Notes

- OTP is always `1234` for now (configurable for production)
- Users are auto-created on first successful OTP verification
- JWT access tokens expire in 1 hour (configurable)
- Refresh tokens expire in 30 days
- Rate limiting: 5 OTP requests per hour per mobile number
- OTP expires in 5 minutes
- Maximum 3 OTP attempts per request