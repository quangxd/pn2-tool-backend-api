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