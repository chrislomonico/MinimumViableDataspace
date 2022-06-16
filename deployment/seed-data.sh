#!/bin/bash

set -euxo pipefail

newman run \
  --folder "Publish Master Data" \
  --env-var data_management_url="http://$EDC_HOST:9191/api/v1/data" \
  --env-var storage_account="$ASSETS_STORAGE_ACCOUNT" \
  --env-var api_key="$API_KEY" \
  --env-var provider_company_name="$PROVIDER_COMPANY_NAME" \
  deployment/data/MVD.postman_collection.json
