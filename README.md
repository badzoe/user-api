# User API – Java Practical Assessment

This project is a RESTful User Management API developed as part of a Java practical assessment.

---

## Tech Stack

* Java 17
* Spring Boot
* Gradle
* Spring Data JPA
* Spring Security
* H2 In-Memory Database

---

## How to Run the Application

### Prerequisites

* Java 17 installed

### Steps

1. Navigate to the project root directory

2. Run the application using Gradle:

   ```bash
   ./gradlew bootRun
   ```

3. The application will start on:

   ```
   http://localhost:8080
   ```

---

## Database

* In-memory H2 database is used
* No external database setup required
* H2 Console:

  ```
  http://localhost:8080/h2-console
  ```
* JDBC URL:

  ```
  jdbc:h2:mem:userdb
  username: sa
  password: password
  ```

---

## API Context Root

All API endpoints are available under:

```
/api
```

---

## Authentication & Security

* Users authenticate via `/api/login`
* Successful login returns a Base64-encoded session token
* The token must be sent in the `Authorization` header for secured endpoints
* Passwords are encrypted using BCrypt
* Session tokens expire after 3 minutes of inactivity

Example header:

```
Authorization: Basic <session_token>
```

---

## Available Endpoints

### Create User

```
PUT /api/users
```

### Login

```
POST /api/login
```

### List Users (Authenticated)

```
GET /api/users
```

### Logout

```
POST /api/logout/{id}
```

---

## Client

A Postman collection is included to demonstrate usage of the API.

Location:

```
/postman/User-API.postman_collection.json
```

---

## Notes

* Only authenticated users can access protected endpoints
* The API follows the provided contract, with minor internal adjustments documented here
* The application is fully self-contained and portable
