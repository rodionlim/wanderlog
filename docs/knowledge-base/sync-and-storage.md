# Sync and Storage

## Local Database

- `WanderlogDatabase` version: `7`
- Exported schema path: `app/schemas/`

## Current Tables

- `trips`
- `trip_days`
- `itinerary_items`
- `item_attachment_links`
- `expenses`
- `packing_items`
- `attachments`

All foreign keys cascade delete. Date and time values are stored as ISO-8601 text via Room converters.

## Attachment Linking Model

- Item attachments no longer overload `confirmationUrl` with local attachment IDs.
- The `item_attachment_links` table is the authoritative item-to-attachment relationship model.
- Link types currently distinguish:
  - `IMPORT_SOURCE`
  - `MANUAL`

## Imported Booking Cleanup

- Deleting an imported entry can remove the whole imported group from the same source file.
- That cleanup can include related itinerary items, linked expenses, attachment links, and the locally stored source attachment.

## Sync Model

- Trip sync protocol version is `2`.
- Sync now includes item-attachment link payloads in addition to trips, days, itinerary items, expenses, packing items, and attachments.
- Legacy `attachment://...` values are backfilled into `item_attachment_links` during database migration.

## Related References

- See [Trip Sync](./trip-sync.md) for the device-to-device sync flow, permissions, and merge behavior.
