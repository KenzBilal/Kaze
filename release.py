import json
import urllib.request
import urllib.parse
import os
import ssl

token = "REDACTED_TOKEN"
headers = {"Authorization": f"Bearer {token}", "Accept": "application/vnd.github+json", "User-Agent": "python"}
ctx = ssl.create_default_context()

# 1. Fetch release to get upload URL
req = urllib.request.Request("https://api.github.com/repos/KenzBilal/Kaze/releases/tags/v2.8.0", headers=headers)
with urllib.request.urlopen(req, context=ctx) as res:
    release = json.loads(res.read().decode())
    upload_url = release['upload_url'].split('{')[0]

# 2. Upload APK
apk_path = "app/build/outputs/apk/debug/app-debug.apk"
with open(apk_path, "rb") as f:
    apk_data = f.read()

upload_req = urllib.request.Request(f"{upload_url}?name=app-debug.apk", data=apk_data, headers={
    **headers,
    "Content-Type": "application/vnd.android.package-archive"
}, method="POST")

with urllib.request.urlopen(upload_req, context=ctx) as res:
    asset = json.loads(res.read().decode())
    apk_download_url = asset['browser_download_url']
    print(f"Uploaded APK to {apk_download_url}")

# 3. Read SHA256 (calculated earlier)
import hashlib
with open(apk_path, "rb") as f:
    sha256 = hashlib.sha256(f.read()).hexdigest()

# 4. Update Gist
update_json = {
  "versionCode": 66,
  "versionName": "2.8.0",
  "apkUrl": apk_download_url,
  "releaseNotes": "Fix bugs, add caching, fix global rating issue",
  "sha256": sha256
}

gist_payload = json.dumps({
    "files": {
        "update.json": {
            "content": json.dumps(update_json, indent=2)
        }
    }
}).encode('utf-8')

gist_req = urllib.request.Request("https://api.github.com/gists/7c19255da1430800f0030ba3c6e99765", data=gist_payload, headers=headers, method="PATCH")
with urllib.request.urlopen(gist_req, context=ctx) as res:
    print("Gist updated!")
