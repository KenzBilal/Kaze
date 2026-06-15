#!/bin/bash
set -e

# Setup java
source "$HOME/.sdkman/bin/sdkman-init.sh"
export JAVA_HOME="$HOME/.sdkman/candidates/java/current"

echo "Building APK..."
./gradlew assembleDebug --no-configuration-cache

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
SHA256=$(sha256sum "$APK_PATH" | awk '{print $1}')
echo "APK SHA256: $SHA256"

TOKEN="REDACTED_TOKEN"
REPO="KenzBilal/Kaze"
TAG="v2.8.0"

echo "Creating GitHub Release..."
RELEASE_RESPONSE=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Accept: application/vnd.github+json" -d "{\"tag_name\":\"$TAG\",\"name\":\"$TAG\",\"body\":\"Release $TAG\"}" "https://api.github.com/repos/$REPO/releases")

UPLOAD_URL=$(echo "$RELEASE_RESPONSE" | jq -r .upload_url | sed -e "s/{?name,label}//")

if [ "$UPLOAD_URL" == "null" ] || [ -z "$UPLOAD_URL" ]; then
    echo "Failed to create release. Response:"
    echo "$RELEASE_RESPONSE"
    # Maybe release already exists, try to fetch it
    UPLOAD_URL=$(curl -s -H "Authorization: Bearer $TOKEN" "https://api.github.com/repos/$REPO/releases/tags/$TAG" | jq -r .upload_url | sed -e "s/{?name,label}//")
fi

echo "Uploading APK to $UPLOAD_URL..."
ASSET_RESPONSE=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/vnd.android.package-archive" --data-binary @"$APK_PATH" "$UPLOAD_URL?name=app-debug.apk")

APK_DOWNLOAD_URL=$(echo "$ASSET_RESPONSE" | jq -r .browser_download_url)
echo "APK Download URL: $APK_DOWNLOAD_URL"

echo "Updating Gist..."
cat <<EOF > update.json
{
  "versionCode": 66,
  "versionName": "2.8.0",
  "apkUrl": "$APK_DOWNLOAD_URL",
  "releaseNotes": "Fix bugs, add caching, fix global rating issue",
  "sha256": "$SHA256"
}
EOF

GIST_ID="7c19255da1430800f0030ba3c6e99765"
PAYLOAD=$(jq -n --arg content "$(cat update.json)" '{"files":{"update.json":{"content":$content}}}')
curl -s -X PATCH -H "Authorization: Bearer $TOKEN" -H "Accept: application/vnd.github+json" -d "$PAYLOAD" "https://api.github.com/gists/$GIST_ID"

echo "Done."
