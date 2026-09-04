# Pending Issues — Detailed Handoff

## #24 (partial) — Occupancy stat shows wrong numbers on Android (backend is already fixed)

Status: Backend fix is done, deployed, and verified correct via direct API call and live
logcat capture of the app's own network traffic — GET /finance/branch/{id} returns
"beds_total":2,"beds_occupied":1 correctly. But the Android UI still displays the old
"100%, 1 filled, 0 free" after a fresh app restart with the new APK installed.

What's been ruled out:
- Not a backend bug — confirmed via curl and via the app's own OkHttp logcat output.
- Not a stale-build issue — pulled the actual APK off the device (adb pull the real
  installed base.apk) and confirmed lastUpdateTime matches the latest install.
- The DTOs (FinanceReportDto, FinanceBranchDto in
  app/src/main/java/com/hamsavilas/pg/core/network/dto/FinanceDtos.kt) look structurally
  correct — both have val bedsTotal: Int = 0 / val bedsOccupied: Int = 0 added.

Next thing to check (untried lead): I searched the installed APK's dex for the literal
strings bedsTotal/bedsOccupied (camelCase) and found nothing — but that's the wrong
search. kotlinx.serialization's generated serializer embeds the wire-format name, which
under this project's namingStrategy = SnakeCase (set in di/NetworkModule.kt) would be
beds_total/beds_occupied. That search was never done. Do that first:

    unzip -o app-debug.apk "classes*.dex" -d /tmp/dexcheck
    for f in /tmp/dexcheck/classes*.dex; do strings "$f" | grep -c "beds_total"; done

If it's present: the DTO/serializer is fine, and the bug is downstream — check
DashboardViewModel.kt (feature/dashboard/DashboardViewModel.kt, the refresh() function,
the Role.Manager branch around line ~100 and the scopedId != null branch around line
~130) to confirm financeRepo.branch(...)'s result is actually flowing into
_state.update { it.copy(finance = ...) } correctly, and that StaffDashboardScreen.kt's
occupancyPct/free calculation (already edited to use r.bedsTotal/r.bedsOccupied) is
reading from the right finance.total object — possibly a caching layer (BranchContext)
is serving a stale cached response, or there's a second/older code path rendering this
screen that wasn't found and updated.

If it's absent: the class wasn't recompiled with the new fields for some reason — check
for a second copy of this Android project on disk (see note below) that Gradle might be
building from instead of the one being edited.

IMPORTANT environmental gotcha for whoever picks this up: ensure edits are made in
/home/bhargavraj/Documents/android_kotlin (the authoritative repository path).
Confirm which copy is authoritative before making further edits — editing the
wrong copy would explain a "fix that doesn't take effect" pattern like this one.

---

## #1 — Owner session doesn't survive force-stop (unconfirmed / intermittent)

File area: app/src/main/java/com/hamsavilas/pg/core/data/SessionManager.kt
(bootstrapInternal()), TokenStore.kt

Status: Originally observed twice by the user during manual testing (force-stopping the
Owner session led to a full phone+OTP re-login instead of restoring). When I later
live-tested this myself via adb shell am force-stop + relaunch + logcat capture, the
Manager role's session restored perfectly (went straight to dashboard, no login screen).
I did not get to repeat the same live test specifically for the Owner role before being
asked to move on — so it's possible this bug is Owner-specific (interacting somehow with
the PIN-gate code path) rather than a general session bug.

Reviewed and found no obvious bug in: TokenStore.identity()/refreshToken()/saveIdentity()
(DataStore-backed, looks correctly persisted), SessionManager.bootstrapInternal() (checks
identity() != null && refreshToken() != null).

Next step: repeat the live force-stop test specifically logged in as Owner, capturing
logcat for "OkHttp|401|refresh|invalidat" during the relaunch. If it reproduces, look for
whether a background 401 on first load (e.g. the dashboard's /branches or /finance/org
call) triggers TokenAuthenticator's refresh flow and that refresh fails (stale/
already-rotated refresh token), calling SessionInvalidator.invalidate() ->
SessionManager.signOut() -> tokenStore.clear(), right after bootstrap had briefly
restored the session.

