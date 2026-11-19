#!/bin/bash
set -eu
echo "host replication replicador all scram-sha-256" >> "$PGDATA/pg_hba.conf"