# NjiaSalama Documentation & Hub

Welcome to **NjiaSalama** ("Safe Path"), a road safety and navigation platform designed to empower drivers and commuters to identify, report, and avoid hazards in real time.

---

## 🚀 Key Features

* **Real-Time Hazard Reporting**: Report potholes, unlit streets, aggressive traffic, and road accidents with optional photo proof.
* **Smart Route Safety Scoring**: Evaluate route safety based on live community reports and road surface quality (paved vs. unpaved).
* **Live WebSocket Broadcasts**: Instant alerts delivered to nearby active drivers.
* **Offline-First Resilience**: Log hazards even when disconnected from cellular network; reports sync automatically when online.
* **Modern Android Interface**: Built using Jetpack Compose, Material 3, and Kotlin Coroutines.

---

## 📥 Get the App

Looking to try out the latest build?

👉 **[Go to the Downloads & Releases page](downloads.md)** to download the latest `.apk` directly sourced from our GitHub releases.

---

## 🛠 Tech Stack

* **Mobile**: Android (Kotlin, Jetpack Compose, Google Maps SDK, Retrofit, Socket.IO Client).
* **Backend**: NestJS, TypeScript, TypeORM, PostgreSQL, WebSockets.
* **Routing**: OpenRouteService API with surface classification & procedural simulation fallbacks.

