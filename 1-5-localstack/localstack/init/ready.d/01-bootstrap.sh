#!/bin/bash

set -euo pipefail

AWS_REGION="us-east-1"
ACCOUNT_ID="000000000000"
BUCKET_NAME="localstack-demo-bucket"
OBJECT_KEY="payloads/payloads.json"
QUEUE_NAME="localstack-demo-queue"
TOPIC_NAME="localstack-demo-topic"
SNS_SUBSCRIPTION_QUEUE_NAME="localstack-demo-sns-subscription-queue"
BATCH_SIZE_PARAMETER="/localstack-demo/batchsize"
USE_LAMBDA_PARAMETER="/localstack-demo/useLambda"
LAMBDA_NAME="localstack-demo-enrichment"
LAMBDA_ROLE_ARN="arn:aws:iam::${ACCOUNT_ID}:role/localstack-demo-lambda-role"
PAYLOAD_FILE="/opt/demo/bootstrap/payloads.json"
LAMBDA_ARCHIVE="/opt/demo/lambda/function.zip"

export AWS_DEFAULT_REGION="${AWS_REGION}"

awslocal s3api create-bucket --bucket "${BUCKET_NAME}" >/dev/null 2>&1 || true
awslocal s3 cp "${PAYLOAD_FILE}" "s3://${BUCKET_NAME}/${OBJECT_KEY}" >/dev/null

awslocal ssm put-parameter \
  --name "${BATCH_SIZE_PARAMETER}" \
  --value "3" \
  --type String \
  --overwrite >/dev/null

awslocal ssm put-parameter \
  --name "${USE_LAMBDA_PARAMETER}" \
  --value "true" \
  --type String \
  --overwrite >/dev/null

QUEUE_URL=$(awslocal sqs create-queue \
  --queue-name "${QUEUE_NAME}" \
  --query "QueueUrl" \
  --output text)

TOPIC_ARN=$(awslocal sns create-topic \
  --name "${TOPIC_NAME}" \
  --query "TopicArn" \
  --output text)

SNS_QUEUE_URL=$(awslocal sqs create-queue \
  --queue-name "${SNS_SUBSCRIPTION_QUEUE_NAME}" \
  --query "QueueUrl" \
  --output text)

SNS_QUEUE_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url "${SNS_QUEUE_URL}" \
  --attribute-names QueueArn \
  --query "Attributes.QueueArn" \
  --output text)

SNS_QUEUE_POLICY=$(cat <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowLocalstackDemoTopic",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "sqs:SendMessage",
      "Resource": "${SNS_QUEUE_ARN}",
      "Condition": {
        "ArnEquals": {
          "aws:SourceArn": "${TOPIC_ARN}"
        }
      }
    }
  ]
}
EOF
)

awslocal sqs set-queue-attributes \
  --queue-url "${SNS_QUEUE_URL}" \
  --attributes "Policy=${SNS_QUEUE_POLICY}" >/dev/null

awslocal sns subscribe \
  --topic-arn "${TOPIC_ARN}" \
  --protocol sqs \
  --notification-endpoint "${SNS_QUEUE_ARN}" \
  --attributes RawMessageDelivery=true >/dev/null

if ! awslocal lambda get-function --function-name "${LAMBDA_NAME}" >/dev/null 2>&1; then
  awslocal lambda create-function \
    --function-name "${LAMBDA_NAME}" \
    --runtime nodejs20.x \
    --zip-file "fileb://${LAMBDA_ARCHIVE}" \
    --handler index.handler \
    --role "${LAMBDA_ROLE_ARN}" >/dev/null
fi

echo "LocalStack demo resources created:"
echo "  bucket=${BUCKET_NAME}"
echo "  object=s3://${BUCKET_NAME}/${OBJECT_KEY}"
echo "  queue=${QUEUE_URL}"
echo "  topic=${TOPIC_ARN}"
echo "  snsSubscriptionQueue=${SNS_QUEUE_URL}"
echo "  lambda=${LAMBDA_NAME}"
