#!/bin/bash

# Base URL
URL="http://localhost:8080"

echo "=== 1. Testing Happy Path (Shorten Google) ==="
RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" -d '{"long_url": "https://google.com"}' $URL/shorten)
SHORT_URL=$(echo $RESPONSE | grep -o 'http://[^"].*')
echo "Shortened URL: $SHORT_URL"

if [[ -z "$SHORT_URL" ]]; then
    echo "❌ Failed to shorten URL"
    exit 1
fi
echo "✅ Happy Path Success"

echo -e "\n=== 2. Testing Deduplication (Shorten Google Again) ==="
RESPONSE_2=$(curl -s -X POST -H "Content-Type: application/json" -d '{"long_url": "https://google.com"}' $URL/shorten)
SHORT_URL_2=$(echo $RESPONSE_2 | grep -o 'http://[^"].*')
echo "Shortened URL 2: $SHORT_URL_2"

if [[ "$SHORT_URL" == "$SHORT_URL_2" ]]; then
    echo "✅ Deduplication Working (Same Code Returned)"
else
    echo "❌ Deduplication Failed (Different Codes)"
fi

echo -e "\n=== 3. Testing Normalization (Trailing Slash) ==="
RESPONSE_3=$(curl -s -X POST -H "Content-Type: application/json" -d '{"long_url": "https://google.com/"}' $URL/shorten)
SHORT_URL_3=$(echo $RESPONSE_3 | grep -o 'http://[^"].*')

if [[ "$SHORT_URL" == "$SHORT_URL_3" ]]; then
    echo "✅ Normalization Working (Trailing slash ignored)"
else
    echo "❌ Normalization Failed"
fi

echo -e "\n=== 4. Testing Invalid URL (Garbage) ==="
ERROR_RESP=$(curl -s -X POST -H "Content-Type: application/json" -d '{"long_url": "not-a-url"}' $URL/shorten)
echo "Response: $ERROR_RESP"
if [[ $ERROR_RESP == *"Invalid URL"* ]]; then
    echo "✅ Validation Working (Caught invalid URL)"
else
    echo "❌ Validation Failed"
fi

echo -e "\n=== 5. Testing Redirect ==="
CODE=${SHORT_URL##*/}
REDIRECT_STATUS=$(curl -o /dev/null -s -w "% {http_code}
" $URL/$CODE)
echo "HTTP Status for $SHORT_URL: $REDIRECT_STATUS"

if [[ "$REDIRECT_STATUS" == "302" ]]; then
    echo "✅ Redirect Working (302 Found)"
else
    echo "❌ Redirect Failed (Expected 302)"
fi
