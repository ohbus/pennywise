# Observability assets

`prometheus.yml` and `rules/pennywise.yml` are deployment-neutral starting
points. Mount them into Prometheus, replace the service discovery targets with
the platform's discovery mechanism, and restrict `/actuator/prometheus` to the
monitoring network. Import the dashboard JSON into Grafana and configure a
service-owned notification policy for the alert severities.

The queries deliberately use only bounded labels (`service`, `status`, and
route dimensions supplied by Spring). Never add request IDs, user IDs, group
IDs, raw exception messages, or tokens as metric labels.
