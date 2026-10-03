# Warranty Claims API

A small Java backend for product warranty claims. An applicant opens a claim and uploads a PDF or image as evidence. The application checks the warranty through a SOAP service. A reviewer can then approve or reject a submitted claim.

This portfolio demo explores a REST → SOAP integration and S3 document storage. It is not a production insurance or warranty system.

## What is inside

- Java 21, Spring Boot 4.1, Spring Web MVC, Spring Data JPA
- PostgreSQL and Flyway SQL migrations
- Spring Web Services client for a WSDL-defined SOAP operation
- AWS SDK for Java 2.x for S3-compatible document storage
- Spring Security with two demo users and HTTP Basic authentication
- JUnit 5, Mockito and Spring Web Services client tests
- Docker Compose for PostgreSQL, an S3-compatible test server and a SOAP stub

The local S3 server is [Adobe S3Mock](https://github.com/adobe/S3Mock). It implements the S3 API for development and tests and starts with the `claim-documents` bucket. The Java code uses the AWS S3 SDK. Set `S3_ENDPOINT` to another compatible endpoint, or to an empty value for the SDK's AWS endpoint; provision the bucket first. The local SOAP service is a WireMock stub; its [WSDL](wiremock/__files/warranty.wsdl) shows the contract used by the client.

## Run locally

Requirements: Docker with Compose. The first build downloads images and Gradle dependencies.

```bash
docker compose up --build -d
docker compose ps
```

The API runs at `http://localhost:8080`. PostgreSQL is mapped to port `5434`, S3Mock to `9090`, and the SOAP stub to `8081`. The SOAP contract is available at `http://localhost:8081/soap/warranties?wsdl`.

Demo users:

| Role | Username | Password |
| --- | --- | --- |
| Applicant | `applicant` | `demo-applicant` |
| Reviewer | `reviewer` | `demo-reviewer` |

These passwords are for the local demo only. The application requires `APP_APPLICANT_PASSWORD` and `APP_REVIEWER_PASSWORD` to be set; Compose supplies the demo values. Set them to different values before running the application outside this demo stack. HTTP Basic also needs HTTPS in a real deployment.

### Try the complete flow

1. Create a claim with the active demo warranty `POL-1001`:

   ```bash
   curl -u applicant:demo-applicant -H 'Content-Type: application/json' \
     -d '{"warrantyNumber":"POL-1001","description":"Laptop screen stopped working"}' \
     http://localhost:8080/api/claims
   ```

2. Upload a real PDF, PNG or JPEG (up to 5 MB). Use the claim ID from step 1:

   ```bash
   curl -u applicant:demo-applicant \
     -F 'file=@/path/to/proof.pdf;type=application/pdf' \
     http://localhost:8080/api/claims/1/documents
   ```

3. Submit the claim. This calls the SOAP warranty service:

   ```bash
   curl -u applicant:demo-applicant -X POST \
     http://localhost:8080/api/claims/1/submit
   ```

4. Review it:

   ```bash
   curl -u reviewer:demo-reviewer -H 'Content-Type: application/json' \
     -d '{"decision":"APPROVED"}' \
     http://localhost:8080/api/claims/1/decision
   ```

The stub returns an expired warranty for other numbers such as `POL-0000`. `POL-FAIL` returns a SOAP fault; the API responds with `502` and leaves the claim in `DRAFT`.

## REST endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/claims` | Create a draft claim (applicant) |
| `GET` | `/api/claims` | List own claims; reviewer lists all |
| `GET` | `/api/claims/{id}` | Read one accessible claim |
| `POST` | `/api/claims/{id}/documents` | Upload evidence to S3 (applicant, draft only) |
| `GET` | `/api/claims/{id}/documents` | List evidence metadata |
| `GET` | `/api/claims/{id}/documents/{documentId}` | Download evidence |
| `POST` | `/api/claims/{id}/submit` | Verify warranty through SOAP and submit |
| `POST` | `/api/claims/{id}/decision` | Approve or reject (reviewer) |

Statuses: `DRAFT` → `SUBMITTED` → `APPROVED` or `REJECTED`. An inactive warranty makes the claim `REJECTED` during submission. A claim needs at least one document before submission.

## Tests and configuration

```bash
./gradlew test
```

Tests cover claim state rules, access to another applicant's claim, file type checks, S3 upload calls and SOAP response handling. The full Compose flow can also be exercised with the commands above.

Configuration is in [`application.yml`](src/main/resources/application.yml). Environment variables let you change the database, SOAP URL, S3 endpoint, bucket, credentials and demo passwords. The database schema is created by Flyway, while Hibernate checks that the entities match it.

## Limits of this demo

- Authentication uses two in-memory demo accounts. Real accounts should use a dedicated identity provider, HTTPS and proper user management.
- S3Mock and WireMock are local test doubles. This project does not claim to run on AWS or integrate with a real warranty provider.
- A file upload writes to S3 before saving metadata in PostgreSQL. If metadata saving fails, the code attempts to remove the object; this is a best-effort compensation, not a distributed transaction.
- Documents are limited to 5 MB and checked by MIME type and file signature. Production file handling would also need malware scanning and stronger content validation.

Stop the local services with `docker compose down`. Add `-v` only if you intentionally want to remove the demo database and S3 data.
