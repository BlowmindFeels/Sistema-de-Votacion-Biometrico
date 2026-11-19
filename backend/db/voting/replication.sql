\getenv replication_password REPL_PASSWORD
CREATE ROLE replicador WITH REPLICATION LOGIN PASSWORD :'replication_password';