# Core Bank System

A simple yet robust core banking backend system built with Spring Boot. It provides fundamental banking operations including user management, account handling, and transaction processing.

## Technologies

- **Java 25**
- **Spring Boot 4.0.1** (Web, Data JPA, Security, Validation)
- **Database:** PostgreSQL
- **Database Migrations:** Flyway
- **Authentication:** JWT (JSON Web Tokens)
- **API Documentation:** SpringDoc OpenAPI
- **Containerization:** Docker & Docker Compose
- **Build Tool:** Maven

## Features

- **User & Profile Management:** Register and manage bank users and their profiles.
- **Account Operations:** Create and maintain bank accounts (with PIN, Card Number, CVV, Balance).
- **Transactions:** Process deposits, withdrawals, and inter-account transfers.
- **Security:** Role-based access control and secure API endpoints using JWT.
- **Database Versioning:** Automated database schema migrations using Flyway.

## Prerequisites

- Java Development Kit (JDK) 25
- Maven
- Docker & Docker Compose

## Setup & Running Locally

1. **Environment Variables:**
   Create a `.env` file in the root directory based on the requirements of `compose.yaml`. You will need to set:

   ```env
   POSTGRES_USER=root
   POSTGRES_PASSWORD=password
   POSTGRES_DB=core_bank_system
   ```

2. **Start the Database:**
   The project includes a `compose.yaml` file to easily spin up PostgreSQL and Adminer (a database management tool on port 8081).

   ```bash
   docker-compose up -d
   ```

3. **Run the Application:**
   You can run the application using the Maven wrapper. Flyway will automatically run the database migrations on startup.

   ```bash
   ./mvnw spring-boot:run
   ```

4. **API Documentation (Swagger UI):**
   Once the application is running, you can explore and test the APIs via the SpringDoc OpenAPI user interface (typically available at `http://localhost:8080/swagger-ui.html` or `/swagger-ui/index.html`).

5. **Postman Collection:**
   A comprehensive Postman collection is available in the `postman/` directory (`generic_collection.json`) for testing the API endpoints.

## Database Schema Overview (Flyway Migrations)

- `V1`: `users` table
- `V2`: `profiles` table
- `V3`: `accounts` table (linked to users and enforcing unique account/card numbers)
- `V4`: `transactions` table (recording from/to accounts and amounts)
