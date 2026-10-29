# Archivalia Digital Library

Archivalia is a React + TypeScript digital library backed by Spring Boot microservices. Members can search the collection, borrow and return books, check due dates, and view assessed late fees. JWT authentication, an API Gateway, Eureka service discovery, and Spring Cloud LoadBalancer connect the services.

## Database

For a quick local demo, the Auth, Book, Borrow, and Fine services use separate in-memory H2 databases and do not require PostgreSQL. Demo data is lost when a service restarts. To retain data, set `ARCHIVALIA_PROFILE=postgres` and connect these services to your existing PostgreSQL database `archivalia` and role `archivalia_user`. Each service owns separate tables in that database. The API Gateway and Eureka Server do not use PostgreSQL.

The PostgreSQL JDBC driver is managed by Spring Boot 3.4.4 for Java 21. Hibernate's existing `ddl-auto: update` setting creates or updates the service tables when a data service starts. There is no Flyway or Liquibase migration setup in the project. Back up important data and review schema changes before using this automatic development-time update against valuable data.

**No PostgreSQL password, JWT secret, or administrator password is committed to this project.** Follow [DATABASE_SETUP.md](./DATABASE_SETUP.md) to start the demo without PostgreSQL or securely configure your existing PostgreSQL database. Do not put credentials in chat.

## Run the demo without PostgreSQL

From PowerShell in the project root, run:

```powershell
.\scripts\Start-ArchivaliaServices.ps1
```

The launcher defaults to the Spring `demo` profile, prompts securely for a JWT signing secret and initial administrator password, then opens the Eureka server, Auth, Book, Fine, Borrow, API Gateway, and frontend. Registration and sign-in return a session immediately after the account details are validated. Signed-in users can change their email or password from **Account** settings after confirming their current password. The demo uses in-memory H2 databases, so its accounts and activity are cleared when a service restarts. Keep the service windows open while using the app. For persistent PostgreSQL use, see [DATABASE_SETUP.md](./DATABASE_SETUP.md).

## Services and ports

| Service | Purpose | Port |
|---|---|---:|
| React + Nginx frontend | Catalogue, account forms, borrowing and member shelf | 5173 |
| API Gateway | Central routing and JWT/admin authorization | 8080 |
| Eureka Server | Service registry | 8761 |
| Auth Service | Accounts, password hashes, JWT issuance | 8081 |
| Book Service | Searchable catalogue and copy availability | 8082 |
| Fine Service | Persistent late-fee assessments | 8083 |
| Borrow Service | Loan history and returns | 8084 |
| PostgreSQL (persistent profile only) | Existing `archivalia` database | 5432 |
| H2 (demo profile) | Separate in-memory database per data service | internal |

## Local frontend

Start the backend services and API Gateway by following [DATABASE_SETUP.md](./DATABASE_SETUP.md) first. The registration API is not available until the Auth Service registers with Eureka and the gateway is running on port 8080.

From `frontend`, install the dependencies once with `npm ci`, then run `npm run dev`. Open `http://localhost:5173`.

## Docker Compose

Docker Compose builds the frontend and microservices, but connects them to your **existing** PostgreSQL server; it does not create another PostgreSQL server, database, or role. The backend targets Java 17 and runs on Java 17 or newer. From a PowerShell terminal in the project root, set `$env:ARCHIVALIA_PROFILE = "postgres"`, dot-source `scripts/Set-ArchivaliaEnvironment.ps1` to enter credentials securely, then run `docker compose up --build` in that same terminal. Use `host.docker.internal` for a host-installed PostgreSQL server from Docker Desktop.

The services will not start with the example secrets or if the required credentials are missing. Change the example loan duration and fine rate only if your policy requires it; local defaults are 14 days and ₹1 per overdue day.

## Tests

From the project root, run `mvn verify`. The repository does not include a Maven wrapper; use an installed Maven 3.9+ and Java 17 or newer. Run the frontend production build with `npm ci` and `npm run build` from `frontend`.
