# 📱 Android Client Architecture

The mobile application is a native Android project written in **Kotlin** and built with **Jetpack Compose**.

---

## 🏛 Clean Architecture + MVVM

The app follows standard Clean Architecture principles:

* **Presentation Layer (`com.njiasalama.ui`)**:
  * `MapScreen.kt`: Google Maps Compose canvas rendering active user position, hazard markers, and route navigation overlays.
  * `MapViewModel.kt`: Manages state flows, coordinates hazard reports, queries routes, and triggers alerts.
  * `AddPinDialog.kt`: Modal sheet allowing users to drop a hazard pin, categorize it, and capture/attach photo proof.
  * `AuthScreen.kt` & `AuthViewModel.kt`: Login/registration UI supporting credential and Google OAuth sign-in.

* **Domain Layer (`com.njiasalama.domain`)**:
  * Models: `DangerPin`, `Route`, `RouteSafetyStatus`, `SavedRoute`, `AuthModels`.
  * Interfaces: `PinRepository`, `AuthRepository`.

* **Data Layer (`com.njiasalama.data`)**:
  * `NjiaSalamaApi`: Retrofit REST client defining endpoints for authentication, pins, and routes.
  * `SocketManagerImpl`: Socket.IO client that listens to incoming hazard alerts broadcast by the NestJS backend.
  * `LocationService`: FusedLocationProviderClient abstraction providing GPS updates.
  * `offline_pins.json`: Persistent file queue used to buffer offline hazard submissions.

