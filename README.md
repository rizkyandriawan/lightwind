# Lightwind

**Quarkus CRUD framework — build enterprise backends in minutes, not weeks.**

Lightwind is an opinionated, convention-over-configuration framework built on Quarkus. It provides pre-built base classes for entities, services, and REST resources with a powerful query engine, authentication, audit logging, and GraalVM native image support — so you can focus on business logic instead of boilerplate.

```
                        Build Time    Startup     RAM
                        ──────────    ───────     ───
Spring Boot (JVM)       5s            3-5s        300-500MB
Quarkus (native)        3-5 min       0.02s       30-50MB
Lightwind (native)      15-30s*       0.02s       30-50MB
                        ───────
                        * with layered native image
```

## Quick Start

### 1. Add dependency

**Maven:**
```xml
<dependency>
    <groupId>dev.kakrizky</groupId>
    <artifactId>lightwind-core</artifactId>
    <version>0.1.0</version>
</dependency>
```

**Gradle (Kotlin DSL):**
```kotlin
implementation("dev.kakrizky:lightwind-core:0.1.0")
```

### 2. Define your entity

```java
@Entity
@Table(name = "products")
public class Product extends LightEntity<Product, ProductDto> {

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column
    private Integer price;

    @Column
    private String category;

    @Override
    protected Class<ProductDto> getDtoClass() { return ProductDto.class; }

    @Override
    protected Class<Product> getEntityClass() { return Product.class; }

    // Define which fields are filterable via query params
    public static List<String> getFilterableFields() {
        return List.of("name", "price", "category", "createdAt");
    }

    // Define which fields are searchable via ?search=
    public static List<String> getSearchableFields() {
        return List.of("name", "description");
    }

    // Getters & setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    // ... etc
}
```

### 3. Create a DTO

```java
public class ProductDto {
    private UUID id;
    private String name;
    private String description;
    private Integer price;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters & setters (or use records)
}
```

### 4. Create a service

```java
@ApplicationScoped
public class ProductService extends LightCrudService<Product, ProductDto> {

    @Override
    protected Class<Product> getEntityClass() { return Product.class; }

    @Override
    protected Class<ProductDto> getDtoClass() { return ProductDto.class; }

    // Optional: add validation
    @Override
    protected List<ValidationError> validateCreate(ProductDto dto) {
        List<ValidationError> errors = new ArrayList<>();
        if (dto.getName() == null || dto.getName().isBlank()) {
            errors.add(new ValidationError("name", "Name is required"));
        }
        return errors;
    }

    // Optional: enable audit logging
    @Override
    protected boolean isAuditEnabled() { return true; }
}
```

### 5. Create a REST resource

```java
@Path("/api/products")
public class ProductResource extends LightCrudResource<Product, ProductDto> {

    @Inject
    ProductService service;

    @Override
    protected LightCrudService<Product, ProductDto> getService() { return service; }

    @Override
    protected Class<ProductDto> getDtoClass() { return ProductDto.class; }
}
```

**That's it.** You now have 11 REST endpoints with filtering, search, pagination, soft delete, audit logging, and more.

## What You Get

### REST Endpoints (auto-generated)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/products` | List with filters, search, pagination, sorting |
| `GET` | `/api/products/{id}` | Get single item |
| `GET` | `/api/products/deleted` | List soft-deleted items |
| `POST` | `/api/products` | Create |
| `POST` | `/api/products/bulk` | Bulk create (array body) |
| `PUT` | `/api/products/{id}` | Full update |
| `PATCH` | `/api/products/{id}` | Partial update (merge semantics) |
| `DELETE` | `/api/products/{id}` | Soft delete |
| `DELETE` | `/api/products/bulk` | Bulk soft delete (array of UUIDs) |
| `POST` | `/api/products/{id}/restore` | Restore deleted item |

### Query Engine (13 Filter Operators)

Django-style query parameters — no custom code needed:

```
GET /api/products?category=electronics          # exact match
GET /api/products?price__gte=100                # greater than or equal
GET /api/products?price__between=50,200         # range
GET /api/products?name__contains=widget         # substring match
GET /api/products?status__in=active,pending     # in list
GET /api/products?deletedAt__isnull=true        # null check
GET /api/products?search=gadget                 # full-text search across searchable fields
GET /api/products?page=2&size=20&sort=price,asc # pagination + sorting
```

