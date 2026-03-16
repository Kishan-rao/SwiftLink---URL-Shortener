# SwiftLink: High-Scale URL Shortener

SwiftLink is a production-grade, distributed URL shortening service designed for high throughput and low latency. It moves beyond simple shortening by implementing advanced distributed system patterns and modern Java features.

## 🚀 Tech Stack

### Backend
- **Java 21 (LTS):** Leveraging modern language features and performance.
- **Spring Boot 3.4:** For the core application framework and dependency management.
- **Project Loom / Virtual Threads:** Enabled via `spring.threads.virtual.enabled=true` to handle high-concurrency I/O efficiently.
- **Spring Security & JWT:** Stateless authentication for user management and secure link operations.

### Data Layer
- **AWS DynamoDB:** Used as the primary persistent storage for URL metadata and user data.
- **Redis:**
    - **Global Sequencer:** For generating unique ID blocks.
    - **Caching Layer:** For ultra-fast redirects (Hot-URL caching).
    - **Write-Behind Buffer:** For aggregating analytics data before persistence.

### Observability & Infrastructure
- **Prometheus & Grafana:** Comprehensive monitoring of throughput, latency, and business metrics.
- **Docker & Docker Compose:** Fully containerized setup for rapid deployment and local development.
- **Micrometer:** Custom metric collection for tracking link creations and click rates.

### Testing
- **JUnit 5:** Modern testing framework.
- **Testcontainers:** To run integration tests against real Redis and DynamoDB instances.

---

## 🛠 Key Functions & Features

### 1. URL Shortening & Management
- **Automatic Shortening:** Generates unique, short codes (Base62) for long URLs.
- **Custom Aliases:** Users can specify vanity URLs (e.g., `swiftlink.io/my-portfolio`).
- **Link Expiry (TTL):** Support for time-limited links that automatically become invalid after a set duration.
- **User Dashboard:** Manage links, view aggregate stats, and delete existing redirects.

### 2. High-Performance Analytics
- **Click Tracking:** Captures redirect counts in real-time.
- **Write-Behind Strategy:** Instead of hitting the DB on every click, counts are incremented in Redis and flushed to DynamoDB in asynchronous batches using Virtual Threads. This decouples user experience from DB latency.

### 3. Security & Access Control
- **JWT Auth:** Secure user registration, login, and protected management routes.
- **Public/Private Routes:** Transparent handling of public redirects vs. private administrative actions.
- **ID Obfuscation:** Uses a Linear Congruential Generator (LCG) approach for ID generation, creating non-sequential IDs that prevent competitors from guessing traffic volume.

### 4. Extra Features
- **QR Code Generation:** Every link automatically generates a corresponding high-resolution QR code.
- **Swagger Documentation:** Auto-generated API documentation for easy developer integration.
- **Responsive Web UI:** Clean, premium dark-mode interface built with Tailwind CSS.

---

## 🏗 Architectural Innovations

- **LCG-based ID Generation:** Formula [(seq * PRIME + SALT) % 2^64](file:///c:/Users/Kishan%20Rao/OneDrive/Desktop%201/Projects/spring-url-shortener-main/src/main/java/com/kishanrao/shortener/domain/url/UrlRepository.java#30-33) ensures unique, non-sequential, and collision-free IDs without requiring a database lookup for uniqueness check.
- **Virtual Thread Scaling:** The app can handle thousands of concurrent requests with a fraction of the memory footprint of traditional thread-per-request models.
- **Stateless Scale:** Designed to run in multiple instances behind a load balancer without shared session state.
