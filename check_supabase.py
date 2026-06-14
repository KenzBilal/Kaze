import urllib.request
import json

URL = "https://mzlxjobibskxhgywszff.supabase.co/rest/v1"
KEY = "sb_publishable_nKC_zzEY-e0szNHRGqy7ag_EAMQNYCW"

headers = {
    "apikey": KEY,
    "Authorization": f"Bearer {KEY}",
    "Range-Unit": "items"
}

def check_table(table_name):
    req = urllib.request.Request(f"{URL}/{table_name}?select=*", headers=headers)
    try:
        with urllib.request.urlopen(req) as response:
            data = json.loads(response.read().decode())
            print(f"Table '{table_name}' has {len(data)} items (up to limit).")
            if len(data) > 0:
                print(f"Sample data: {data[0]}")
    except Exception as e:
        print(f"Error querying {table_name}: {e}")

check_table("users")
check_table("activity_feed")
check_table("public_watchlist")
