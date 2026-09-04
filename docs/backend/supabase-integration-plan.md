# Supabase integration plan

## Scope and decision

This document is an architecture and migration plan only. It does not authorize
an implementation or a switch of production traffic.

The Android client is already backed by a Rust/Axum service. That service owns
OTP issuance, token rotation, tenant/schema routing, authorization,
maker-checker approvals, receipt creation and payment/move-out business rules.
Those operations must **not** be moved into Compose screens or direct mobile
database queries.

The safest first architecture is a hybrid:

```text
Compose UI
  -> ViewModel
  -> feature repository
  -> data source
       -> Rust API data source for protected commands and business views
       -> Supabase data source for approved direct reads, realtime, Auth, or Storage
  -> Supabase
```

In the target state, Supabase Postgres can be the managed database behind the
Rust API. Android direct Supabase access should begin only with low-risk,
read-only public discovery data or explicitly designed realtime subscriptions.
The Rust API remains the command boundary for payments, receipts, move-out,
inventory synchronization, role changes, audit entries, and admin actions.

This preserves the requested `UI -> ViewModel -> Repository -> Data Source ->
Supabase` shape without moving authorization-critical logic to the client.

## Current Android architecture

### UI and presentation

- Single Android application module: `app`.
- Jetpack Compose with Material 3; no XML layout screens were found.
- Navigation Compose routes the app through `navigation/AppRoot.kt` and
  `navigation/Destinations.kt`.
- Feature screens use Hilt ViewModels and `StateFlow` UI state. Examples include
  `feature/auth/LoginViewModel.kt`, `feature/rooms/RoomsViewModel.kt`,
  `feature/occupancy/OccupancyViewModel.kt`, and
  `feature/payments/PaymentsViewModel.kt`.
- `SessionManager` is the app-wide authentication/session source of truth.

### Pattern and data layer

The code is pragmatic feature-oriented MVVM with a repository pattern:

```text
Compose screen -> Hilt ViewModel -> feature repository -> ApiService -> Rust REST API
```

Repositories are currently concrete singleton classes, one per feature. They
translate screen-friendly inputs to network DTOs and return `ApiResult<T>`.
There is not yet a separate remote-data-source layer or repository interfaces.

### Networking and API clients

- Retrofit is the only current API client: `core/network/ApiService.kt`.
- OkHttp supplies bearer-token injection, HTTP logging in debug builds, timeouts,
  and a single-flight refresh-token authenticator.
- `NetworkExecutor` maps Retrofit/OkHttp failures and RFC-7807 server responses
  into `ApiResult` and `AppFailure`.
- Kotlinx serialization uses snake-case field names and ignores unknown fields.
- Public Explore endpoints, authenticated tenant/staff endpoints, and
  provisioning endpoints all share `ApiService`.

### Dependency injection and Gradle

- Hilt is the dependency-injection framework.
- `di/NetworkModule.kt` creates `Json`, Retrofit, bare/authenticated OkHttp
  clients, and the API service.
- `di/AppModule.kt` provides app scope and binds session invalidation.
- Gradle is a single module with a version catalog in `gradle/libs.versions.toml`.
- Current key dependencies are Compose, Navigation, Hilt, Retrofit, OkHttp,
  Kotlinx serialization, Coroutines, and DataStore. There is no Supabase,
  Ktor, Room, SQLite, Firebase, or GraphQL dependency.

### Authentication, organization, and role sources

- The app uses custom phone OTP through `AuthRepository` and REST endpoints.
- The Rust backend issues access/refresh JWTs; `TokenAuthenticator` rotates the
  refresh token after a 401.
- `AuthRepository.completeSignIn()` calls `/me` and creates a `UserSession` with
  `userId`, `orgId`, role, optional `branchId`, name, and verification state.
- Roles are modeled locally in `core/domain/model/Enums.kt`; the backend remains
  authoritative for owner, manager, tenant, prospect, and operator permissions.
- `BranchContext` keeps an in-memory selected branch; it is not a database.

### Local storage

- DataStore Preferences stores access/refresh tokens and the cached identity in
  `TokenStore`.
- DataStore also stores theme preference and an owner-only app PIN hash.
- No local relational database or offline entity cache exists.
- Receipt PDFs are downloaded through the API and exposed through `FileProvider`.

### Current entity/data origins

All business entities currently come from REST DTOs returned by the Rust API:

- Organization/user/role/branch: `/me`, `/branches`, session DTOs.
- Rooms, beds, residents, dues: branch/resident API endpoints.
- Payments, receipts, ledger, settlements: payment and receipt API endpoints.
- Complaints, notices, approvals, audit, away periods, enquiries: feature APIs.
- Public listings and vacancy: discovery APIs.

## Recommended Supabase architecture

### Phase A — Supabase as managed infrastructure behind the Rust API

