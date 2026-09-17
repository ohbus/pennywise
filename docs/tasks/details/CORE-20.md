# CORE-20 — Expose recurring schedule management and pause notifications

Add authenticated recurring-schedule create/list/update/pause/resume transport,
bounded worker catch-up behavior, and visible notification when invalid membership
pauses generation. Depends on CORE-15 and NOT-07. Owns recurrence controllers,
tests, related contracts, and required event schemas.
