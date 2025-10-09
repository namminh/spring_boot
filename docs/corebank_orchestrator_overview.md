# CoreBank Orchestrator Overview

## Local PostgreSQL via Docker

```bash
docker run --name corebank-db \
  -e POSTGRES_DB=corebank \
  -e POSTGRES_USER=orchestrator \
  -e POSTGRES_PASSWORD=changeme \
  -p 5432:5432 \
  -d postgres:15
```

Apply database migrations or sample data using `src/main/resources/data.sql` once available.

## Architecture Notes
- Spring Boot orchestrator adapts legacy C/Pro*C payment core via JDBC/PostgreSQL snapshot tables.
- Monitoring endpoints surface health metrics, recent transactions, and alerting state for Flutter clients.
- Future work replaces JPA adapters with native Pro*C gateway and integrates Kafka/Flyway for realtime and schema management.
```
