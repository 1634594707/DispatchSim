# DispatchSim Backend

Spring Boot backend for DispatchSim.

## Run locally

```powershell
.\mvnw.cmd spring-boot:run
```

The local wrapper prefers the bundled Maven under `../.tools/apache-maven-3.9.11` and falls back to a system `mvn`.

## Test

```powershell
.\mvnw.cmd test
```

## Required infrastructure

- MySQL on `localhost:3306`
- Redis on `localhost:6379`
- RabbitMQ on `localhost:5672`

Current local defaults in `application.yml` are:

- MySQL database: `dispatch_sim`
- MySQL username: `root`
- MySQL password: `1209`
- Redis: `localhost:6379`
- RabbitMQ: `localhost:5672`

If your machine uses different credentials, update `src/main/resources/application.yml` before running.

The repository root provides `docker-compose.yml` and `start-local.cmd` to boot these dependencies together with the frontend.
