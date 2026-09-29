CREATE DATABASE squarewise_accounts;
CREATE DATABASE squarewise_expense_core;
CREATE DATABASE squarewise_notifications;

-- Dedicated least-privilege service roles (M-2)
CREATE USER accounts_user WITH ENCRYPTED PASSWORD 'accounts-local-only';
GRANT ALL PRIVILEGES ON DATABASE squarewise_accounts TO accounts_user;

CREATE USER expense_core_user WITH ENCRYPTED PASSWORD 'expense-core-local-only';
GRANT ALL PRIVILEGES ON DATABASE squarewise_expense_core TO expense_core_user;

CREATE USER notifications_user WITH ENCRYPTED PASSWORD 'notifications-local-only';
GRANT ALL PRIVILEGES ON DATABASE squarewise_notifications TO notifications_user;

