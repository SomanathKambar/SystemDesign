import concurrent.futures
import json
import os
import statistics
import time
from datetime import datetime

import requests

# --- Configuration ---
BASE_URL = "http://localhost:8080"
RESULTS_DIR = "src/test/kotlin/com/urlshortener/loadtestresult"

# Dynamic Scaling Params
START_USERS = 100
MAX_USERS = 10000     # Absolute safety cap
STEP_SIZE = 500       # Users to add each iteration
STEP_DURATION = 10    # Seconds to run each step

# Thresholds for Breakpoint/Throttle
MAX_P95_MS = 800      # Mark as "Throttled" if P95 > 800ms
MAX_ERROR_RATE = 0.05 # Stop test if > 5% requests fail

class DynamicStressTester:
    def __init__(self):
        self.timestamp = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
        self.results = []
        self.breakpoint_found = False
        os.makedirs(RESULTS_DIR, exist_ok=True)

    def perform_cycle(self):
        try:
            start = time.time()
            # Minimal payload for maximum RPS discovery
            r = requests.post(f"{BASE_URL}/shorten", json={"long_url": "https://test.com"}, timeout=3)
            lat = (time.time() - start) * 1000
            return (lat, r.status_code == 200)
        except Exception:
            return (0, False)

    def run_step(self, user_count):
        latencies = []
        success_count = 0
        total_count = 0

        with concurrent.futures.ThreadPoolExecutor(max_workers=user_count) as executor:
            end_time = time.time() + STEP_DURATION
            while time.time() < end_time:
                futures = [executor.submit(self.perform_cycle) for _ in range(user_count)]
                for f in concurrent.futures.as_completed(futures):
                    lat, success = f.result()
                    latencies.append(lat)
                    if success: success_count += 1
                    total_count += 1

        error_rate = (total_count - success_count) / total_count if total_count > 0 else 1
        metrics = {
            "users": user_count,
            "rps": total_count / STEP_DURATION,
            "avg": statistics.mean(latencies) if latencies else 0,
            "p95": statistics.quantiles(latencies, n=20)[-1] if len(latencies) > 1 else 0,
            "error_rate": error_rate
        }
        return metrics

    def execute(self):
        current_users = START_USERS
        print(f"🚀 Starting Dynamic Stress Test on {BASE_URL}")

        try:
            while current_users <= MAX_USERS:
                print(f"-> Testing {current_users} users...")
                metrics = self.run_step(current_users)
                self.results.append(metrics)

                print(f"   RPS: {metrics['rps']:.2f} | P95: {metrics['p95']:.2f}ms | Errors: {metrics['error_rate']*100:.1f}%")

                # BREAKPOINT CHECK: Hard Failure
                if metrics['error_rate'] > MAX_ERROR_RATE:
                    print(f"💥 BREAKPOINT REACHED at {current_users} users (Error Rate: {metrics['error_rate']*100:.1f}%)")
                    self.breakpoint_found = True
                    break

                # THROTTLE CHECK: Performance Degradation
                if metrics['p95'] > MAX_P95_MS:
                    print(f"⚠️ THROTTLING detected at {current_users} users (P95: {metrics['p95']:.2f}ms)")

                # SATURATION CHECK: If RPS drops compared to previous step
                if len(self.results) > 1:
                    prev_rps = self.results[-2]['rps']
                    if metrics['rps'] < (prev_rps * 0.95): # 5% drop
                        print(f"🛑 SATURATION POINT: Throughput dropped at {current_users} users.")
                        break

                current_users += STEP_SIZE

        except KeyboardInterrupt:
            print("\n🛑 Manual Stop.")
        finally:
            self.save_final_report()

    def save_final_report(self):
        filename = os.path.join(RESULTS_DIR, f"stress_test_{self.timestamp}.json")
        with open(filename, "w", encoding="utf-8") as f:
            json.dump(self.results, f, indent=2)
        print(f"✅ Stress report saved: {filename}")

if __name__ == "__main__":
    DynamicStressTester().execute()
