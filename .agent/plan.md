# Project Plan



## Project Brief



## Implementation Steps
**Total Duration:** 43m 3s

### Task_1_SetupAndDataLayer: Update build.gradle.kts and AndroidManifest.xml. Implement PrivacyStore (DataStore) and CafeDatabase (SQLiteOpenHelper).
- **Status:** COMPLETED
- **Updates:** Updated build.gradle.kts with Compose, DataStore, Lifecycle, and JUnit dependencies. Added manifest permissions and security attributes. Created PrivacyStore handling GDPR flags using DataStore. Created CafeDatabase implementing users, menu, orders, feedback, and events schemas using SQLiteOpenHelper.
- **Acceptance Criteria:**
  - Gradle syncs successfully
  - Manifest updated with permissions and security flags
  - PrivacyStore handles GDPR flags
  - CafeDatabase handles users, menu, orders, feedback, events
- **Duration:** 16m 37s

### Task_2_CoreLogicAndViewModel: Implement LocalSentiment (Naive Bayes classifier), DeviceFeatures (location handling), and CafeViewModel (StateFlow management).
- **Status:** COMPLETED
- **Updates:** Implemented LocalSentiment with Multinomial Naive Bayes text classifier in-memory. Implemented DeviceFeatures with findNearestBranchEphemeral using timeout and distance calculation. Implemented CafeViewModel managing auth, cart, orders, and simulating order lifecycle. All tests compile perfectly.
- **Acceptance Criteria:**
  - LocalSentiment implements text classification
  - DeviceFeatures handles nearest branch location
  - CafeViewModel manages auth, cart, orders, privacy toggles
- **Duration:** 3m 57s

### Task_3_ComposeUI: Implement Material 3 Compose UI in MainActivity with AuthScreen, TermsGateScreen, and 5-tab Bottom Navigation.
- **Status:** COMPLETED
- **Updates:** Implemented Material 3 Compose UI in MainActivity with AuthScreen, TermsGateScreen, and 5-tab Bottom Navigation (Menu, Cart, Orders, Review, Privacy). Integrated with CafeViewModel, DeviceFeatures, and PrivacyStore.
- **Acceptance Criteria:**
  - AuthScreen and TermsGateScreen implemented
  - Bottom Navigation functional with 5 tabs (Menu, Cart, Orders, Review, Privacy)
  - UI connects to CafeViewModel
- **Duration:** 10m 52s

### Task_4_RunAndVerify: Implement SmartCafeUnitTest and verify application stability, alignment with requirements, and zero compilation errors.
- **Status:** COMPLETED
- **Updates:** Implemented SmartCafeUnitTest with tests for LocalSentiment (positive, negative, unknown, negation) and cart/price math. Clean build with zero errors.
- **Acceptance Criteria:**
  - SmartCafeUnitTest covers LocalSentiment and Cart math
  - make sure all existing tests pass
  - build pass
  - app does not crash
- **Duration:** 11m 37s

