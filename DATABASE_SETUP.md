# Archivalia PostgreSQL and Local Setup

This guide connects the application to your existing PostgreSQL 16.9 installation. It does **not** create, drop, or reset a database or role.

## 1. Prerequisites

- PostgreSQL 16.9 is running on `localhost:5432`.
- The existing database is `archivalia`.
- The existing application role is `archivalia_user`, with the database/schema permissions you already granted.
- Java 17 or later and Maven 3.9 or later are available on `PATH`. The backend targets Java 17 for compatibility with both Java 17 and Java 21 runtimes. There is no Maven wrapper in this workspace.
- For the web interface, Node.js 20+ and npm are available.
- Docker Desktop is optional. It is only needed for Docker Compose deployment.
- The project uses Spring Boot 3.4.4, Spring Cloud 2024.0.1, and the PostgreSQL JDBC driver managed by Spring Boot.

PostgreSQL is shared by the Auth, Book, Borrow, and Fine services. Those services create/manage their own distinct tables in `archivalia`; no extra database is needed. The API Gateway and Eureka Server do not use a database.

## 2. Enter credentials securely

Spring Boot does not load `.env` files by itself. `.env.example` lists the non-secret connection settings and blank secret placeholders, but it contains no usable credentials. Do not copy passwords or keys into chat, source code, or a committed file.

Open a **new PowerShell terminal** in the project root and dot-source the helper:

```powershell
. .\scripts\Set-ArchivaliaEnvironment.ps1
```

By default the helper selects the PostgreSQL-free `demo` profile, then requests a JWT signing secret and the initial administrator password using hidden terminal prompts. Press Enter at the JWT prompt to generate a secret automatically. The administrator email defaults to `admin@archivalia.local` and can be changed when prompted. To use your persistent PostgreSQL database instead, first set `$env:ARCHIVALIA_PROFILE = "postgres"`; the helper will also request the password for `archivalia_user`. The helper checks that the JWT secret is at least 32 UTF-8 bytes and the administrator password is at least 12 characters. It stores credentials only as environment variables in the current PowerShell process and its child processes. It does not write or print secrets.

Run Maven or the local service launcher from this **same terminal**. The demo profile does not use or prompt for a PostgreSQL password. If you select the PostgreSQL profile and the prompt rejects the login, stop and rerun with the correct password. The project never resets role passwords or changes PostgreSQL authentication settings.

To set or change non-secret connection settings in that PowerShell session before running the helper, for example:

```powershell
$env:POSTGRES_HOST = "localhost"
$env:POSTGRES_PORT = "5432"
$env:POSTGRES_DB = "archivalia"
$env:POSTGRES_USER = "archivalia_user"
$env:ADMIN_EMAIL = "admin@archivalia.local"
$env:LOAN_PERIOD_DAYS = "14"
$env:FINE_PER_DAY = "1.00"
```

The helper supplies these defaults if they are not set. For native Windows services, `POSTGRES_HOST=localhost` is appropriate. For Docker Desktop, it sets `POSTGRES_HOST_DOCKER=host.docker.internal` so containers can reach PostgreSQL installed on the Windows host. If PostgreSQL runs elsewhere, set both host values to the appropriate address before starting.

## 3. Test PostgreSQL connectivity

The `psql` command-line client must be on `PATH` or installed in the usual PostgreSQL 16/18 location. From the project root, run:

```powershell
.\scripts\Test-ArchivaliaPostgres.ps1
```

`psql` prompts for the password without putting it in the command. The script runs a read-only `SELECT` to show the connected database, role, and PostgreSQL version. It does not change data or permissions.

If `psql` is not on `PATH`, that does not indicate a bad password. In pgAdmin, connect to the existing server using the `archivalia_user` role and open **Tools → Query Tool** for the `archivalia` database. Run this read-only query:

```sql
SELECT current_database(), current_user, version();
```

Confirm it reports the expected database, role, and PostgreSQL 16.9 version. Do not grant extra privileges or change authentication settings just to silence an error.

## 4. Start the services on Windows

Use the secure setup step above in one PowerShell terminal. The default launcher uses in-memory H2 databases and does not require PostgreSQL:

```powershell
.\scripts\Start-ArchivaliaServices.ps1
```

The script installs only the shared `common` project module into the local Maven cache, then opens terminals for Eureka, the four data services, the gateway, and the frontend. Demo data is temporary and is lost when services restart. To use PostgreSQL, set `$env:ARCHIVALIA_PROFILE = "postgres"` before sourcing the credential helper. The script does not reset credentials or write secrets to a file. Read each service terminal to verify startup. The frontend and browser URLs are printed when the script finishes.

To start services manually instead, open separate PowerShell terminals. Set the secure environment variables in every terminal before starting an application process. Start these services in order:

1. Eureka Server:

   ```powershell
   mvn -pl discovery-server spring-boot:run
   ```

2. Install the shared `common` module once from the project root. Auth Service and API Gateway depend on it:

   ```powershell
   mvn -pl common install
   ```

