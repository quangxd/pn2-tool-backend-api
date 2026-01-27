# Sonar Issue Export Tool

This tool provides a **web portal** and a **REST API** to export issues from SonarQube for one or multiple repositories.

---

## 🌐 Web Portal Access

Use the following URL to access the export portal via browser:

{method}://{host}:8080/index.html

### Example

http://localhost:8080/index.html

---

## 📤 Export Issues via REST API

The API allows you to export SonarQube issues by repository, status, and pagination settings.

### Endpoint

POST /api/v1/tools/sonar/export

Full URL:

{method}://{host}:8080/api/v1/tools/sonar/export

---

## 🧾 Request Headers

| Header       | Value            |
|--------------|------------------|
| Content-Type | application/json |

---

## 📦 Request Body

```json
{
  "cookie": "token",
  "pageSize": 499,
  "pageNumber": 1,
  "repositories": [
    "jaa-backend",
    "Pamnas2-POC",
    "pn2-common-app"
  ],
  "statuses": [
    "CONFIRMED",
    "OPEN"
  ]
}
```

### Example

```text
curl --location '{method}://{host}:8080/api/v1/tools/sonar/export' \
--header 'Content-Type: application/json' \
--data '{
  "cookie": "token",
  "pageSize": 499,
  "pageNumber": 1,
  "repositories": [
    "jaa-backend",
    "Pamnas2-POC",
    "pn2-common-app"
  ],
  "statuses": [
    "CONFIRMED",
    "OPEN"
  ]
}'
```
## PamnasII repositories
dcn1-backend-api,
dnc2-backend-api,
jaa-backend,
Pamnas2-POC,
pn2-atj-ecall-backend-api,
pn2-bmw-api-backend-api,
pn2-bmw-backend-api,
pn2-byd-backend-api,
pn2-common-app,
pn2-dht-emg-backend,
pn2-lambda-function,
pn2-mmc-icall-backend-api,
pn2-nmc-backend-api,
pn2-nsf-backend-api,
pn2-ntm-ecall-backend,
pn2-nvt-backend-api,
pn2-pdrive-api,
pn2-revbackend-api,
pn2-sbr-bcall-backend-api,
pn2-sbr-ecall-backend-api,
pn2-sekisui-house-backend-api,
pn2-share-service,
pn2-tmnf-backend-api,
pn2-tmnf-den-backend-api,
pn2-tmnf-pio-backend-api
##