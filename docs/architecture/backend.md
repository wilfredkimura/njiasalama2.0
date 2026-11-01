# 🖥️ Backend Architecture

The NjiaSalama backend is built with **NestJS**, running on Node.js and TypeScript.

---

## 🏗 Core Modules

### 1. Danger Pins (`src/pins/`)
* **Entity (`DangerPin`)**: Maps to PostgreSQL `danger_pins` table (UUID, title, description, type, coordinates, reportedBy, imageUrl, timestamp).
* **Gateway (`PinsGateway`)**: Socket.IO gateway broadcasting new hazard occurrences in real-time to active mobile apps.
* **Service & Controller**: Provides REST CRUD operations.

### 2. Route Safety Engine (`src/routes/`)
* **OpenRouteService Integration**: Queries car and cycling directions between two coordinates.
* **Surface Tag Analysis**: Identifies paved vs unpaved road segments.
* **Safety Scoring**: Cross-references route polylines with proximity to active danger pins.
* **Simulation Fallback**: Gracefully generates plausible simulated routes if the OpenRouteService API is unreachable.

### 3. Authentication (`src/auth/`)
* JWT-based bearer authentication.
* Password hashing with `bcryptjs`.
* Google OAuth ID token verification via `google-auth-library`.

