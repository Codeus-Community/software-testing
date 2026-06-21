#!/bin/sh

set -eu

AWS_ENDPOINT_URL="${AWS_ENDPOINT_URL:-http://floci:4566}"
AWS_REGION="${AWS_DEFAULT_REGION:-us-east-1}"
STACK_NAME="floci-demo-stack"
BUCKET_NAME="floci-demo-bucket"
OBJECT_KEY="payloads/payloads.json"
TEMPLATE_FILE="/opt/demo/cloudformation/floci-demo.yml"
PAYLOAD_FILE="/opt/demo/bootstrap/payloads.json"

until aws --endpoint-url "${AWS_ENDPOINT_URL}" --region "${AWS_REGION}" s3 ls >/dev/null 2>&1; do
  sleep 1
done

aws --endpoint-url "${AWS_ENDPOINT_URL}" \
  --region "${AWS_REGION}" \
  cloudformation deploy \
  --stack-name "${STACK_NAME}" \
  --template-file "${TEMPLATE_FILE}" >/dev/null

aws --endpoint-url "${AWS_ENDPOINT_URL}" \
  --region "${AWS_REGION}" \
  s3 cp "${PAYLOAD_FILE}" "s3://${BUCKET_NAME}/${OBJECT_KEY}" >/dev/null

STACK_STATUS=$(aws --endpoint-url "${AWS_ENDPOINT_URL}" \
  --region "${AWS_REGION}" \
  cloudformation describe-stacks \
  --stack-name "${STACK_NAME}" \
  --query "Stacks[0].StackStatus" \
  --output text)

echo "Floci demo stack deployed:"
echo "  stack=${STACK_NAME} (${STACK_STATUS})"
echo "  bucket=${BUCKET_NAME}"
echo "  object=s3://${BUCKET_NAME}/${OBJECT_KEY}"