3. Auth, Book, Fine, and Borrow services (each in its own terminal):

   ```powershell
   $env:SPRING_PROFILES_ACTIVE = "demo"
   mvn -pl auth-service spring-boot:run
   ```

   ```powershell
   $env:SPRING_PROFILES_ACTIVE = "demo"
   mvn -pl book-service spring-boot:run
   ```

   ```powershell
   $env:SPRING_PROFILES_ACTIVE = "demo"
   mvn -pl fine-service spring-boot:run
   ```

   ```powershell
   $env:SPRING_PROFILES_ACTIVE = "demo"
   mvn -pl borrow-service spring-boot:run
   ```

4. API Gateway (its terminal also needs the same JWT secret):

   ```powershell
   mvn -pl api-gateway spring-boot:run
   ```

5. Frontend (in another terminal):

   ```powershell
   Set-Location .\frontend
   npm ci
   npm run dev
   ```

   `npm ci` installs only the packages listed in the frontend lockfile. Run it once per clean install.

The services keep their existing ports: Eureka `8761`, gateway `8080`, auth `8081`, book `8082`, fine `8083`, borrow `8084`, and the frontend `5173`.

## 5. Optional Docker Compose startup

Start Docker Desktop. Docker Compose uses the existing PostgreSQL server; it does not use the local H2 demo profile. In a PowerShell terminal at the project root, select the PostgreSQL profile and enter credentials securely using `. .\scripts\Set-ArchivaliaEnvironment.ps1`, then run:

```powershell
docker compose up --build
```

Compose builds the application containers and connects them to the host's **existing** PostgreSQL instance at `host.docker.internal:5432/archivalia`. It does not start a database container or run database/role-creation SQL. The required passwords and JWT secret are passed to the application containers as runtime environment variables.

The local demo profile instead uses separate in-memory H2 databases. It is suitable for trying the application without a PostgreSQL login; all demo accounts, books, loans and fines are lost when their service processes stop.

Open the frontend at `http://localhost:5173`; the Eureka dashboard is at `http://localhost:8761`. Stop the application with `Ctrl+C`, or use `docker compose down` in another terminal. No database volume is created or deleted.

## 6. Verify service and database health

- **PostgreSQL:** run the read-only psql test above; confirm it prints `archivalia`, `archivalia_user`, and PostgreSQL version `16.9`.
- **Eureka:** open `http://localhost:8761`. After startup, confirm that `AUTH-SERVICE`, `BOOK-SERVICE`, `FINE-SERVICE`, `BORROW-SERVICE`, and `API-GATEWAY` register.
- **Database-backed catalogue/API:** run:

  ```powershell
  Invoke-RestMethod http://localhost:8080/api/books
  ```

  An HTTP response with the book list (or an empty list) confirms the gateway and Book Service responded. Each data service must also log that its application started successfully; database startup failures are reported in that service's terminal.
- **Frontend:** open `http://localhost:5173` and confirm the catalogue loads.

The project does not include Spring Boot Actuator, so it does not expose a separate `/actuator/health` endpoint.

## 7. Troubleshoot common problems

### PostgreSQL login failed

Check that PostgreSQL is running, the host and port are correct, and `archivalia_user` has the correct password and access to `archivalia`. A `password authentication failed` error means PostgreSQL rejected the supplied role/password; a `no pg_hba.conf entry` message indicates the existing server authentication rules rejected the connection. Enter the correct password at the hidden prompt or ask your database administrator to verify the existing account and grants. This project will not reset passwords, edit `pg_hba.conf`, or weaken authentication automatically.

### The service cannot create or update a table

Confirm that the role has the required `CONNECT` and schema/table creation privileges in `archivalia`. Keep existing data intact and review any permission change with the database owner. Do not drop tables to fix a permissions error.

### Port already in use

Ports `5432`, `8761`, `8080`-`8084`, or `5173` may already have a process bound to them. Stop the conflicting application or deliberately change that application's configured port; update service registrations and gateway URLs consistently if a service port changes. Do not stop a process until you identify what it is.

### Docker containers cannot connect to PostgreSQL

Inside Docker Desktop, `localhost` means the container itself. The Compose file uses `POSTGRES_HOST_DOCKER=host.docker.internal` for the PostgreSQL server on the Windows host. Confirm PostgreSQL is listening on an address reachable from Docker Desktop and accepts connections from the Docker network under its existing authentication rules. Do not change those rules automatically.

### Missing environment variable

Open a new PowerShell terminal and dot-source `. .\scripts\Set-ArchivaliaEnvironment.ps1`. Environment variables set in a different terminal, VS Code process, or Windows session are not automatically copied into this terminal.

## 8. Schema creation and migrations

There is currently no Flyway or Liquibase dependency and no versioned database migration. Each JPA service retains its existing `spring.jpa.hibernate.ddl-auto: update`, which allows Hibernate to create or non-destructively update tables based on that service's entity mappings when it starts.

The project does **not** use `create` or `create-drop`, and its Docker Compose file does not run initialization SQL. Back up a database with valuable data before changing entity classes or starting a schema update. For a production release, review the SQL changes and move schema changes to versioned Flyway/Liquibase migrations before deployment. Never apply a destructive migration without reviewing it and getting explicit approval.

## 9. Build and test

From the project root, use the installed Maven (there is no `mvnw.cmd` in this workspace):

```powershell
mvn verify
```

This compiles the multi-module backend and runs the existing unit tests. It does not start PostgreSQL or prove that the provided database credentials authenticate. Run the psql check and service startup steps above for those separate checks.
