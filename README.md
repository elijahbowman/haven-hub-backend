# HavenHub backend

Spring Boot backend of HavenHub

[Angular Frontend](https://github.com/elijahbowman/haven-hub-frontend)

### Key Features
- Booking management for travelers
- Landlord reservation management
- Search for houses by criteria (location, date, guests, beds, etc)
- Authentication and Authorization (Role management) with Auth0 (OAuth2)
- Domain-driven design

### Tech stack
- Spring Boot 3
- Angular 17
- PrimeNG
- PostgreSQL
- Auth0 (2025)

## Usage
### Prerequisites
- [JDK 21](https://adoptium.net/temurin/releases/)
- [PostgreSQL](https://www.postgresql.org/download/)
- IDE ([VSCode](https://code.visualstudio.com/download), [IntelliJ](https://www.jetbrains.com/idea/download/))

### Clone the repository
``git clone https://github.com/elijahbowman/haven-hub-backend``

### Launch
#### Maven
``./mvnw spring-boot:run  -Dspring-boot.run.arguments="--AUTH0_CLIENT_ID=<client-id> --AUTH0_CLIENT_SECRET=<client-secret>"``

#### IntelliJ
Go in IntelliJ add the environment variables and then run it.

---

## Production Scaling Roadmap & Known Architectural Trade-offs

This application was intentionally built as an decoupled, self-contained single-repository prototype to optimize for portfolio evaluation without introducing high cloud infrastructure costs or specialized cluster dependencies. However, a production-grade Homestay Brokerage engine operating at scale must survive high concurrency, maintain tight security boundaries, and remain cost-effective. 

Below is an explicit engineering analysis of the current architectural shortcuts and the corresponding technical blueprint designed to graduate HavenHub to production readiness.

### 1. Concurrency, Distributed Transactions & Booking Race Conditions
* **The Shortcut:** The current booking confirmation engine inside `BookingService.java` relies on a synchronous "Check-Then-Act" pipeline (`bookingExistsAtInterval` validation followed by a standard `bookingRepository.save`). 
* **The Scale Risk:** Under high concurrency (e.g., thousands of parallel transactions matching on seasonal inventory), this design introduces a classic race condition. Two separate application threads can discover a false negative availability flag simultaneously before committing, causing severe data corruption in the form of **double bookings**.
* **Production Remedy:**
    * **Optimistic Locking:** Introduce an `@Version` concurrency control column to tracking rows within our PostgreSQL schema for low-contention paths.
    * **Distributed Sharded Locks:** Implement an distributed locking abstraction utilizing **Redis (via Redisson)** or **ShedLock** keyed explicitly to the `listingPublicId` cluster block. This serializes booking confirmation steps safely across horizontally scaled, stateless backend micro-containers without creating a database I/O bottleneck.

### 2. Media Management, Memory Footprint & Database Abuse
* **The Shortcut:** Property images are parsed directly as raw `byte[]` arrays inside the application layer and persisted to PostgreSQL utilizing binary Large Object blocks (`${blobType}` in the Liquibase schema).
* **The Scale Risk:** Storing multiple high-resolution graphics files directly inside transactional relational tables causes rapid database bloating, severely degrades database backup and restore speeds, and floods application server heap memory profiles during entity mapping cycles.
* **Production Remedy:**
    * Migrate to an **asynchronous decoupled object storage architecture** utilizing cloud bucket topology (e.g., AWS S3, Google Cloud Storage, or MinIO).
    * Refactor the backend controller to generate short-lived, cryptographically secure **Presigned Upload URLs**. The Angular client will then stream binary files directly to cloud object storage from the browser, keeping the Spring Boot instance stateless and tracking only lightweight deterministic string storage references in the relational database.

### 3. Identity Provider Couplings & Boundary Violations
* **The Shortcut:** When a user transitions state to host a property, `Auth0Service.java` executes a blocking outbound HTTP transaction directly to the Auth0 Management API to map the administrative `ROLE_LANDLORD` value to the remote user profile.
* **The Scale Risk:** This architectural tight coupling introduces an external third-party infrastructure dependency directly inside our primary core domain transaction boundary. A network blip, API outage, or rate-limiting trip on Auth0's endpoint will cause local property creation events to hard-fail immediately.
* **Production Remedy:**
    * Isolate the external Identity Provider (IdP) boundary. Treat Auth0 strictly as an authorization and identity validation gateway.
    * Maintain role-based authorization scopes natively inside the application's local database tables (`user_authority`). Elevate local customer status instantly on property creation, and defer down-stream external token updates asynchronously utilizing an isolated queue worker or webhook-driven transactional outbox pattern.

### 4. Input Sanitization, CSRF & Cross-Site Scripting (XSS) Gaps
* **The Shortcut:** Cookie-based Cross-Site Request Forgery (CSRF) tracking is enabled on our Spring Security filter chain utilizing the standard configuration `.withHttpOnlyFalse()`. Additionally, rich textual user data (e.g., property descriptions) is saved without mutation.
* **The Scale Risk:** Setting HTTP-Only flags to false allows SPA client scripts to read validation tokens directly. If a landlord successfully injects a malicious script string into an un-sanitized markdown description field, viewing tenants are exposed to critical XSS token hijacking vectors.
* **Production Remedy:**
    * Implement explicit server-side validation and rich HTML sanitization utilizing an aspect parser like **jsoup** inside our entity interceptor tier.
    * Enforce strict runtime **Content Security Policies (CSP)** headers across our API edge proxies to block cross-site execution paths completely.

### 5. High-Throughput Sequence Bottlenecks
* **The Shortcut:** Database sequences generated via Liquibase scripts currently default to an `incrementBy="1"` allocation tracking structure.
* **The Scale Risk:** For every row written during high-frequency intervals, stateless application server containers are forced to execute a blocking round-trip network ping to PostgreSQL to pull a unique identifier value.
* **Production Remedy:** Change the sequence tracking bounds to a bulk memory allocation layout (`incrementBy="50"`) mapped tightly to Hibernate’s **pooled-lo optimizer** settings, saving vast amounts of network overhead on sequential inserts.

### 6. Frontend Resiliency & State Immutability
* **The Shortcut:** The Angular service tier subscribes directly to network observables without fallback middleware or data protection patterns, while list modifications (such as cancels) apply raw JavaScript mutations like `splice` on local component reference memory.
* **The Scale Risk:** Spotty mobile carrier client connectivity or an ephemeral packet drop immediately translates into a crashing application state for the user. Direct list pointer mutation can also trigger subtle template change-detection rendering failures.
* **Production Remedy:**
    * Refactor client operations into immutable functional pipelines (utilizing `filter`, `map`, and the object spread operator) to provide transparent and predictable Signal reactivity.
    * Wrap external endpoints inside highly resilient RxJS pipe operations utilizing robust operators like `retry({ count: 3, delay: 1000 })` and uniform `catchError` handlers.

### Architectural Risk & Priority Matrix

The following ledger ranks the identified architectural shortcuts by their severity, impact on system stability, and production remediation priority:

| Priority | Architectural Gap | Risk Classification | Business Impact | Primary Remediation Strategy |
| :---: | :--- | :--- | :--- | :--- |
| **P0** | Booking Race Conditions (`BookingService.java`) | **Data Integrity / High Concurrency Failure** | Overlapping double-bookings; severe customer trust loss and financial friction. | Distributed Lock (Redis/Redisson) on `listingPublicId` or Database Pessimistic Locking. |
| **P0** | Manually Parsed Jackson validation (`LandlordResource.java`) | **Security / Vulnerability** | Information disclosure via stack trace leaks; potential validation bypass. | Standardize with native Spring `@Valid` payloads and a global `@ControllerAdvice` handler. |
| **P1** | Media Asset Storage as Database BLOBs (`listing_picture`) | **Performance Degradation / High Cost** | Rapid database bloat, expensive read/write I/O, heavy memory profiles, un-cachable assets. | Asynchronous cloud storage integration via AWS S3/MinIO with **Presigned Upload URLs**. |
| **P1** | Blocking IdP Role Synchronization (`Auth0Service.java`) | **System Availability / Coupling Fault** | Cascading downtime. If Auth0 drops or limits rates, core system actions (listing properties) fail. | Decouple sync boundary into an asynchronous queue or transaction outbox pattern. |
| **P2** | In-Memory Array Collection Mutations (`splice`) | **UI Consistency / Rendering Flaws** | Broken Angular template change-detection cycles; erratic UI states on deletion. | Refactor frontend array filters to maintain **Strict Data Immutability**. |
| **P2** | Monolithic Sequence Counter Allocations (`incrementBy="1"`) | **Database Performance Bottleneck** | Heavy network chatter to PostgreSQL under write-heavy loads. | Modify Liquibase sequence tracking blocks to `incrementBy="50"` with **pooled-lo** optimization. |
| **P2** | Missing Network Layer Fault Tolerance | **Resiliency Failure** | Flaky mobile data connections trigger immediate application hard-failures for tenants. | Wrap Angular HTTP Observable flows inside structured **RxJS `retry` and `catchError` pipes**. |
