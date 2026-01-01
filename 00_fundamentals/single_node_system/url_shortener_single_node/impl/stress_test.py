import concurrent.futures
import time
import requests
import random
import string
import sys
import statistics
from collections import defaultdict

# --- Configuration ---
BASE_URL = "http://localhost:8080"
# Scenarios to run: (concurrent_users, duration_seconds)
STAGES = [
    (10, 5),    # Warmup
    (50, 10),   # Light Load
    (200, 10),  # Medium Load
    (500, 10),  # Heavy Load
    (1000, 10), # Stress (Approaching max for local usually)
    (2000, 10)  # Breakpoint?
]

class LoadTester:
    def __init__(self):
        self.results = defaultdict(list)
        self.errors = 0
        self.success = 0
        self.start_time = 0

    def generate_random_url(self):
        return f"https://example.com/{''.join(random.choices(string.ascii_letters, k=10))}"

    def single_request(self):
        try:
            # 1. Shorten
            long_url = self.generate_random_url()
            t0 = time.time()
            resp = requests.post(f"{BASE_URL}/shorten", json={"long_url": long_url}, timeout=5)
            t1 = time.time()
            
            if resp.status_code == 200:
                short_url = resp.json()['short_url']
                short_code = short_url.split("/")[-1]
                
                # 2. Resolve (Read)
                requests.get(f"{BASE_URL}/{short_code}", allow_redirects=False, timeout=5)
                t2 = time.time()
                
                # Record Latency (Write + Read)
                return (t2 - t0) * 1000 # to ms
            else:
                return None
        except Exception:
            return None

    def run_stage(self, concurrent_users, duration):
        print(f"\n--- Running Stage: {concurrent_users} Concurrent Users for {duration}s ---")
        
        deadline = time.time() + duration
        latencies = []
        local_errors = 0
        local_success = 0
        
        with concurrent.futures.ThreadPoolExecutor(max_workers=concurrent_users) as executor:
            while time.time() < deadline:
                # Batch submit tasks equal to concurrency level
                futures = [executor.submit(self.single_request) for _ in range(concurrent_users)]
                
                for f in concurrent.futures.as_completed(futures):
                    res = f.result()
                    if res:
                        latencies.append(res)
                        local_success += 1
                    else:
                        local_errors += 1
        
        self.print_metrics(concurrent_users, latencies, local_success, local_errors, duration)

    def print_metrics(self, users, latencies, success, error, duration):
        if not latencies:
            print("  -> No successful requests.")
            return

        avg_lat = statistics.mean(latencies)
        p95_lat = statistics.quantiles(latencies, n=20)[18] if len(latencies) > 20 else avg_lat
        p99_lat = statistics.quantiles(latencies, n=100)[98] if len(latencies) > 100 else avg_lat
        rps = success / duration

        print(f"  Results:")
        print(f"  - Throughput: {rps:.2f} RPS")
        print(f"  - Latency: Avg={avg_lat:.2f}ms, P95={p95_lat:.2f}ms, P99={p99_lat:.2f}ms")
        print(f"  - Success: {success}")
        print(f"  - Errors: {error} ({(error/(success+error))*100:.2f}%)")

def check_server():
    print("Checking if server is up...")
    for _ in range(10):
        try:
            requests.get(f"{BASE_URL}/api-docs", timeout=2)
            print("Server is UP.")
            return True
        except:
            time.sleep(1)
            print(".", end="", flush=True)
    print("\nServer not found on localhost:8080. Please start it using ./gradlew bootRun")
    return False

if __name__ == "__main__":
    if not check_server():
        sys.exit(1)
    
    tester = LoadTester()
    for users, duration in STAGES:
        tester.run_stage(users, duration)
        time.sleep(2) # Cool down
