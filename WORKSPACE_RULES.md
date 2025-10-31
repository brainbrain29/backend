# Workspace Rules

This document summarizes the tech stack, dependencies, and project structure for this workspace.

## Tech Stack
- Java 17
- Spring Boot 3.5.x
- Spring Web, Data JPA, Security, Actuator
- MySQL 8.x
- Flyway (DB migration)
- Lombok
- JJWT (JSON Web Token)
- SpringDoc OpenAPI (Swagger UI)
- Thymeleaf (if needed for views)

## Key Dependencies (from pom.xml)
- org.springframework.boot:spring-boot-starter-web
- org.springframework.boot:spring-boot-starter-data-jpa
- org.springframework.boot:spring-boot-starter-security
- org.springframework.boot:spring-boot-starter-actuator
- org.springframework.boot:spring-boot-starter-thymeleaf
- com.mysql:mysql-connector-j (runtime)
- org.flywaydb:flyway-core, flyway-mysql
- io.jsonwebtoken:jjwt-api, jjwt-impl (runtime), jjwt-jackson (runtime)
- org.projectlombok:lombok (provided)
- org.springdoc:springdoc-openapi-starter-webmvc-ui
- spring-boot-devtools (runtime optional)
- spring-boot-starter-test (test scope)

## Build & Plugins
- maven-compiler-plugin (source/target 17)
- spring-boot-maven-plugin
- flyway-maven-plugin (configured with local MySQL URL/user/password)

## Project Structure (high level)
- src/main/java/com/pandora/backend
  - config/ (e.g., StartupTokenCleaner)
  - controller/
    - AuthController
    - EmployeeController
    - LogController
    - MilestoneController (includes CEO-only create endpoint)
    - NoticeController
    - ProjectController (CEO-only project creation)
    - TaskController (includes /tasks/assign for managers)
  - dto/
    - EmployeeDTO, LogDTO, MilestoneDTO, Notice* DTOs
    - ProjectDTO
    - ProjectCreateDTO (for CEO project creation)
    - MilestoneCreateDTO (for CEO milestone creation)
    - TaskDTO, TokenPair
  - entity/
    - Employee, Department, Team relations
    - Project, Milestone, Task, Log, Notice, RefreshToken
  - repository/
    - JPA repositories for entities (EmployeeRepository, ProjectRepository, ...)
  - service/
    - AuthService, TaskService, LogService, NoticeService
    - ProjectService (project creation)
    - MilestoneService (milestone creation)
  - filter/
    - JwtAuthFilter (validates token and injects request attribute userId)
  - util/
    - JwtUtil (token generation/validation)
- src/main/resources
  - application.properties / application.yaml (not shown here)
  - db/migration (Flyway SQL: V2__create_test_data.sql, etc.)

## Authentication & Authorization
- JwtAuthFilter expects header: `Authorization: Bearer <token>`
- On success, sets `request.setAttribute("userId", <id>)` for controllers
- Controllers use `EmployeeRepository` to load the user and check `position`:
  - CEO-only: `position == 0`
  - Manager and above: `position >= 2`

## API Conventions
- JSON bodies use camelCase fields
- All protected endpoints require Bearer token
- Error responses use standard HTTP status codes: 401 (Unauthorized), 403 (Forbidden), 400 (Bad Request)

## Migrations & Test Data
- Flyway manages schema and seed data (employees, teams, a sample project, milestones, tasks, logs)

## Notes
- DTOs are used to decouple API models from entities
- Avoid using `any` types; keep explicit types for fields and return values
- Keep controllers thin; delegate logic to services