All 13 operators: `eq` (default), `ne`, `gt`, `gte`, `lt`, `lte`, `like`, `contains`, `startswith`, `endswith`, `between`, `in`, `isnull`

### Response Format

```json
// Success (single)
{ "code": 200, "data": { "id": "...", "name": "Widget A", ... } }

// Success (paginated list)
{
  "code": 200,
  "data": {
    "items": [ ... ],
    "meta": {
      "currentPage": 1,
      "itemsPerPage": 10,
      "totalData": 42,
      "totalPages": 5
    }
  }
}

// Error
{
  "timestamp": "2026-03-04T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [{ "field": "name", "message": "Name is required" }],
  "requestId": "a1b2c3d4-..."
}
```

## Authentication

Lightwind includes a full JWT authentication system with refresh tokens, social login, and session management.

### Setup

Extend the base auth classes:

```java
// 1. User entity
@Entity
@Table(name = "users")
public class User extends LightAuthUser {
    private String firstName;
    private String lastName;
    // getters, setters
}

// 2. Auth service
@ApplicationScoped
public class AuthService extends LightAuthService<User> {

    @Override
    protected Class<User> getUserClass() { return User.class; }

    @Override
    protected Set<String> getDefaultRoles() { return Set.of("USER"); }
}

// 3. Auth resource
@Path("/auth")
public class AuthResource extends LightAuthResource<User> {

    @Inject AuthService authService;

    @Override
    protected LightAuthService<User> getAuthService() { return authService; }
}
```

### Auth Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/auth/register` | Register with email/password |
| `POST` | `/auth/login` | Login → access + refresh token |
| `POST` | `/auth/refresh` | Refresh access token |
| `POST` | `/auth/logout` | Revoke refresh token |
| `POST` | `/auth/social/{provider}` | Social login (google, github) |
| `POST` | `/auth/forgot-password` | Request password reset |
| `POST` | `/auth/reset-password` | Reset with token |

### Social Login (Google & GitHub)

Configure in `application.properties`:

```properties
# Google
lightwind.auth.social.google.client-id=your-google-client-id

# GitHub
lightwind.auth.social.github.client-id=your-github-client-id
lightwind.auth.social.github.client-secret=your-github-client-secret
```

Frontend flow:
1. User clicks "Login with Google/GitHub" → gets token/code from provider
2. Frontend sends token/code to `POST /auth/social/google` or `POST /auth/social/github`
3. Backend verifies, creates/links user, returns JWT token pair

### Custom Social Providers

Implement `SocialAuthProvider` for any OAuth2 provider:

```java
@ApplicationScoped
public class AppleAuthProvider implements SocialAuthProvider {
    @Override
    public String getProviderName() { return "apple"; }

    @Override
    public SocialUser verify(SocialLoginRequest request) {
        // Verify Apple token, return SocialUser
    }
}
```

### RBAC

Lightwind JWT tokens include role claims. Use Quarkus's built-in `@RolesAllowed`:

```java
@GET
@Path("admin/dashboard")
@RolesAllowed("ADMIN")
public Response adminDashboard() { ... }
```

## Entity Features

### Built-in Fields

Every entity extending `LightEntity` automatically gets:

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Auto-generated primary key |
| `createdAt` | LocalDateTime | Auto-set on creation |
| `updatedAt` | LocalDateTime | Auto-updated on modification |
| `createdById` | UUID | User who created |
| `createdByName` | String | User display name |
| `updatedById` | UUID | User who last updated |
| `updatedByName` | String | User display name |
| `deletedAt` | LocalDateTime | Soft delete timestamp |
| `deletedById` | UUID | User who deleted |
| `deletedByName` | String | User display name |
| `version` | Long | Optimistic locking counter |

### Soft Delete

Soft-deleted records are automatically filtered from all queries via `@SQLRestriction("deleted_at IS NULL")`. Use `GET /deleted` to view them, and `POST /{id}/restore` to bring them back.

### Optimistic Locking

The `@Version` field prevents concurrent update conflicts. If two users try to update the same record simultaneously, the second one gets an `OptimisticLockException`.

