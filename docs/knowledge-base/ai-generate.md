# AI Generate

## Purpose

AI Generate is the itinerary-generation workflow used from the itinerary screen. It supports two distinct modes:

- full-trip generation
- update existing days

Both modes use the general OpenAI model configured in Settings.

## Entry Point

- Screen: `presentation/ai/generate/AiGenerateScreen.kt`
- ViewModel: `presentation/ai/generate/AiGenerateViewModel.kt`
- Repository logic: `data/repository/AiRepositoryImpl.kt`

The screen is launched from the itinerary overflow menu.

## Mode: Full Trip

Full-trip generation sends:

- destination
- trip start date
- trip end date
- travel style / preferences
- saved traveller profiles from the trip
- a traveller count inferred from those profiles

The full-trip prompt asks the model to return a complete day-by-day itinerary as JSON.

When committed:

- existing trip days are deleted
- new trip days are created
- generated itinerary items are inserted against the new day records

This mode behaves like a replacement of the itinerary structure.

## Mode: Update Days

Update-days generation is additive rather than destructive.

It requires the user to describe what should change, and then the app sends:

- destination
- the user’s update request / preferences text
- saved traveller profiles from the trip
- a traveller count inferred from those profiles
- all existing trip days currently available
- each existing day’s current itinerary items

### Does It Send Existing Context?

Yes.

The update-days path explicitly loads all available trip days and populates each day with its current `ItineraryItem` list before calling `generateItinerary(...)`.

That context is then formatted into the repository prompt as:

- day number
- day date
- existing items for that day

The prompt tells the model to:

- choose only from the existing days already present in the trip
- return only the subset of days that should receive additions
- return only new items to add
- avoid repeating unchanged items
- avoid obvious duplicates

## Existing Item Formatting

Existing items are summarized per day using:

- title
- start and end time when available
- place name when available
- notes when available

This is enough context for the model to understand what is already planned without sending the raw internal object structure.

## Commit Semantics For Update Days

When the user commits update-day suggestions:

- generated days are matched back to existing days by date or day number
- only the generated items are inserted
- existing items remain in place
- new items are appended after the current maximum `sortOrder` for that day

So update mode adds onto the itinerary instead of replacing it.

## UI Behavior Notes

- The screen uses chips to switch between `Full trip` and `Update days`.
- In `Update days`, the preferences field becomes a change-request prompt rather than a generic travel-style field.
- Traveller context comes from the trip’s saved traveller profiles; there is no separate manual traveller-count field in AI Generate.
- The screen shows an approximate prompt breakdown for context, input, and total tokens.
- The preview clearly labels update mode as suggested additions across matched days.
- Generated items can be accepted or rejected individually before commit.
- Potential overlaps with existing itinerary items are detected in update mode and start unchecked by default.
- The commit action label changes to `Add to Matching Days` in update mode.

## Practical Implication

If the question is whether the model sees the current itinerary while doing `Update days`, the answer is yes. That existing itinerary context is a core part of the update-days prompt and is what allows the model to choose which current days to extend.

## Related References

- [AI and Imports](./ai-and-imports.md)
- [Settings and Onboarding](./settings-and-onboarding.md)
- [Project Overview](./project-overview.md)
