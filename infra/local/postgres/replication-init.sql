DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'squarewise_replica') THEN
        CREATE ROLE squarewise_replica WITH REPLICATION LOGIN PASSWORD 'squarewise-replica-local-only';
    END IF;
END
$$;
