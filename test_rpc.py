import urllib.request
import json

URL = "https://mzlxjobibskxhgywszff.supabase.co/rest/v1"
KEY = "sb_publishable_nKC_zzEY-e0szNHRGqy7ag_EAMQNYCW"

headers = {
    "apikey": KEY,
    "Authorization": f"Bearer {KEY}",
    "Content-Type": "application/json"
}

req = urllib.request.Request(f"{URL}/rpc/execute_sql", headers=headers, method="POST", data=json.dumps({"sql": "SELECT 1"}).encode())
try:
    with urllib.request.urlopen(req) as response:
        print(response.read().decode())
except Exception as e:
    print("Error:", e)
    if hasattr(e, 'read'):
        print(e.read().decode())
