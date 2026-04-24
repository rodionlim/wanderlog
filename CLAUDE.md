# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android travel planning app — a clone of [wanderlog.com](https://wanderlog.com/). Single-user, offline-first, with AI-powered itinerary generation and booking file import.

## Development Environment

This project lives in WSL2. **Android Studio cannot run usefully inside WSL** (no AVD hardware acceleration). The recommended workflow:

1. Install **Android Studio on Windows** (not inside WSL).
2. Open the project from Windows via the UNC path:
   ```
   \\wsl$\Ubuntu\home\rodionlim\workspace\Github\wanderlog
   ```
3. Edit code in Claude Code (WSL) or Android Studio on Windows — both work on the same files.
4. Gradle and the Android SDK must be installed on the **Windows** side; set `sdk.dir` in `local.properties` to the Windows SDK path (e.g. `C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk`).

## Build & Run

Run these from Windows (Android Studio terminal or PowerShell), or from WSL if the Android SDK is also installed there:

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires emulator or device)
./gradlew connectedAndroidTest

# Clean
./gradlew clean
```

### Prerequisites

1. Copy `local.properties.template` → `local.properties` and set `sdk.dir` to your Android SDK path.
2. Set `MAPS_API_KEY` in `local.properties` (injected into the manifest via `manifestPlaceholders`).
3. In the running app → Settings screen, enter your **OpenAI API key** and optionally the Google Maps key at runtime (stored in `EncryptedSharedPreferences`).

## Architecture

**Kotlin + Jetpack Compose + MVVM + Clean Architecture** with three layers:

| Layer | Package | Role |
|---|---|---|
| Domain | `domain/model`, `domain/repository`, `domain/usecase` | Pure Kotlin — no Android deps. Defines data models, repository interfaces, and use cases |
| Data | `data/local`, `data/remote`, `data/repository` | Room (local), Retrofit/OpenAI (remote), repository implementations |
| Presentation | `presentation/` | ViewModels + Compose screens |

**DI**: Hilt (SingletonComponent). All modules in `core/di/`.

**Navigation**: Single-activity (`MainActivity`), Compose Navigation, `NavGraph.kt` + `Screen.kt` sealed class with typed routes.

## Key Packages

```
core/di/           — DatabaseModule, NetworkModule, RepositoryModule
core/util/         — FileUtils (PDF rasterise, image compress), DateTimeUtils, Result.kt
data/local/        — Room: 5 entities + DAOs + WanderlogDatabase
data/remote/openai — OpenAiService (Retrofit), DTOs, ApiKeyInterceptor
data/remote/places — PlacesDataSource (wraps Google Places SDK)
data/repository/   — *RepositoryImpl (bind interfaces to Room/network)
domain/usecase/    — trip/, itinerary/, ai/, expense/, packing/
presentation/      — one sub-package per screen; ViewModels use SavedStateHandle
```

## Room Database

5 tables: `trips`, `trip_days`, `itinerary_items`, `expenses`, `packing_items`.  
All FKs cascade-delete. Dates stored as ISO-8601 text via `RoomConverters`.  
`WanderlogDatabase` version = 1. Schema exported to `app/schemas/`.

## OpenAI Integration

`AiRepositoryImpl` uses `POST /v1/chat/completions` with `model = "gpt-4o"` and `response_format = { "type": "json_object" }`.

- **Itinerary generation**: system + user text prompt → JSON with day/item structure → `TripDay` + `ItineraryItem` lists.
- **File parsing**: message `content` is an array of `ContentPartDto` (text or image). PDFs are rasterised page-by-page via `android.graphics.pdf.PdfRenderer` → JPEG → base64 in `ParseFileUseCase`.

The API key is injected per-request by `ApiKeyInterceptor` reading from `EncryptedSharedPreferences`.

## Google Maps & Places

`MapScreen` uses `maps-compose` (`GoogleMap` composable). Markers for each `ItineraryItem` with lat/lng. Polyline connects all stops in order.

`PlacesDataSource` wraps `PlacesClient.findAutocompletePredictions` (autocomplete) and `fetchPlace` (details). `PlaceSearchViewModel` debounces the query by 300 ms.

## File Import Flow

`ParseFileUseCase` (AI use case) → branch by MIME type → `ContentPartDto` list → `AiRepositoryImpl.parseFile()` → `ParsedBooking` → `FileImportViewModel` shows review dialog → user confirms → `ItineraryRepository.insertItems()`.

## Screens & Navigation

```
TripList → TripForm (create/edit)
TripList → Itinerary(tripId)
  ├─ BottomSheet: ItineraryItemFormSheet
  ├─ BottomSheet: PlaceSearchSheet
  ├─ BottomSheet: FileImportSheet
  ├─ Map(tripId[, dayId])
  ├─ Budget(tripId)
  ├─ Packing(tripId)
  └─ AiGenerate(tripId)
Settings (from TripList top-bar)
```