Use Supabase Postgres as the database target of the existing Rust service.
Keep SQLx repositories, Axum routes, and existing API contracts. Move database
connection configuration from the current PostgreSQL host to the Supabase
database connection string. Use the Rust service for migrations and all writes.

Use Supabase Storage through a server-side storage adapter for receipt PDFs and
future documents. The Android app continues to download receipts from the
existing authorized API endpoint or receives a short-lived URL generated by the
backend.

Benefits:

- Lowest behavior-change risk.
- Existing authorization, audit, transaction, and maker-checker logic remains.
- Existing Android repositories and ViewModels need no initial rewrite.
- Supabase dashboard, backups, database tools, Storage, and later Realtime are
  available without placing a service-role key in the app.

### Phase B — introduce Android data-source boundaries

Before direct Supabase reads, split each repository's transport dependency:

```text
RoomRepository
  -> RoomApiDataSource        (current Retrofit implementation)
  -> RoomSupabaseDataSource   (future approved direct reads)
```

The repository chooses the source through Hilt bindings or a feature flag. Do
not expose Supabase clients to Compose screens or ViewModels.

Start with public Explore listings only. Keep staff and tenant records behind
the Rust API until RLS, authorization tests, observability, and rollback have
been proven.

### Phase C — optional Supabase Auth migration

Do not run custom backend JWT authentication and Supabase Auth as independent
authorities indefinitely. Choose one token authority per request path.

If Supabase Auth is adopted:

1. Configure phone OTP and email OTP in Supabase Auth.
2. Android uses a `SupabaseAuthDataSource`, never auth calls from a screen.
3. Backend verifies the Supabase JWT (`iss`, `aud`, signature/JWKS, expiry) and
   maps `auth.uid()` to the application user/profile.
4. Retire custom token issuance/refresh only after compatibility and migration
   testing pass.
5. Do not use a Supabase service-role key on Android.

For the immediate project, keeping backend-owned OTP and adding Supabase as
Postgres/Storage is safer than changing database and authentication at once.

## Proposed files

### Existing Android files to modify in a later implementation

| File | Planned change |
|---|---|
| `gradle/libs.versions.toml` | Add Supabase/Ktor versions only when a concrete phase is approved. |
| `app/build.gradle.kts` | Add Supabase dependencies and build-config loading; remove no Retrofit dependency in phase A. |
| `di/NetworkModule.kt` | Keep Retrofit bindings; add separate Supabase provider/module, not a replacement in-place. |
| `core/data/TokenStore.kt` | Preserve for current backend JWTs. Replace/migrate only when Supabase Auth becomes the token authority; use secure session storage. |
| `core/data/SessionManager.kt` | Consume an auth abstraction rather than assuming a custom `/me` flow, only in the Auth migration phase. |
| `feature/auth/AuthRepository.kt` | Delegate to an auth data source; retain current behavior until a cutover is approved. |
| Each `feature/*/*Repository.kt` | Introduce source interfaces and preserve the existing Retrofit implementation as the default. |
| `core/network/ApiService.kt` | Retain protected API endpoints. Remove endpoint use only after an endpoint has a tested, RLS-safe replacement. |

### New Android files/directories to create in a later implementation

```text
core/config/
  BackendConfig.kt
  SupabaseConfig.kt

core/supabase/
  SupabaseClientProvider.kt
  SupabaseAuthDataSource.kt
  SupabaseStorageDataSource.kt
  SupabaseErrorMapper.kt

feature/explore/data/
  ExploreDataSource.kt
  ExploreApiDataSource.kt
  ExploreSupabaseDataSource.kt

di/
  SupabaseModule.kt
```

Create only the files required by the approved phase. Do not add empty
abstractions for every feature up front.

### Backend changes needed in a later implementation

- Supabase Postgres connection configuration and migration strategy.
- Supabase Storage adapter for receipts/documents.
- Optional Supabase JWT verifier when Supabase Auth is adopted.
- RLS migration files, policy tests, and a restricted database role used by the
  Rust service.
- No mobile service-role key and no direct mobile access to privileged tables.

## What Supabase replaces and what remains

| Current mechanism | Supabase role | Decision |
|---|---|---|
| Rust/PostgreSQL database connection | Supabase Postgres | Replace infrastructure target in phase A; retain Rust repositories. |
| Local/server receipt object storage | Supabase Storage | Replace through backend adapter, not direct unrestricted mobile storage. |
| Retrofit protected commands | Supabase RPC/Edge Function only if justified | Keep initially. Payments, approvals, and move-out remain backend commands. |
| Public Explore listing read API | PostgREST/Realtme candidate | Optional first direct-read pilot after RLS testing. |
| Custom backend OTP/JWT | Supabase Auth candidate | Do not replace in initial database migration. |
| DataStore theme, branch selection, PIN | Local DataStore | Keep. |
| DataStore backend tokens | Keep initially; migrate only with Auth cutover | Do not store both unrelated sessions permanently. |
| Retrofit/OkHttp | Keep | Rust API remains needed for privileged workflows. |

