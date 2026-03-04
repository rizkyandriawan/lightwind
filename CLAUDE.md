# Lightwind — AI Agent Context

## What Is This

Lightwind is a Quarkus-based CRUD framework (Java 17). It's a port of "Summer" (Spring Boot) to Quarkus, designed as a library that application developers extend via abstract base classes. The key insight: by controlling the entire convention (fixed base classes, fixed extension set, fixed DI pattern), the framework layer can be pre-compiled as a GraalVM native image layer, reducing native build time from 3-5 min to 15-30s.

## Architecture

```
lightwind/                          # Multi-module Maven project
├── pom.xml                         # Parent POM (pom packaging, Quarkus BOM 3.17.7)
├── lightwind-core/                 # Main library JAR
│   └── src/main/java/dev/kakrizky/lightwind/
│       ├── entity/                 # LightEntity<E,D>, SoftDeletable, Auditable
│       ├── crud/                   # LightCrudService<E,D>, LightCrudResource<E,D>
│       ├── query/                  # LightQueryEngine (JPA Criteria API, 13 operators)
│       ├── auth/                   # JWT auth, refresh tokens, social login (Google/GitHub)
│       │   ├── dto/                # LoginRequest, RegisterRequest, etc.
│       │   └── social/             # SocialAuthProvider interface + implementations
│       ├── audit/                  # AuditLog entity + AuditLogService
│       ├── filter/                 # RequestIdFilter (X-Request-Id correlation)
│       ├── exception/              # 7 exception types + LightExceptionMapper
│       ├── response/               # LightResponse<T>, PagedResult<T>, PageMeta
│       ├── dto/                    # RangeDto, DateRangeDto, IdNameDto, KeyValueDto
│       ├── config/                 # LightwindConfig (@ConfigMapping)
│       └── util/                   # BeanUtil (getter/setter based property copy)
├── lightwind-layer-cache/          # Redis caching + distributed lock
│   └── src/main/java/dev/kakrizky/lightwind/cache/
│       ├── LightCache.java         # @InterceptorBinding annotation
│       ├── LightCacheInterceptor   # CDI interceptor, Redis get/set
│       ├── LightCacheService       # Redis wrapper (get, put, evict, evictByPrefix)
│       ├── LightCacheConfig        # @ConfigMapping (enabled, defaultTtl, keyPrefix)
│       └── LightDistributedLock    # Redis SET NX EX based distributed lock
├── lightwind-layer-storage/        # File storage (local + S3/MinIO)
│   └── src/main/java/dev/kakrizky/lightwind/storage/
│       ├── StorageProvider.java    # Interface (upload, download, delete, presignedUrl)
│       ├── LocalStorageProvider    # Filesystem implementation
│       ├── S3StorageProvider       # AWS S3 / MinIO implementation
│       ├── LightStorageService     # Facade (provider selection, validation)
│       ├── StorageResource         # Abstract REST (download, delete, upload helper)
│       ├── StorageConfig           # @ConfigMapping (provider, s3*, maxFileSize)
│       └── FileInfo.java           # Record DTO (path, fileName, contentType, size, url)
├── lightwind-layer-email/          # Email sending with templates
│   └── src/main/java/dev/kakrizky/lightwind/email/
│       ├── LightMailService        # Send sync/async/bulk via Quarkus Mailer
│       ├── EmailRequest            # Builder pattern (to, subject, body, template, attach)
│       ├── EmailTemplate           # Static factory: welcome, resetPassword, verification
│       ├── EmailConfig             # @ConfigMapping (fromAddress, fromName, asyncEnabled)
│       └── templates/emails/       # Qute HTML templates (welcome, reset, verify)
├── lightwind-layer-scheduler/      # Background job queue
│   └── src/main/java/dev/kakrizky/lightwind/scheduler/
│       ├── LightJob.java           # Interface (getJobName, execute, getMaxRetries)
│       ├── JobRecord.java          # @Entity (lightwind_jobs table, persistent queue)
│       ├── JobSchedulerService     # Enqueue, cancel, list jobs
│       ├── JobProcessor            # @Scheduled(every="10s") polls and executes jobs
│       ├── JobResource             # REST API (/api/jobs — list, get, enqueue, cancel)
│       └── JobStatus.java          # Enum (PENDING, RUNNING, COMPLETED, FAILED, CANCELLED)
├── lightwind-layer-events/         # CDI events + outbox pattern
│   └── src/main/java/dev/kakrizky/lightwind/events/
│       ├── LightEvent.java         # Base event (eventType, payload, sourceId, correlationId)
│       ├── EventBus.java           # CDI event publisher (sync + async)
│       ├── LightEventListener      # @Qualifier for @Observes/@ObservesAsync
│       ├── OutboxEvent.java        # @Entity (lightwind_outbox table)
│       ├── OutboxService           # Save, poll, mark published/failed
│       ├── OutboxProcessor         # @Scheduled(every="5s") processes outbox
│       ├── EntityEventPublisher    # Helper for entity CRUD lifecycle events
│       └── OutboxStatus.java       # Enum (PENDING, PUBLISHED, FAILED)
├── lightwind-layer-realtime/        # WebSocket (STOMP) + SSE
│   └── src/main/java/dev/kakrizky/lightwind/realtime/
│       ├── RealtimeConfig           # @ConfigMapping (enabled, heartbeat, SSE reconnect)
│       ├── stomp/                   # STOMP 1.2 protocol layer
│       │   ├── StompFrame           # Frame POJO (command, headers, body)
│       │   ├── StompFrameCodec      # Parser + serializer
│       │   ├── StompWebSocketEndpoint # @WebSocket(path="/ws/stomp")
│       │   └── StompSessionHandler  # Per-connection state
│       ├── connection/              # Connection + subscription tracking
│       │   ├── ConnectionManager    # userId → WebSocket sessions
│       │   └── SubscriptionRegistry # destination → subscriptions
│       ├── messaging/
│       │   └── LightMessagingService # Main API: sendToUser(), broadcast()
│       ├── sse/
│       │   ├── SseConnectionManager # userId → SSE streams
│       │   └── LightSseResource     # Abstract base SSE endpoint
│       ├── auth/
│       │   └── WebSocketAuthenticator # JWT from STOMP CONNECT → userId
│       └── handler/
│           └── MessageHandler       # Interface for client→server messages
├── lightwind-build/                # Quarkus extension for layered native builds
│   ├── runtime/                    # NativeLayerConfig, LayerInfo
│   └── deployment/                 # LightwindProcessor (class scanning, reflection reg)
```