### Audit Logging

Enable per-service to track all changes:

```java
@Override
protected boolean isAuditEnabled() { return true; }
```

Records are stored in `lightwind_audit_log` with:
- Entity type and ID
- Action (CREATE, UPDATE, DELETE, RESTORE)
- Previous and new values (JSON)
- User who made the change
- Request ID for correlation

### Lifecycle Hooks

Override these in your service for custom behavior:

```java
// Validation
protected List<ValidationError> validateCreate(D dto) { ... }
protected List<ValidationError> validateUpdate(D dto) { ... }

// Lifecycle
protected void beforeCreate(D dto, E entity) { ... }
protected void afterCreate(E entity) { ... }
protected void beforeUpdate(D dto, E entity) { ... }
protected void afterUpdate(E entity) { ... }
protected void beforeRemove(E entity) { ... }
```

## Configuration

### application.properties

```properties
# --- Database ---
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/mydb
quarkus.datasource.username=postgres
quarkus.datasource.password=postgres
quarkus.hibernate-orm.database.generation=update

# --- Flyway (optional, for production) ---
quarkus.flyway.migrate-at-start=true

# --- JWT ---
mp.jwt.verify.publickey.location=publickey.pem
smallrye.jwt.sign.key.location=privatekey.pem
lightwind.auth.issuer=my-app
lightwind.auth.token-expiration=900          # 15 minutes (access token)
lightwind.auth.refresh-token-expiration=2592000  # 30 days

# --- Dev mode ---
lightwind.dev-mode=true
lightwind.auth.bypass-enabled=true           # Skip JWT validation in dev

# --- CORS ---
quarkus.http.cors=true
quarkus.http.cors.origins=http://localhost:3000
quarkus.http.cors.methods=GET,POST,PUT,PATCH,DELETE,OPTIONS
quarkus.http.cors.headers=Content-Type,Authorization,X-Request-Id
quarkus.http.cors.exposed-headers=X-Request-Id

# --- Social Auth ---
lightwind.auth.social.google.client-id=your-id
lightwind.auth.social.github.client-id=your-id
lightwind.auth.social.github.client-secret=your-secret

# --- OpenAPI ---
quarkus.smallrye-openapi.path=/openapi
quarkus.swagger-ui.always-include=true
```

## Built-in Endpoints

In addition to your CRUD endpoints:

| Path | Description |
|------|-------------|
| `/q/health` | Health check |
| `/q/health/live` | Liveness probe |
| `/q/health/ready` | Readiness probe |
| `/q/openapi` | OpenAPI spec (JSON/YAML) |
| `/q/swagger-ui` | Swagger UI |

## Layered Native Image Build

Lightwind includes a Quarkus extension (`lightwind-build`) that enables faster GraalVM native builds by pre-compiling the framework layer.

```bash
# Standard native build (3-5 minutes)
mvn package -Pnative

# Build base layer (once, for CI)
mvn package -Pnative-base-layer

# Build app with pre-built base layer (15-30 seconds)
mvn package -Pnative-layered -Dlightwind.native.layer.path=~/.lightwind/layers/lightwind-base.nil
```

The `lightwind-build` extension automatically:
- Scans for your entity, service, and resource classes at build time
- Registers them for GraalVM reflection
- Configures the native image layering

## Project Structure

```
your-app/
├── src/main/java/com/example/
│   ├── entity/
│   │   └── Product.java          # extends LightEntity<Product, ProductDto>
│   ├── dto/
│   │   └── ProductDto.java
│   ├── service/
│   │   └── ProductService.java   # extends LightCrudService<Product, ProductDto>
│   ├── resource/
│   │   └── ProductResource.java  # extends LightCrudResource<Product, ProductDto>
│   └── auth/
│       ├── User.java             # extends LightAuthUser
│       ├── AuthService.java      # extends LightAuthService<User>
│       └── AuthResource.java     # extends LightAuthResource<User>
├── src/main/resources/
│   ├── application.properties
│   ├── publickey.pem
│   └── privatekey.pem
└── pom.xml
```

## Optional Layers

Lightwind provides additional modules for common SaaS needs. Add them as dependencies:

