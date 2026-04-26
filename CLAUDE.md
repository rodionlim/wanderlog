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
3. In the running app → Settings screen, enter your **OpenAI API key** and choose separate OpenAI models for general trip generation vs booking image/PDF parsing (stored in `EncryptedSharedPreferences`). The runtime Google Maps key helps Places-based search, but `MapScreen` still requires `MAPS_API_KEY` in `local.properties` because the Android Maps SDK reads the manifest key at app startup.

## Architecture

**Kotlin + Jetpack Compose + MVVM + Clean Architecture** with three layers:

| Layer        | Package                                               | Role                                                                                     |
| ------------ | ----------------------------------------------------- | ---------------------------------------------------------------------------------------- |
| Domain       | `domain/model`, `domain/repository`, `domain/usecase` | Pure Kotlin — no Android deps. Defines data models, repository interfaces, and use cases |
| Data         | `data/local`, `data/remote`, `data/repository`        | Room (local), Retrofit/OpenAI (remote), repository implementations                       |
| Presentation | `presentation/`                                       | ViewModels + Compose screens                                                             |

**DI**: Hilt (SingletonComponent). All modules in `core/di/`.

**Navigation**: Single-activity (`MainActivity`), Compose Navigation, `NavGraph.kt` + `Screen.kt` sealed class with typed routes.

## Key Packages

```
core/di/           — DatabaseModule, NetworkModule, RepositoryModule
core/util/         — FileUtils (PDF rasterise, image compress), DateTimeUtils, Result.kt
data/local/        — Room: 6 entities + DAOs + WanderlogDatabase
data/remote/openai — OpenAiService (Retrofit), DTOs, ApiKeyInterceptor
data/remote/places — PlacesDataSource (wraps Google Places SDK)
data/repository/   — *RepositoryImpl (bind interfaces to Room/network)
domain/usecase/    — trip/, itinerary/, ai/, expense/, packing/
presentation/      — one sub-package per screen; ViewModels use SavedStateHandle
```

## Room Database

6 tables: `trips`, `trip_days`, `itinerary_items`, `expenses`, `packing_items`, `attachments`.  
All FKs cascade-delete. Dates stored as ISO-8601 text via `RoomConverters`.  
`WanderlogDatabase` version = 4. Schema exported to `app/schemas/`.

## OpenAI Integration

`AiRepositoryImpl` uses `POST /v1/chat/completions` with `response_format = { "type": "json_object" }` and runtime-configurable model selection from Settings.

- **Itinerary generation**: system + user text prompt → JSON with day/item structure → `TripDay` + `ItineraryItem` lists, using the user-selected general OpenAI model. The flow supports full-trip generation and a multi-day additive update where AI chooses which existing days to extend.
- **File parsing**: message `content` is an array of `ContentPartDto` (text or image). `ParseFileUseCase` can combine one or more uploaded files into a single parsing request. PDFs default to text extraction via `pdfbox-android`, with an import-time checkbox that can rasterise them page-by-page via `android.graphics.pdf.PdfRenderer` → JPEG → base64 for scanned or layout-heavy documents. Parsing uses a separate user-selectable parsing model, and image/PDF requests fall back to a vision-safe model when needed.

The API key is injected per-request by `ApiKeyInterceptor` reading from `EncryptedSharedPreferences`.

## Google Maps & Places

`MapScreen` uses `maps-compose` (`GoogleMap` composable). Markers for each `ItineraryItem` with lat/lng. Polyline connects all stops in order. The map view depends on the manifest `MAPS_API_KEY`; the runtime Settings key is only used for Places-based search/fetch flows.

`PlacesDataSource` wraps `PlacesClient.findAutocompletePredictions` (autocomplete), `fetchPlace` (details), and place photo fetching for trip cover images. Destination cover photos are cached locally under app files storage. `PlaceSearchViewModel` debounces the query by 300 ms.

## File Import Flow

`ParseFileUseCase` (AI use case) → branch by MIME type across one or more uploaded files, with PDFs defaulting to text extraction and optional image rasterisation → combined `ContentPartDto` list → `AiRepositoryImpl.parseFile()` → `ParsedBooking` → `FileImportViewModel` builds itinerary items and shows an editable review dialog → user confirms → `ItineraryRepository.insertItems()`.

- Imported items are assigned to matching `TripDay`s based on parsed dates when possible.
- For file-based imports, the original source documents are stored locally and imported items keep an attachment link back to the upload.
- Flight imports can also create `TRANSPORT` expenses automatically from parsed ticket totals.

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

`TripForm` also captures traveller aliases, and `PackingScreen` can show an aggregated checklist plus individual traveller tabs backed by per-traveller packing items.
