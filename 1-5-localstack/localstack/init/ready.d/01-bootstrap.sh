#!/bin/bash

set -euo pipefail

AWS_REGION="us-east-1"
STACK_NAME="localstack-demo-stack"
BUCKET_NAME="localstack-demo-bucket"
OBJECT_KEY="payloads/payloads.json"
TEMPLATE_FILE="/opt/demo/cloudformation/localstack-demo.yml"
PAYLOAD_FILE="/opt/demo/bootstrap/payloads.json"

export AWS_DEFAULT_REGION="${AWS_REGION}"

awslocal cloudformation deploy \
  --stack-name "${STACK_NAME}" \
  --template-file "${TEMPLATE_FILE}" >/dev/null

awslocal s3 cp "${PAYLOAD_FILE}" "s3://${BUCKET_NAME}/${OBJECT_KEY}" >/dev/null
STACK_STATUS=$(awslocal cloudformation describe-stacks \
  --stack-name "${STACK_NAME}" \
  --query "Stacks[0].StackStatus" \
  --output text)

echo "LocalStack demo stack deployed:"
echo "  stack=${STACK_NAME} (${STACK_STATUS})"
echo "  bucket=${BUCKET_NAME}"
echo "  object=s3://${BUCKET_NAME}/${OBJECT_KEY}"
