import os
import json
import urllib.request

url = None
key = None
with open("local.properties", "r") as f:
    for line in f:
        if line.startswith("SUPABASE_URL="):
            url = line.split("=")[1].strip()
        if line.startswith("SUPABASE_KEY="):
            key = line.split("=")[1].strip()

req1 = urllib.request.Request(f"{url}/rest/v1/global_chat_members?limit=1", headers={"apikey": key, "Authorization": f"Bearer {key}"})
with urllib.request.urlopen(req1) as response:
    print("global_chat_members:", json.loads(response.read().decode()))

req2 = urllib.request.Request(f"{url}/rest/v1/global_chat_messages?limit=1", headers={"apikey": key, "Authorization": f"Bearer {key}"})
with urllib.request.urlopen(req2) as response:
    print("global_chat_messages:", json.loads(response.read().decode()))