| Module | Maven | Gradle |
|--------|-------|--------|
| Cache | `dev.kakrizky:lightwind-layer-cache:0.1.0` | `implementation("dev.kakrizky:lightwind-layer-cache:0.1.0")` |
| Storage | `dev.kakrizky:lightwind-layer-storage:0.1.0` | `implementation("dev.kakrizky:lightwind-layer-storage:0.1.0")` |
| Email | `dev.kakrizky:lightwind-layer-email:0.1.0` | `implementation("dev.kakrizky:lightwind-layer-email:0.1.0")` |
| Scheduler | `dev.kakrizky:lightwind-layer-scheduler:0.1.0` | `implementation("dev.kakrizky:lightwind-layer-scheduler:0.1.0")` |
| Events | `dev.kakrizky:lightwind-layer-events:0.1.0` | `implementation("dev.kakrizky:lightwind-layer-events:0.1.0")` |
| Search | `dev.kakrizky:lightwind-layer-search:0.1.0` | `implementation("dev.kakrizky:lightwind-layer-search:0.1.0")` |
| Export | `dev.kakrizky:lightwind-layer-export:0.1.0` | `implementation("dev.kakrizky:lightwind-layer-export:0.1.0")` |
| Integration | `dev.kakrizky:lightwind-layer-integration:0.1.0` | `implementation("dev.kakrizky:lightwind-layer-integration:0.1.0")` |

### Cache (`lightwind-layer-cache`)

Redis-based caching with annotation-driven cache + distributed locks.

```java
@LightCache(key = "products", ttl = 300)
public List<Product> getPopularProducts() { ... }

// Programmatic cache
@Inject LightCacheService cacheService;
cacheService.put("key", value, 600);
cacheService.evictByPrefix("products:");

// Distributed lock
@Inject LightDistributedLock lock;
if (lock.tryLock("process-orders", 30)) {
    try { /* critical section */ }
    finally { lock.unlock("process-orders"); }
}
```

### Storage (`lightwind-layer-storage`)

File storage abstraction — local filesystem or S3/MinIO, switchable via config.

```properties
lightwind.storage.provider=s3          # or "local"
lightwind.storage.s3.bucket=my-bucket
lightwind.storage.s3.endpoint=http://localhost:9000  # MinIO
lightwind.storage.max-file-size=10485760             # 10MB
```

```java
@Inject LightStorageService storage;
FileInfo info = storage.upload("uploads/photo.jpg", inputStream, "image/jpeg", size);
String url = storage.generatePresignedUrl("uploads/photo.jpg");
```

### Email (`lightwind-layer-email`)

Email sending via Quarkus Mailer with Qute templates and built-in email templates.

```java
@Inject LightMailService mailService;

// Simple send
mailService.send(EmailRequest.builder()
    .to("user@example.com")
    .subject("Welcome!")
    .template("welcome", Map.of("name", "Alice", "loginUrl", "/login"))
    .build());

// Built-in templates
mailService.send(EmailTemplate.welcome("user@example.com", "Alice", "/login"));
mailService.send(EmailTemplate.resetPassword("user@example.com", "Alice", "/reset?token=abc"));
```

### Scheduler (`lightwind-layer-scheduler`)

Persistent background job queue with retry support and REST monitoring API.

```java
// Define a job
@ApplicationScoped
public class SendReportJob implements LightJob {
    public String getJobName() { return "send-report"; }
    public String execute(String payload) throws Exception {
        // process payload JSON, return result JSON
    }
}

// Enqueue
@Inject JobSchedulerService scheduler;
UUID jobId = scheduler.enqueue("send-report", "{\"reportId\": \"123\"}");

// Schedule for later
scheduler.enqueue("send-report", payload, LocalDateTime.now().plusHours(1));
```

REST API at `/api/jobs` — list, get, enqueue, cancel.

### Events (`lightwind-layer-events`)

CDI event bus with outbox pattern for reliable event publishing.

