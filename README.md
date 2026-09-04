# Aalaya — Android App (Kotlin & Jetpack Compose)

Modern, native Android client for the **Aalaya PG Management System**. Built with Jetpack Compose (Material 3), MVVM, Dagger Hilt, Retrofit + Kotlinx Serialization, and a custom design system tailored for fast, reliable property management.

---

## 🌟 Key Features

### For Owners & Managers
- **Staff Dashboard**: Real-time revenue analytics, collection totals, deposit tracking, and occupancy metrics across organizations and individual branches.
- **Branch & Room Inventory**: Manage branches, multi-tier rooms, and individual bed assignments with real-time occupancy status chips.
- **Resident Management**: Onboard residents, assign plans (daily, weekly, monthly), track rent ledgers, record UPI/Cash/Card payments, and generate branded PDF receipts.
- **Approvals & Maker-Checker**: Tiered operational controls ensuring critical manager operations (deletions, modifications) pass through owner approval.
- **Notices & Staff Invites**: Publish organization/branch notices and securely invite staff with one-time verification codes.
- **Receipt Template Editor**: Customize receipt branding with property details, custom accents, and terms directly from the app.
- **Public Explore Listings**: Manage public-facing branch listings with amenities, pricing, and submission workflows.

### For Residents / Tenants
- **Tenant Portal**: Personal ledger view, rent payment status, downloadable PDF receipts, and accommodation details.
- **Complaints System**: File maintenance and service tickets with status updates through a state-machine workflow.
- **Notice Board**: View real-time branch and organization-wide announcements.
- **Identity Verification**: Integrated DigiLocker and document verification flow for quick, seamless onboarding.
- **Public Property Discovery**: Search and explore nearby verified PGs with haversine distance filtering and details.

---

## 🛠 Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/) (Coroutines + StateFlow)
- **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 Design
- **Architecture**: MVVM + Clean Architecture (Domain, Data, Feature layers)
- **Dependency Injection**: [Dagger Hilt](https://dagger.dev/hilt/)
- **Networking**: [Retrofit 2](https://square.github.io/retrofit/) + [OkHttp 3](https://square.github.io/okhttp/) with custom `TokenAuthenticator` (automatic 401 token refresh)
- **Serialization**: `kotlinx.serialization`
- **Local Persistence**: Jetpack DataStore (TokenStore & SessionManager)
- **Document Handling**: Android Intent PDF Viewer + secure FileProvider

---

## 📐 Architecture Overview

The codebase is organized cleanly into modular layers designed to mirror the backend domain:

```
app/src/main/java/com/hamsavilas/pg/
├── core/
│   ├── common/          # Transport-agnostic ApiResult & AppFailure hierarchy
│   ├── network/         # Retrofit ApiService, DTOs, AuthInterceptor & TokenAuthenticator
│   ├── data/            # TokenStore (DataStore) and SessionManager auth state
│   ├── domain/model/    # Domain entities (Role, PlanKind, ComplaintState, etc.)
│   └── util/            # Money utilities (paise ↔ ₹ formatting)
├── di/                  # Hilt modules (network, repositories, application scope)
├── feature/             # Feature modules (ViewModel, Repository, UI screens)
│   ├── auth/            # Phone/Email OTP login flow
│   ├── dashboard/       # Financial & occupancy metrics
│   ├── branches/        # Branch creation and branch management
│   ├── residents/       # Resident onboarding, ledger, payments, move-out
│   ├── rooms/           # Rooms and bed-level occupancy
│   ├── tenant/          # Tenant self-service portal
│   ├── complaints/      # Issue reporting and state transition engine
│   ├── notices/         # Announcement broadcast & viewing
│   ├── approvals/       # Maker-checker change requests
│   ├── invites/         # Staff invitation code generator
│   ├── explore/         # Public PG discovery & location search
│   └── receipts/        # Shared PDF downloader and opener
├── navigation/          # Navigation graph, role-based tabs, and session gate
└── ui/
    ├── theme/           # Design tokens: Colors, Inter/Manrope Typography, Dimens
    └── components/      # Custom reusable Compose widgets (Cards, Chips, Buttons)
```

### Architectural Principles
- **Single Source of Truth**: `SessionManager` manages the active session state; `AppRoot` handles navigation between authentication and authenticated screens without manual routing.
- **Transparent Token Refresh**: HTTP 401 responses trigger `TokenAuthenticator`, refreshing JWT tokens via an isolated client without recursion.
- **Typed Error Handling**: ViewModels interact only with `ApiResult<T>` and `AppFailure`, mapping RFC 7807 problem details directly into user-friendly UI feedback.

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio**: Ladybug (2024.2.1) or newer
- **JDK**: Version 17+
- **Android SDK**: API level 34 (Android 14) minimum target API 24+

### Running the App
1. Open the project in Android Studio and let Gradle sync.
2. Ensure the Aalaya Backend is running locally (`cargo run -p pg-server` on port `8080`).
3. If testing on a physical Android device connected via USB:
   ```bash
   adb reverse tcp:8080 tcp:8080
   ```
4. Run the `:app` configuration on your emulator or connected device.

> **Environment Note:**
> - **Debug builds** default to `http://127.0.0.1:8080/api/v1/` (with `network_security_config.xml` allowing localhost cleartext).
> - **Release builds** require `https://api.aalaya.in/api/v1/`.

---

## 🧪 Testing

Run the local unit test suite (covering financial calculations, complaint state machines, and authentication flow):

```bash
./gradlew testDebugUnitTest
```
