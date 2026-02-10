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

POST /api/v1/tools/report/export

Full URL:

{method}://{host}:8080/api/v1/tools/report/export

---

## 🧾 Request Headers

| Header       | Value            |
|--------------|------------------|
| Content-Type | application/json |

---

## 📦 Request Body
cookie take from "cookie" of the header of http request of SonarQ server which you intend to export
example: "XSRF-TOKEN=jh6otqhpbdqovihqcrk81qb1ar; JWT-SESSION=eyJhbGciOiJIUzI1NiJ9.eyJsYXN0UmVmcmVzaFRpbWUiOjE3NzAyNTgyMzczNDUsInhzcmZUb2tlbiI6ImpoNm90cWhwYmRxb3ZpaHFjcms4MXFiMWFyIiwianRpIjoiMTY5Y2ZkMzMtZTgyNC00YzQ5LWJkOTgtNDRlZDA5NTBjYmM2Iiwic3ViIjoiNWQwOGRlOTQtZGRjNS00MzZlLWI3YzgtYzFjN2E1ZTM0OTE2IiwiaWF0IjoxNzY5NjY3MTg5LCJleHAiOjE3NzA1MTc0Mzd9.7MANfcjhgKBeDGJgH_OMcOcsNoljTNg5A9_F0vRNYMc"

```json
{
  "cookie": "XSRF-TOKEN=jh6otqhpbdqovihqcrk81qb1ar; JWT-SESSION=eyJhbGciOiJIUzI1NiJ9.eyJsYXN0UmVmcmVzaFRpbWUiOjE3N",
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
curl --location '{method}://{host}:8080/api/v1/tools/report/export' \
--header 'Content-Type: application/json' \
--data '{
  "cookie": "XSRF-TOKEN=jh6otqhpbdqovihqcrk81qb1ar; JWT-SESSION=eyJhbGciOiJIUzI1NiJ9.eyJsYXN0UmVmcmVzaFRpbWUiOjE3N",
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
dcn1-backend-api,dcn1-frontend-app,dcn2-backend-api,dcn2-frontend-app,pn2-atj-ecall-backend-api,pn2-atj-ecall-frontend-app,pn2-bmw-api-backend-api,pn2-bmw-backend-api,pn2-bmw-frontend-app,pn2-byd-backend-api,pn2-byd-frontend-app,pn2-common-app,pn2-dht-emg-backend-api,pn2-dht-emg-frontend-app,pn2-mmc-icall-backend-api,pn2-nbc-ecall-backend-api,pn2-nbc-ecall-frontend-app,pn2-nmc-backend-api,pn2-nmc-frontend-app,pn2-nsf-backend-api,pn2-nsf-frontend-app,pn2-ntm-ecall-backend-api,pn2-ntm-ecall-frontend-app,pn2-pad-backend-api,pn2-pad-frontend-app,pn2-pdrive-api,pn2-pdrive-frontend-app,pn2-rev-backend-api,pn2-rev-frontend-app,pn2-sbr-bcall-backend-api,pn2-sbr-bcall-frontend-app,pn2-sbr-ecall-backend-api,pn2-sbr-ecall-frontend-app,pn2-sekisui-house-backend-api,pn2-sekisui-house-frontend-app,pn2-share-app,pn2-share-service,pn2-tmnf-backend-api,pn2-tmnf-den-backend-api,pn2-tmnf-den-frontend-app,pn2-tmnf-frontend-app,pn2-tmnf-pio2-backend-api,pn2-tmnf-pio2-frontend-app,pn2-wiki-api
##