```java
// Publish events
@Inject EventBus eventBus;
eventBus.publish(new LightEvent("order.created", orderJson, orderId, "Order"));
eventBus.publishAsync(event);  // non-blocking

// Listen for events
public void onOrderCreated(@Observes @LightEventListener LightEvent event) {
    if ("order.created".equals(event.getEventType())) { ... }
}

// Entity lifecycle events (auto-publish CRUD events)
@Inject EntityEventPublisher publisher;
publisher.entityCreated("Product", productId, productDto);

// Outbox pattern (reliable cross-service events)
@Inject OutboxService outbox;
outbox.save("order.created", "order-service", orderPayload);
// OutboxProcessor polls every 5s and publishes pending events
```

Configure outbox: `lightwind.events.outbox.enabled=true`

### Search (`lightwind-layer-search`)

Elasticsearch integration with annotation-driven indexing.

```java
@Searchable(index = "products")
public class ProductDto {
    @SearchField(type = SearchFieldType.TEXT, boost = 2.0f)
    private String name;

    @SearchField(type = SearchFieldType.KEYWORD)
    private String category;

    @SearchField(type = SearchFieldType.FLOAT)
    private float price;
}

// Index & search
@Inject LightSearchService searchService;
searchService.index("products", productId, productDto);

SearchResult<Map<String, Object>> results = searchService.search("products",
    SearchRequest.builder()
        .query("wireless headphones")
        .filter("category", "electronics")
        .page(0).size(20)
        .highlight(true)
        .build());
```

### Export (`lightwind-layer-export`)

Export data to Excel (XLSX), CSV, or PDF with annotation-driven column definitions.

```java
public class OrderDto {
    @ExportColumn(header = "Order #", order = 1)
    private String orderNumber;

    @ExportColumn(header = "Customer", order = 2)
    private String customerName;

    @ExportColumn(header = "Total", order = 3, format = "#,##0.00")
    private BigDecimal total;

    @ExportColumn(header = "Date", order = 4, format = "yyyy-MM-dd")
    private LocalDateTime createdAt;
}

@Inject LightExportService exportService;

// Export to Excel
byte[] xlsx = exportService.export(orders, ExportRequest.builder()
    .fileName("orders")
    .format(ExportFormat.XLSX)
    .title("Order Report")
    .build());

// Or get a JAX-RS Response with proper headers
Response response = exportService.exportAsResponse(orders, request);
```

### Integration (`lightwind-layer-integration`)

REST client, webhook management, and circuit breaker pattern.

```java
// REST client
@Inject LightRestClient restClient;
UserDto user = restClient.get("https://api.example.com/users/1", UserDto.class);
OrderDto created = restClient.post("https://api.example.com/orders", orderDto, OrderDto.class);

// Webhooks — register & dispatch
@Inject WebhookService webhookService;
webhookService.register("Order Events", "https://partner.com/webhook",
    "order.created,order.updated", "signing-secret");
webhookService.dispatch("order.created", orderData);  // sends to all subscribers

// Circuit breaker
@LightCircuitBreaker(name = "payment-api", failureThreshold = 3)
public PaymentResult processPayment(PaymentRequest req) {
    return restClient.post("https://payment.example.com/charge", req, PaymentResult.class);
}
```

REST API at `/api/webhooks` — register, list, unregister, test, view deliveries.

## Tech Stack

- **Runtime**: Quarkus 3.17.7
- **Language**: Java 17+
- **ORM**: Hibernate ORM with Panache
- **REST**: RESTEasy Reactive + Jackson
- **Auth**: SmallRye JWT + BCrypt
- **Migration**: Flyway
- **API Docs**: SmallRye OpenAPI (Swagger)
- **Health**: SmallRye Health
- **Native**: GraalVM with layered image support
- **Cache**: Quarkus Redis (optional layer)
- **Storage**: AWS S3 SDK (optional layer)
- **Email**: Quarkus Mailer + Qute (optional layer)
- **Scheduler**: Quarkus Scheduler (optional layer)
- **Events**: CDI Events + Outbox (optional layer)
- **Search**: Elasticsearch 8.x (optional layer)
- **Export**: Apache POI + OpenPDF (optional layer)
- **Integration**: JDK HttpClient, webhooks, circuit breaker (optional layer)

## Requirements

- Java 17+
- Maven 3.8+
- A database (PostgreSQL recommended, H2 for dev/testing)

## License

MIT

## Links

- **Website**: https://lightwind.kakrizky.dev
- **GitHub**: https://github.com/rizkyandriawan/lightwind
