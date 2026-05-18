# Wandercraft

[![Android CI](https://github.com/rodionlim/travel-log/actions/workflows/android.yml/badge.svg)](https://github.com/rodionlim/travel-log/actions/workflows/android.yml)

An offline-first Android travel planner. Plan trips, build day-by-day itineraries, keep reusable travel notes, track expenses, manage packing lists, and import booking confirmations using AI.

## Features

- **Trip management** — create and manage trips with destination, dates, duration, and automatic destination cover images
- **Traveller-aware trip setup** — set the number of travellers and save a name or alias for each traveller when creating a trip
- **Itinerary builder** — day-by-day items with drag-to-reorder and swipe-to-delete, active accommodation shown separately for the selected day, optional 1-10 ratings, selected-day or whole-trip filtering by item type or rating, built-in Food, Groceries, and Shopping item types, and optional linked costs back to Budget
- **AI itinerary generation** — describe your trip and get a full itinerary, or let AI choose the best existing days to update
- **Ask About Trip chat** — have a back-and-forth conversation about the current trip using trip details, itinerary, budget, and packing context, with optional multi-select attachments when you need extra source material
- **Configurable AI models** — choose separate OpenAI models for itinerary generation and booking image/PDF parsing
- **File import** — share booking emails, upload one or more PDFs/images/text files, or paste booking text from the clipboard; PDFs default to text extraction with an optional rasterize-to-images mode, AI parses them into itinerary items, lets you review/edit extracted details, stores the source documents locally, links imported entries back to the upload, can create linked transport/accommodation/activity expenses, and can remove the related imported items, budget rows, and stored attachment together when you delete an imported entry
- **Attachment-aware AI Q&A** — Ask About Trip keeps attachments off by default to save tokens, but you can selectively include one or more saved attachments for a question, and PDFs are sent as extracted text instead of rasterized pages
- **Attachments vault** — attach markdown notes and files to any trip, with local storage and in-app preview for images, text, and PDFs
- **Trip notes and global reminders** — add notes inside any trip, mark selected reminders to appear on the home screen, and keep those trip-linked global notes synced across devices
- **Map view** — see all stops on a map with a connecting polyline, including best-effort coordinate resolution for imported places that were saved without map data
- **Budget tracker** — log and categorise trip expenses with optional manual dates, a day-by-day grouped view, an `Unscheduled` bucket, primary day and secondary category filters, auto-created transport/accommodation/activity expenses from imports, and a separate display currency with approximate offline FX conversion
- **Packing list** — per-trip checklist with aggregated and per-traveller tabs, the option to copy a packing list from an existing trip when creating a new trip, plus AI-powered whole-list updates using trip context and a natural-language prompt
- **Google Places integration** — search and pin locations when adding itinerary items, and fetch trip cover photos automatically
- **Offline-first sync** — all data is stored locally in a Room database, with a nearby device sync layer that now includes trip-linked global notes alongside the rest of each trip's data

## Tech Stack

Kotlin · Jetpack Compose · MVVM · Clean Architecture · Hilt · Room · Retrofit · OpenAI API · Google Maps & Places

## Prerequisites

1. **Android Studio** (Hedgehog or later) with Android SDK installed on your machine
2. `local.properties` in the project root (copy from template):
   ```
   sdk.dir=/path/to/Android/sdk
   MAPS_API_KEY=<your Google Maps API key>
   ```
   On Windows, escape backslashes in `sdk.dir` like `C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk`.
3. For any AI-related functionality, create an **OpenAI API key** in your OpenAI account before using the app.
4. At runtime, open **Settings** in the app and enter your **OpenAI API key**.
5. In **Settings**, you can also choose separate OpenAI models for general trip generation and booking image/PDF parsing, plus a budget display currency (default `SGD`). The runtime Google Maps key helps Places-based search, but the map screen itself still requires `MAPS_API_KEY` in `local.properties` because the Android Maps SDK reads its key from the manifest.

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

See [docs/RELEASING.md](docs/RELEASING.md) for signing setup and CI details.

## Docs

- [Trip Sync](docs/TripSync.md)
- [Releasing](docs/RELEASING.md)

## Architecture

Three-layer Clean Architecture:

```
domain/     — models, repository interfaces, use cases (pure Kotlin)
data/       — Room DB, Retrofit/OpenAI, repository implementations
presentation/ — ViewModels + Compose screens
```

DI via Hilt. Single-activity navigation with Compose Navigation.