## Critical Implementation Details

### BeanUtil MUST use getter/setter methods
Quarkus Hibernate uses bytecode enhancement for dirty tracking. Direct `Field.set()` via reflection BYPASSES dirty tracking, causing updates not to persist. `BeanUtil` was explicitly rewritten to use `Method.invoke()` on getters/setters, falling back to field access only when no setter exists. **Never change this back to direct field access.**

### LightEntity uses private fields + explicit getters/setters
NOT Panache's public field style. This is required because:
1. `@Access(AccessType.FIELD)` is implicit via `@MappedSuperclass`
2. Hibernate dirty tracking needs setter interception
3. BeanUtil needs getters/setters to copy properties correctly

### @SQLRestriction goes on LightEntity, NOT on SoftDeletable interface
Hibernate `@SQLRestriction("deleted_at IS NULL")` is on the `@MappedSuperclass`, which means ALL subclass entities automatically filter soft-deleted records from standard queries. The `getAllIncludingDeleted()` method uses a separate query path that skips this filter.

### LightQueryEngine uses separate Root for count query
JPA Criteria API requires each query to have its own `Root<E>`. The count query can't reuse the data query's root. Predicates are rebuilt via `buildPredicates(cb, root)` helper method called with each query's own root.

### Type erasure in LightCrudResource
`List<D>` is erased to `List<Object>` at runtime. Bulk endpoints (`POST /bulk`, `DELETE /bulk`) accept `String` body and use `ObjectMapper` with `TypeFactory.constructCollectionType()` to deserialize correctly. The resource has an abstract `getDtoClass()` method for this purpose.

### LightRefreshToken is a concrete entity (not MappedSuperclass)
It has a fixed schema and references users by `UUID userId` (not by entity reference), avoiding the need to know the concrete user type.

### Optimistic locking
`@Version private Long version` on LightEntity. The `fillFromDto()` and `patchFromDto()` methods explicitly ignore "version" to prevent DTOs from overwriting the version counter.

## Conventions

### Package: `dev.kakrizky.lightwind`
GroupId: `dev.kakrizky`, domain: `lightwind.kakrizky.dev`

### Naming
- Entities: `LightEntity`, `LightAuthUser`, `LightRefreshToken`
- Services: `LightCrudService`, `LightAuthService`
- Resources: `LightCrudResource`, `LightAuthResource`
- Config: `LightwindConfig` (prefix `lightwind.*`)
- Build items: `LightwindClassesBuildItem`

### Test structure
Tests live in `lightwind-core/src/test/` with a test app (`TestItem`, `TestItemDto`, `TestItemService`, `TestItemResource`) that exercises the framework. H2 in-memory database, dev bypass auth enabled. RSA key pair in test resources for JWT signing.

### No Lombok
All classes use explicit getters/setters. This is intentional for GraalVM compatibility and to avoid Lombok-related build issues.

### Library JAR — no application.properties in main/resources
`application.properties` only exists in `src/test/resources/`. The library doesn't ship config — the consuming application provides all configuration.

## How to Extend (App Developer Pattern)

```java
// Entity → extends LightEntity<E, D>
// Service → extends LightCrudService<E, D>
// Resource → extends LightCrudResource<E, D>
// Auth User → extends LightAuthUser
// Auth Service → extends LightAuthService<U>
// Auth Resource → extends LightAuthResource<U>
```

## Key Files for Common Tasks