## #2 — Owner PIN gate skippable (unconfirmed / possibly linked to #1)

File area: app/src/main/java/com/hamsavilas/pg/feature/main/AppViewModel.kt
(_pinUnlocked state), navigation/AppRoot.kt (OwnerPinGate composable),
feature/auth/PinScreen.kt

Status: Observed once — after force-stop + full re-login via OTP, the app went straight
into the main shell with no PIN prompt at all, even though a PIN was already set. Static
code review found no obvious bug (the unlocked/hasPin state machine in OwnerPinGate looks
logically sound, and _pinUnlocked is a per-ViewModel-instance field that should reset to
false on every fresh process). Could not reproduce live before being interrupted.

Next step: live-test specifically: log in as Owner, set PIN, force-stop, relaunch,
complete OTP login again, and observe whether the PIN screen appears. If it doesn't, add
temporary logging to OwnerPinGate's when block to see which branch (unlocked /
hasPin==null / hasPin==false / else) is actually being hit.

---

## #23 — Approvals screen shows raw UUIDs instead of names

File: app/src/main/java/com/hamsavilas/pg/feature/approvals/ApprovalsScreen.kt
(~line 75: "${cr.entityType...} ${cr.entityId.take(8)}...")

Fix requires backend work too: the change_requests table already has a proposed jsonb
column (see migrations/tenant/0001_init.sql line ~76) that isn't currently exposed via
the API at all (crates/api/src/routes/changes.rs, ChangeRequestDto in Android's
FinanceDtos.kt). Simplest real fix: expose proposed (or better, resolve the entity's
actual name server-side per entity_type — branches->name, residents->full_name,
rooms->number — before returning it) so the Android card can show something like "Main
Branch" instead of "Branch 23a1ab8a...". This is a multi-file change (repository query ->
domain struct -> route DTO -> Android DTO -> screen), not started.

## #10 — Pull-to-refresh indicator doesn't auto-retract

Where observed: Owner's Home dashboard, after a manual pull-to-refresh gesture — spinner
stays extended.

Not investigated yet. Look at whichever pull-to-refresh wrapper HamsaScreen/
StaffDashboardScreen uses (refreshing = state.loading && ..., onRefresh =
viewModel::refresh pattern seen throughout — check ui/components/ScreenScaffold.kt for
the actual PullToRefresh/SwipeRefresh implementation) and confirm the refreshing boolean
genuinely flips back to false once viewModel.refresh()'s coroutine completes, including
on the error path.

## #20 — One dialog missing a dimmed background

Where observed: tapping an occupied bed in Occupancy (opens a tenant-details popup) — no
scrim behind it, unlike every other dialog checked (Edit branch, Edit resident, Record
payment, resident quick-view — all correctly dimmed).

File area: likely ui/components/ResidentSheet.kt or wherever the bed-tap popup is
implemented (OccupancyViewModel.openResidentSheet / state.sheet in OccupancyScreen.kt —
search for how ResidentSheet composable is rendered/positioned, e.g. is it a proper
Dialog/ModalBottomSheet or a plain Box overlay missing the scrim modifier that other
dialogs get for free).

## #11 — Occupancy search field enabled before a branch/floor is selected

Needs a product decision first, not just a code fix: should search be disabled until
scoped, or intentionally search across all branches? Once decided, the field is in
OccupancyScreen.kt's SearchField(...) call (enabled = branchPicked already partially
there — extend the condition or change the UX copy per whichever direction is chosen).

---

## Also still fully untested (not bugs, just not gotten to)

- Full dedicated Tenant-role QA pass (Complaints raise flow, Away days, Profile, Help
  screens)
- Move-out & settlement flow
- Receipt template / branding editor
- Delete-resident / delete-branch flows
- Marketplace enquiry end-to-end (blocked in this dev DB — zero listings/branches existed
  for most of testing; a branch exists now, so this may be testable)

## Already fixed and confirmed working (for reference — don't re-do)

Backend: notices branch-visibility bug (#27), finance bed-count fields (#24 backend
half). Android: reject snackbar text (#25), add-bed UI (#17), sharing filter (#12/13),
"No beds" label (#18), Explore Listings removed from More menu + empty-Business-card fix
(#4).
