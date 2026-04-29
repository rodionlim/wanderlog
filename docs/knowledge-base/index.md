# Wanderlog Knowledge Base

This knowledge base is a project-local reference for the current Wanderlog codebase. It is split into topic files so the documentation stays navigable and does not accumulate into one oversized note.

## Topics

- [Project Overview](./project-overview.md) — product scope, core user workflows, and current feature set.
- [Architecture](./architecture.md) — package layout, app layers, navigation, and key runtime responsibilities.
- [AI and Imports](./ai-and-imports.md) — OpenAI integration, Ask About Trip, booking parsing, and import behavior.
- [AI Generate](./ai-generate.md) — full-trip generation, update-days behavior, prompt context, and commit semantics.
- [Map Integration](./map-integration.md) — map screen behavior, coordinate resolution, Google Maps launching, and Maps key requirements.
- [Settings and Onboarding](./settings-and-onboarding.md) — runtime settings, API key help, and first-run AI onboarding.
- [Sync and Storage](./sync-and-storage.md) — Room schema, attachment-link model, and sync-specific storage rules.
- [Trip Sync](./trip-sync.md) — Nearby device-to-device sync flow, payload contents, permissions, and conflict behavior.
- [Development and Operations](./development-and-operations.md) — environment setup, build/test commands, CI, and release flow.
- [Maintenance Rules](./maintenance-rules.md) — how to keep this knowledge base small, useful, and easy to query.

## Chronology

- [Notes Overview](./NOTES.md) — chronology conventions and quick entry points.
- [Chronology Index](./chronology/index.md) — dated monthly logs.

## Existing Canonical Docs

- [README](../../README.md) — public project overview and setup instructions.
- [CLAUDE](../../CLAUDE.md) — repository working notes and implementation-facing guidance.
- [Releasing](../RELEASING.md) — release process and signing guidance.

## How To Use This Folder

- Put high-level stable knowledge here.
- Keep implementation-specific deep dives in the topic file that best owns the concept.
- Prefer linking to existing canonical docs over duplicating long operational instructions.
- Keep chronology in the monthly files under `chronology/` instead of expanding one flat notes file forever.
