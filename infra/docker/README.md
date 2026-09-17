# Container builds

`Dockerfile.jvm` is the single production build definition. Set
`APP_PROJECT` to `accounts`, `expense-core`, `notifications`, or `bff`; the
result is a non-root, read-only-runtime-compatible image. `Dockerfile.dev`
provides the Gradle toolchain for Compose development.

The production Compose file accepts immutable image references through
`PENNYWISE_*_IMAGE`; it does not build or contain credentials. Run migrations as
a separate release step before rolling application images. Local Compose uses
development-only credentials and is not a production deployment recipe.
