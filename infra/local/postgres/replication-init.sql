DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'pennywise_replica') THEN
        CREATE ROLE pennywise_replica WITH REPLICATION LOGIN PASSWORD 'pennywise-replica-local-only';
    END IF;
END
$$;
