# Bruno API collection

Import `tools/bruno/` into Bruno and select the `local` environment. The
collection covers the Accounts, Expense Core, Notifications, and GraphQL BFF
HTTP surfaces currently defined by the repository contracts.

The local environment uses `test-user` as a demo bearer token. The complete
stack must be running first:

```sh
make full-up
make full-status
```

Requests that create a group or expense save response identifiers into Bruno
variables for subsequent requests. IDs such as `notificationId` and
`membershipId` must be copied from a live response when a request needs one.

The collection is intentionally local-only: it contains no production hosts,
secrets, or environment files.
