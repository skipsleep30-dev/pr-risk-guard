# PR Risk Guard

PR Risk Guard is a learning-friendly Java modular monolith that assigns a risk level to pull requests. It can run entirely offline with deterministic rules or call TypeSafe AI's Jev model for typed, confidence-aware decisions.

The project is intentionally small enough to understand end to end while still demonstrating Spring Boot, REST APIs, persistence, modular design, webhook security, external AI integration, testing, metrics, Docker, and a lightweight frontend.

## What it does

- Accepts pull-request metadata through a REST API.
- Optionally receives signed GitHub `pull_request` webhooks.
- Classifies risk as `LOW`, `MEDIUM`, `HIGH`, or `CRITICAL`.
- Estimates security sensitivity, test adequacy, and the need for human review.
- Stores analysis results in H2 by default or PostgreSQL through a profile.
- Displays recent analyses in a small browser dashboard.
- Switches between deterministic rules and Jev through configuration.

## Architecture

```text
GitHub webhook / browser
          |
          v
  github / analysis modules
          |
          v
    decision module ------> rules or Jev API
          |
          v
    narrative module
          |
          v
       H2 / PostgreSQL
```

The direct subpackages under `com.ankit.prguard` are Spring Modulith application modules:

- `analysis`: use case orchestration, REST API, and persistence
- `decision`: rule-based and Jev decision engines
- `github`: webhook signature validation and payload ingestion
- `narrative`: human-readable explanation generation
- `shared`: small set of cross-module value objects and configuration

`ModularityTest` verifies module dependencies during the test build.

## Run locally

Requirements: Java 17 or newer. The Maven wrapper is included.

```bash
./mvnw spring-boot:run
```

Open <http://localhost:8080> and submit the prefilled example. H2 persists data beneath `./data`.

Run tests:

```bash
./mvnw test
```

## Use Jev

The default rule engine keeps local setup simple. To use Jev:

```bash
export DECISION_PROVIDER=jev
export JEV_API_KEY=your-key
./mvnw spring-boot:run
```

The integration sends pull-request state and four typed questions to `POST /v1/systemone`:

- `Choice`: pull-request risk level
- `Noul`: security-sensitive change
- `Score`: test adequacy
- `Noul`: explicit human-review requirement

The application combines Jev's typed results and confidence values in Java. It escalates high-risk, security-sensitive, or low-confidence results to human review.

## REST API

Set `PRGUARD_PASSWORD1` and `PRGUARD_PASSWORD2` to distinct development passwords
before starting the app. API and dashboard requests use HTTP Basic authentication.
The signed GitHub webhook remains independently authenticated by its HMAC signature.

Analysis requests now return `202 Accepted` with a `PENDING` job. Poll
`GET /api/analyses/{id}` until it is `COMPLETED` or `FAILED`. Repeating the same
request with the same `Idempotency-Key` returns the existing job; reusing the key
for different PR data returns `409 Conflict`. `POST /api/analyses/{id}/retry`
requeues a failed job. Users can see only their own jobs and analyses.

On an existing PostgreSQL database, run this once before starting the updated
application, because pending jobs have no decision or narrative yet:

```sql
ALTER TABLE pull_request_analysis ALTER COLUMN risk_level DROP NOT NULL;
ALTER TABLE pull_request_analysis ALTER COLUMN decision_model DROP NOT NULL;
ALTER TABLE pull_request_analysis ALTER COLUMN narrative DROP NOT NULL;
```

Existing rows predate ownership and remain outside user history. Use a fresh
development database or backfill those rows before relying on old history.
After Hibernate updates an existing schema, confirm that the database has the
`uq_analysis_owner_key` unique constraint on `(owner, request_key)`; concurrent
deduplication depends on that database constraint.

Create an analysis:

```bash
curl -X POST http://localhost:8080/api/analyses \
  -H 'Content-Type: application/json' \
  -d '{
    "repository": "acme/payments-api",
    "number": 42,
    "title": "Update authentication token validation",
    "description": "Adds stricter validation to the login flow",
    "author": "demo-user",
    "additions": 180,
    "deletions": 45,
    "changedFiles": 6,
    "files": [
      "src/main/java/auth/TokenValidator.java",
      "src/test/java/auth/TokenValidatorTest.java"
    ],
    "testsPresent": true
  }'
```

List recent analyses:

```bash
curl http://localhost:8080/api/analyses
```

Health and metrics:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/prometheus
```

## GitHub webhook

Configure a webhook for `pull_request` events and set:

```bash
export GITHUB_WEBHOOK_SECRET=a-long-random-secret
```

Endpoint:

```text
POST /api/github/webhook
```

The application verifies `X-Hub-Signature-256` with HMAC-SHA256 before processing a delivery. For a local demonstration without signatures, set `GITHUB_REQUIRE_SIGNATURE=false`.

GitHub webhook payloads do not include the changed-file list. This MVP evaluates the counts included in the payload. Manual API submissions support filenames, which makes security-sensitive path detection more useful. Fetching changed files from the GitHub API is a suggested next step.

## PostgreSQL and Docker

Copy the environment template and start both services:

```bash
cp .env.example .env
docker compose up --build
```

The application uses the `postgres` Spring profile inside Docker. The default local profile uses H2.

## Suggested learning path

1. Trace a request from `AnalysisController` to the database.
2. Change one rule and add a corresponding unit test.
3. Enable Jev and inspect the request and typed response in a debugger.
4. Add a GitHub API client that fetches changed filenames.
5. Replace the template narrative with an LLM-backed implementation.
6. Add a small labeled dataset and calculate risk-classification accuracy.
7. Publish analysis results as GitHub Checks.
8. Introduce Kafka only after the synchronous workflow is understood.

## Scope and limitations

- The rule engine is educational and is not a substitute for security review.
- AI results never merge code or execute repository actions.
- The application intentionally keeps one database and one deployment unit.
- Secrets are read from environment variables and are excluded from source control.

## Resume wording after you understand and extend it

> Built a modular Java and Spring Boot application for confidence-aware pull-request risk analysis, integrating signed GitHub webhooks, typed Jev decisions, persistence, human-review escalation, automated tests, and operational metrics.