## Environment management

### Android

- Put `SUPABASE_URL` and `SUPABASE_ANON_KEY` in a developer-local,
  git-ignored `supabase.properties` or CI secret injection.
- Generate `BuildConfig` values per debug/staging/release flavor.
- The anon key is publishable but must be scoped by RLS; it is not a secret.
- Never place `SUPABASE_SERVICE_ROLE_KEY`, database password, Twilio secret,
  SMTP secret, or private JWT signing key in Android code or BuildConfig.
- Keep `local.properties` untracked and add a checked-in
  `supabase.properties.example` with placeholders.

### Backend

- Store Supabase database URL, service role, storage credentials, and JWT
  verification settings in a production secret manager.
- Use typed startup config and fail fast for missing production values.
- Separate local, staging, and production Supabase projects.
- Rotate credentials and use least-privilege database roles.

## Multi-tenancy and Row Level Security

RLS is mandatory if Android will ever query Supabase directly.

Recommended data model:

```text
auth.users
  -> profiles(id = auth.uid())
  -> organization_memberships(user_id, org_id, role, branch_id?)
  -> branches(org_id)
  -> rooms(org_id, branch_id)
  -> beds(org_id, room_id)
  -> residents(org_id, branch_id, user_id?)
  -> payments/receipts/complaints/notices/... (org_id)
```

Policy rules:

- Every tenant-scoped table has a non-null `org_id`.
- Policies derive access from `auth.uid()` and membership rows, never from an
  `org_id` supplied by Android.
- Owner: organization-wide access within their membership organization.
- Manager: only assigned branch access.
- Tenant: only their own resident/receipt/ledger/away data plus approved
  organization/branch notices.
- Public Explore data: expose a narrow published-listings view, not base tables.
- Payments, audit, role changes, approval actions, and receipt readiness: no
  direct table write policy for Android; use backend/RPC with server validation.
- Storage policies scope receipt object paths by org and verify membership; use
  short-lived signed URLs where possible.

Use `security definer` functions sparingly. Pin `search_path`, validate caller
membership inside the function, use minimal grants, and test every function for
cross-organization access.

## Migration risks and mitigations

| Risk | Mitigation |
|---|---|
| Split-brain auth between custom JWTs and Supabase Auth | Keep one authority per phase; migrate tokens deliberately. |
| RLS policy accidentally exposes an organization | RLS integration tests for owner, manager, tenant, anonymous, and cross-org IDs before any direct client access. |
| Rust service bypasses RLS with powerful credentials | Use a narrowly scoped DB role where possible; enforce org context in repositories and retain service authorization tests. |
| Client starts bypassing business rules | Restrict direct mobile writes; keep backend/RPC command boundary. |
| Migrating existing schema/data breaks IDs or audit history | Rehearse on an anonymized copy; retain UUIDs; verify row counts, checksums, and foreign keys. |
| Existing tenant schemas/siloed organizations are not migrated | Inventory all schemas and explicitly test pooled and siloed routing before cutover. |
| Receipt URLs become public | Store private objects only; issue signed URLs after backend authorization. |
| Plain DataStore token storage | Use Android Keystore-backed encrypted storage during any auth/session migration. |
| Offline cache leaks previous user data | Partition cache by authenticated user/org and wipe it on sign-out/account switch. |

## Safe rollout plan

1. **Inventory and baseline** — document current REST endpoints, tables,
   authorization, and test coverage. No traffic changes.
2. **Supabase project setup** — separate development/staging projects, schema
   migration rehearsal, backups, monitoring, and restricted credentials.
3. **Backend database/storage pilot** — point a non-production backend at
   Supabase Postgres/Storage while Android continues using Retrofit unchanged.
4. **RLS and tenancy tests** — prove anonymous, tenant, manager, owner, and
   cross-org behavior with integration tests.
5. **Data-source pilot** — add a Supabase read-only source for public Explore
   under a feature flag; compare results and latency with the API.
6. **Optional Auth migration** — only after OTP, recovery, session revocation,
   user ID mapping, and backend JWT verification are tested.
7. **Incremental feature migration** — migrate only independent read/realtime
   features. Keep sensitive commands on the backend unless an audited RPC is
   clearly better.
8. **Production cutover and rollback** — metrics, audit trail, feature flags,
   rollback to the Rust API source, and a tested database restore procedure.

## Acceptance criteria before implementation

- A Supabase project/environment decision exists for dev, staging, and prod.
- The authoritative authentication strategy is chosen.
- RLS policies and cross-org tests are written before direct Android queries.
- No service-role credential appears in Android source, APK, or BuildConfig.
- All protected operations retain backend/RPC validation and audit behavior.
- Migration and rollback are rehearsed on non-production data.
- Existing Android flows, UI behavior, and API contracts remain functional.
