# Floci Spring Demo

This module is a small Spring Boot example that shows how to combine Floci, PostgreSQL, and several AWS services in one app.

## What is included

- `POST /s3-only-flow`
  Reads the configured JSON payload file from S3, takes only the first `batchsize` records from SSM, and stores them in PostgreSQL.
- `POST /sqs-flow`
  Reads the S3 file and pushes each payload into SQS.
- `POST /sns-flow`
  Reads the S3 file and publishes each payload into SNS.
- `POST /lambda-flow`
  Reads the S3 file, optionally invokes Lambda depending on `useLambda`, and stores the final payloads in PostgreSQL.
- A small background polling job that reads:
  - the main SQS queue for `/sqs-flow`
  - the SNS subscription SQS queue for `/sns-flow`
  and persists those messages to PostgreSQL with the correct `sourceType`.

## AWS services used with Floci

- S3
- SSM
- SQS
- SNS
- Lambda

PostgreSQL runs as a standalone Docker container so the example stays lightweight and self-contained.

## Profiles

- `local`
  - uses Floci endpoint overrides
  - reads `batchsize` and `useLambda` from SSM
  - enables the background consumer
- `prod-like`
  - removes Floci endpoint overrides
  - uses example AWS-style resource names
  - uses profile-local fallback values instead of SSM
  - disables the background consumer

## Start the infrastructure

From `1-5-localstack` run:

```powershell
docker compose up -d
```

This starts:

- `floci` on `http://localhost:4566`
- `postgres` on `localhost:5432`

The bootstrap container deploys the shared CloudFormation stack `floci-demo-stack` from `floci/cloudformation/floci-demo.yml`, then uploads the demo payload file to S3.

The stack creates:

- S3 bucket `floci-demo-bucket`
- object `payloads/payloads.json`
- SSM parameters `/floci-demo/batchsize` and `/floci-demo/useLambda`
- SQS queue `floci-demo-queue`
- SNS topic `floci-demo-topic`
- SNS subscription queue `floci-demo-sns-subscription-queue`
- Lambda function `floci-demo-enrichment`

## Run the app

```powershell
mvn spring-boot:run -pl 1-5-localstack
```

To run with the example production-like profile:

```powershell
mvn spring-boot:run -pl 1-5-localstack -Dspring-boot.run.profiles=prod-like
```

## Example calls

```powershell
curl -X POST http://localhost:8080/s3-only-flow
curl -X POST http://localhost:8080/sqs-flow
curl -X POST http://localhost:8080/sns-flow
curl -X POST http://localhost:8080/lambda-flow
```

## Useful inspection commands

```powershell
docker compose run --rm --entrypoint aws bootstrap --endpoint-url http://floci:4566 cloudformation describe-stacks --stack-name floci-demo-stack
docker compose run --rm --entrypoint aws bootstrap --endpoint-url http://floci:4566 cloudformation list-stack-resources --stack-name floci-demo-stack
docker compose run --rm --entrypoint aws bootstrap --endpoint-url http://floci:4566 s3 ls s3://floci-demo-bucket/payloads/
docker compose run --rm --entrypoint aws bootstrap --endpoint-url http://floci:4566 ssm get-parameter --name /floci-demo/batchsize
docker compose run --rm --entrypoint aws bootstrap --endpoint-url http://floci:4566 sqs receive-message --queue-url http://floci:4566/000000000000/floci-demo-queue
docker compose run --rm --entrypoint aws bootstrap --endpoint-url http://floci:4566 lambda invoke --function-name floci-demo-enrichment response.json
docker exec -it floci-demo-postgres psql -U floci -d floci_demo -c "select id, source_type, payload from processed_payload order by id;"
```

## Payload generator

`org.codeus.localstackdemo.util.PayloadFileGenerator` generates payload JSON files in the same format that the demo uploads to S3:

```json
[
  {"id": 1, "message": "message_1"},
  {"id": 2, "message": "message_2"}
]
```

You can regenerate a file manually with:

```powershell
mvn -pl 1-5-localstack -DskipTests exec:java "-Dexec.mainClass=org.codeus.localstackdemo.util.PayloadFileGenerator" "-Dexec.args=floci/bootstrap/payloads.json 10"
```
