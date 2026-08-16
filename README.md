# Inventory Reporting Service

Spring Boot service that generates inventory activity CSV reports in the background.

You submit a job over HTTP. The service stores it, puts a message on RabbitMQ, and a worker builds the report by calling the [inventory management API](https://github.com/jliang65/inventory-management). When the job finishes, the CSV is on disk and the job record has the file path.

## Requirements

- Java 17
- Docker (for Postgres and RabbitMQ, or for the whole stack)
- The [inventory management API](https://github.com/jliang65/inventory-management) running, if you want reports to actually generate



## How a job runs

1. `POST /api/jobs` creates a row in `report_jobs` with status `QUEUED` and publishes the job id to RabbitMQ.
2. A worker claims the job (processing token + 5 minute lease) so two workers cannot process the same job at once.
3. The worker loads transactions from the inventory API and writes `reports/inventory-activity-{id}.csv`.
4. The job is marked `COMPLETED`, or the lease is released so a retry can claim it again.
5. After 3 failed attempts the message goes to the dead-letter queue and the job is marked `FAILED`.

Statuses: `QUEUED`, `PROCESSING`, `COMPLETED`, `FAILED`.

## Run with Docker

From this directory:

```bash
docker compose up --build
```

That starts Postgres (port 5434), RabbitMQ (5672, management UI 15672), and the app on [http://localhost:8081](http://localhost:8081).

Point it at the inventory API:

```bash
INVENTORY_API_BASE_URL=http://host.docker.internal:8080 \
INVENTORY_API_TOKEN=your-token \
docker compose up --build
```

CSV files are written to `./reports` on the host.

## Run locally

Start only the dependencies:

```bash
docker compose up postgres rabbitmq
```

Then:

```bash
export INVENTORY_API_BASE_URL=http://localhost:8080
export INVENTORY_API_TOKEN=your-token
./mvnw spring-boot:run
```

Defaults assume Postgres at `localhost:5434` and RabbitMQ at `localhost:5672`.

## Environment


| Variable                 | Default                                                | Purpose                            |
| ------------------------ | ------------------------------------------------------ | ---------------------------------- |
| `DB_URL`                 | `jdbc:postgresql://localhost:5434/inventory_reporting` | Postgres JDBC URL                  |
| `DB_USERNAME`            | `postgres`                                             | Database user                      |
| `DB_PASSWORD`            | `postgres`                                             | Database password                  |
| `RABBITMQ_HOST`          | `localhost`                                            | RabbitMQ host                      |
| `RABBITMQ_PORT`          | `5672`                                                 | RabbitMQ port                      |
| `RABBITMQ_USERNAME`      | `guest`                                                | RabbitMQ user                      |
| `RABBITMQ_PASSWORD`      | `guest`                                                | RabbitMQ password                  |
| `INVENTORY_API_BASE_URL` | `http://localhost:8080`                                | Inventory API base URL             |
| `INVENTORY_API_TOKEN`    | empty                                                  | Bearer token for the inventory API |
| `REPORTS_DIRECTORY`      | `reports`                                              | Where CSV files are written        |


Schema is applied with Flyway on startup.

## API

Swagger UI: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)

### Create a job

`POST /api/jobs`

```json
{
  "startDate": "2026-01-01",
  "endDate": "2026-01-31",
  "locationId": 1
}
```

All fields are optional.

- Omit `startDate` to include history from the beginning.
- Omit `endDate` to use today.
- Omit `locationId` to include all locations.
- If `locationId` is set, the inventory API must already have that location (`GET /api/locations/{id}`).
- `endDate` cannot be before `startDate`.

Returns `201` and the job. Type is always `INVENTORY_ACTIVITY`.

```bash
curl -s -X POST http://localhost:8081/api/jobs \
  -H "Content-Type: application/json" \
  -d '{"startDate":"2026-01-01","endDate":"2026-01-31"}'
```



### Get a job

`GET /api/jobs/{id}`

`resultPath` is the CSV path after the job completes.

### List jobs

`GET /api/jobs`

Query params:

- `status` — `QUEUED`, `PROCESSING`, `COMPLETED`, or `FAILED`
- `locationId`
- `page`, `size`, `sort` (default: 20 per page, newest first)

```bash
curl -s "http://localhost:8081/api/jobs?status=COMPLETED&page=0&size=20"
```



## Reports

Each completed job writes a CSV named `inventory-activity-{jobId}.csv`. The file has report details, a summary (stock in/out, adjustments, transfers), then the transaction rows.

Generated CSV files cannot be accessed using this API. Open them from the `reports` directory.

The worker calls:

- `GET /api/locations/{id}` when a location is specified
- `GET /api/inventory/transactions` (paged) for the date range and location



## Tests

```bash
./mvnw test
```

`JobServiceTest` is mocked. `JobServiceClaimTest` and the Spring Boot context test need Postgres and RabbitMQ running (the same `docker compose up postgres rabbitmq` as local run).