| Task | File |
|------|------|
| Add field to all entities | `entity/LightEntity.java` |
| Change query behavior | `query/LightQueryEngine.java` |
| Modify CRUD operations | `crud/LightCrudService.java` |
| Change REST endpoint behavior | `crud/LightCrudResource.java` |
| Modify error response format | `exception/LightExceptionMapper.java` |
| Change auth flow | `auth/LightAuthService.java` |
| Add social auth provider | `auth/social/SocialAuthProvider.java` (implement interface) |
| Modify response wrapper | `response/LightResponse.java` |
| Add build-time processing | `lightwind-build/deployment/LightwindProcessor.java` |
| Change config options | `config/LightwindConfig.java` |
| Add caching to a method | `lightwind-layer-cache/` — use `@LightCache` annotation |
| File upload/download | `lightwind-layer-storage/` — extend `StorageResource` |
| Send emails | `lightwind-layer-email/` — use `LightMailService` |
| Background jobs | `lightwind-layer-scheduler/` — implement `LightJob` interface |
| Publish events | `lightwind-layer-events/` — use `EventBus` or `EntityEventPublisher` |
| Full-text search | `lightwind-layer-search/` — use `LightSearchService`, `@Searchable` |
| Export data | `lightwind-layer-export/` — use `LightExportService`, `@ExportColumn` |
| REST client calls | `lightwind-layer-integration/` — use `LightRestClient` |
| Webhooks | `lightwind-layer-integration/webhook/` — use `WebhookService` |
| Circuit breaker | `lightwind-layer-integration/circuitbreaker/` — use `@LightCircuitBreaker` |
| WebSocket/STOMP | `lightwind-layer-realtime/` — use `LightMessagingService.sendToUser()` |
| SSE endpoints | `lightwind-layer-realtime/sse/` — extend `LightSseResource` |
| Handle WS messages | `lightwind-layer-realtime/handler/` — implement `MessageHandler` |

## Build & Test

```bash
mvn compile           # Compile all modules
mvn test              # Run all tests (50 tests)
mvn compile -q        # Quiet compile (errors only)
mvn test -pl lightwind-core  # Test only core module
```

## Filter Operators (QueryEngine)

```
field=value              → EQUALS
field__ne=value          → NOT_EQUAL
field__gt=value          → GREATER_THAN
field__gte=value         → GREATER_THAN_OR_EQUAL
field__lt=value          → LESS_THAN
field__lte=value         → LESS_THAN_OR_EQUAL
field__like=pattern      → LIKE
field__contains=text     → CONTAINS (wraps with %)
field__startswith=text   → STARTS_WITH
field__endswith=text     → ENDS_WITH
field__between=min,max   → BETWEEN
field__in=a,b,c          → IN
field__isnull=true       → IS_NULL
```

## Dependencies (lightwind-core)

- `quarkus-arc` (CDI)
- `quarkus-rest-jackson` (REST + JSON)
- `quarkus-hibernate-orm-panache` (ORM)
- `quarkus-smallrye-jwt` + `quarkus-smallrye-jwt-build` (JWT)
- `quarkus-elytron-security-common` (BCrypt)
- `quarkus-smallrye-openapi` (Swagger/OpenAPI)
- `quarkus-smallrye-health` (Health checks)
- `quarkus-flyway` (DB migration)
- Test: `quarkus-junit5`, `rest-assured`, `quarkus-jdbc-h2`

## What's NOT Implemented Yet

### Tier 1 gaps (nice-to-have for 0.1)
- Bulk update operation
- Micrometer metrics endpoint
- Multi-tenancy (column-based `tenant_id`)
- API versioning mechanism
- Permission-based ACL (`@HasPermission`)
- API key auth
- Rate limiting
- Database seeding
- Forgot/reset password (hooks exist, need email layer)

### Tier 2 (IMPLEMENTED — SaaS Ready)
- `lightwind-layer-cache` — Redis, @LightCache interceptor, LightCacheService, LightDistributedLock
- `lightwind-layer-storage` — Local + S3/MinIO via StorageProvider interface, file validation, presigned URLs
- `lightwind-layer-email` — Quarkus Mailer (sync/async/bulk), Qute templates, EmailTemplate helpers
- `lightwind-layer-scheduler` — Persistent JobRecord entity, LightJob interface, JobProcessor (10s poll), REST API
- `lightwind-layer-events` — CDI EventBus (sync/async), OutboxEvent entity, OutboxProcessor (5s poll), EntityEventPublisher

### Tier 3 (IMPLEMENTED — Enterprise)
- `lightwind-layer-search` — Elasticsearch client, `@Searchable`/`@SearchField` annotations, `LightSearchService`, `SearchResource`
- `lightwind-layer-export` — Excel (Apache POI), CSV (RFC 4180), PDF (OpenPDF), `@ExportColumn`, `LightExportService`
- `lightwind-layer-integration` — `LightRestClient` (JDK HttpClient), webhooks (HMAC-SHA256, delivery tracking), `@LightCircuitBreaker` interceptor

### Tier 4 (IMPLEMENTED — Realtime)
- `lightwind-layer-realtime` — STOMP 1.2 over WebSocket (`quarkus-websockets-next`), SSE, `LightMessagingService`, `MessageHandler` interface

### Not Yet Implemented
- `lightwind-layer-workflow` — state machine, approval flow
