# Wanderlog

An offline-first Android travel planner. Plan trips, build day-by-day itineraries, track expenses, manage packing lists, and import booking confirmations using AI.

## Features

- **Trip management** — create and manage trips with destination, dates, duration, and automatic destination cover images
- **Itinerary builder** — day-by-day items with drag-to-reorder and swipe-to-delete
- **AI itinerary generation** — describe your trip and get a full itinerary or update existing plans
- **Configurable AI models** — choose separate OpenAI models for itinerary generation and booking image/PDF parsing
- **File import** — share booking emails or upload PDFs/images; AI parses them into itinerary items, lets you review/edit extracted details, stores the source document locally, and links it to imported entries
- **Attachments vault** — attach markdown notes and files to any trip, with local storage and in-app preview for images, text, and PDFs
- **Map view** — see all stops on a map with a connecting polyline
- **Budget tracker** — log and categorise trip expenses, including auto-created transport expenses from imported flight totals
- **Packing list** — per-trip checklist
- **Google Places integration** — search and pin locations when adding itinerary items, and fetch trip cover photos automatically

## Tech Stack

Kotlin · Jetpack Compose · MVVM · Clean Architecture · Hilt · Room · Retrofit · OpenAI API · Google Maps & Places

## Prerequisites

1. **Android Studio** (Hedgehog or later) on Windows with Android SDK installed
2. `local.properties` in the project root (copy from template):
   ```
   sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
   MAPS_API_KEY=<your Google Maps API key>
   ```
3. At runtime, open **Settings** in the app and enter your **OpenAI API key**
4. In **Settings**, you can also choose separate OpenAI models for general trip generation and booking image/PDF parsing, and optionally override the Google Maps key on-device

## Build & Run

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Unit tests
./gradlew test

# Clean
./gradlew clean
```

Open `app/build/outputs/apk/` for the output APK.

## Releasing

Tag a commit to trigger the GitHub Actions release pipeline:

```bash
git tag v1.0.0
git push origin v1.0.0
```

See [RELEASING.md](RELEASING.md) for signing setup and CI details.

## Architecture

Three-layer Clean Architecture:

```
domain/     — models, repository interfaces, use cases (pure Kotlin)
data/       — Room DB, Retrofit/OpenAI, repository implementations
presentation/ — ViewModels + Compose screens
```

DI via Hilt. Single-activity navigation with Compose Navigation.
