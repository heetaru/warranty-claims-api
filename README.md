# Warranty Claims API

A small Spring Boot API for product warranty claims. An applicant creates a claim, uploads proof, and submits it for a warranty check. A reviewer then approves or rejects it. There is no frontend; use Postman or another HTTP client.

The API uses PostgreSQL for claim data, S3-compatible storage for files, and SOAP for the warranty check. Docker Compose runs local S3 and SOAP test services, not real external providers.

## Run

```bash
docker compose up --build -d
```

API: `http://localhost:8080`

| Role | Username | Password |
| --- | --- | --- |
| Applicant | `applicant` | `demo-applicant` |
| Reviewer | `reviewer` | `demo-reviewer` |

## Test with Postman

Import [the Postman collection](postman/Warranty%20Claims%20API.postman_collection.json). Send its requests in order:

1. **Create active claim** → `DRAFT`; the collection saves the claim ID.
2. **Upload proof** → select [demo-proof.png](postman/demo-proof.png) as the `file` in Body → form-data.
3. **Submit claim** → the SOAP check accepts `POL-1001`, so the status becomes `SUBMITTED`.
4. **Approve claim** in the Reviewer folder → `APPROVED`.

`POL-0000` returns an inactive warranty; `POL-FAIL` triggers a SOAP fault. Uploaded files must be PDF, PNG, or JPEG, up to 5 MB.

## Run Java from IntelliJ IDEA

Start only the supporting services with `docker compose up -d db s3mock soap`. Run `WarrantyClaimsApplication` with `APP_APPLICANT_PASSWORD=demo-applicant` and `APP_REVIEWER_PASSWORD=demo-reviewer` in its run configuration. The database uses `localhost:5434`; port `5432` may belong to another project. Stop the Compose `app` service first if it already occupies port `8080`.

## Tests

```bash
./gradlew test
```

Stop the local stack with `docker compose down`. This keeps the demo data in Docker volumes.
