#!/usr/bin/env sh
vault secrets enable -path=postgresql/preprod-fss database
vault write postgresql/preprod-fss/config/helse-spenn-oppdrag plugin_name=postgresql-database-plugin allowed_roles=helse-spenn-oppdrag-admin,helse-spenn-oppdrag-user connection_url=postgresql://{{username}}:{{password}}@postgres:5432?sslmode=disable username=postgres password=postgres
vault write postgresql/preprod-fss/roles/helse-spenn-oppdrag-admin db_name=helse-spenn-oppdrag creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; GRANT \"helse-spenn-oppdrag-admin\" TO \"{{name}}\";" default_ttl="5m" max_ttl="5m"
vault write postgresql/preprod-fss/roles/helse-spenn-oppdrag-user db_name=helse-spenn-oppdrag creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; GRANT \"helse-spenn-oppdrag-user\" TO \"{{name}}\";" default_ttl="5m" max_ttl="5m"
