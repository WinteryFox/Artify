FROM postgres:15.2 AS artify-api-postgres

ENV POSTGRES_PASSWORD 12345

COPY tables.sql /docker-entrypoint-initdb.d/tables.sql
