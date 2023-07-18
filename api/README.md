# Api

This module contains most of the API endpoints for the website, including anything to do with auth,  users,
illustrations, likes and comments.

## Setup

This section will cover the setup process for both a local development environment and the kubernetes
deployment workflow.

### Environment variables

Internal variables

| Variable | Description                   | Type |
|----------|-------------------------------|------|
| PORT     | The port to run the server on | Int  |

Variables required for Cognito (auth) and S3 file storage

| Variable                  | Description                               | Type |
|---------------------------|-------------------------------------------|------|
| AWS_ACCESS_KEY_ID         | The id access key for the AWS account     | Text |
| AWS_SECRET_ACCESS_KEY     | The secret access key for the AWS account | Text |
| AWS_COGNITO_CLIENT_ID     | The pool application client id            | Text |
| AWS_COGNITO_CLIENT_SECRET | The pool application client secret        | Text |
| AWS_COGNITO_POOL          | The name of the pool                      | Text |
| AWS_COGNITO_ISSUER        | The issuer url for the pool               | URI  |
| AWS_COGNITO_REGION        | The AWS region the identity pool is in    | Text |
| AWS_S3_REGION             | The AWS region the S3 bucket is in        | Text |

Variables used for PostgreSQL & RabbitMQ

| Variable          | Description                            | Type    |
|-------------------|----------------------------------------|---------|
| POSTGRES_HOST     | The JDBC URI for the PostgreSQL server | URI     |
| POSTGRES_USERNAME | The user to login with                 | Text    |
| POSTGRES_PASSWORD | The password for the user              | Text    |
| RABBITMQ_HOST     | The hostname for the RabbitMQ server   | URI     |
| RABBITMQ_VHOST    | The virtual host to use                | Text    |
| RABBITMQ_PORT     | The port of the server                 | Int     |
| RABBITMQ_USERNAME | The user to login with                 | Text    |
| RABBITMQ_PASSWORD | The password for the user              | Text    |
| RABBITMQ_SSL      | Connect using SSL                      | Boolean |

### Local environment

Running the server in a local environment is relatively straight forward. Simply create a run configuration for Kotlin,
set the environment variables, then simply run the server.

### Kubernetes environment

TODO
