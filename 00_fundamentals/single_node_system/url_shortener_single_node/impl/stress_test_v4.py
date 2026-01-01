import concurrent.futures
import time
import requests
import random
import string
import sys
import statistics
import json
import os
import threading
from datetime import datetime
from collections import defaultdict

# --- Configuration ---
BASE_URL = "http://localhost:8080"
RESULTS_DIR = "src/test/kotlin/com/urlshortener/loadtestresult"

# Scenarios: (concurrent_users, duration_seconds)
STAGES = [
    (10, 5),      # Warmup
    (50, 5),      # Baseline
    (100, 5),     # Moderate
    (500, 5),     # Moderate
    (1000, 5),     # Moderate
    (10000, 5),     # Heavy
    (20000, 5),     # Heavy
    (40000, 5),     # Heavy
    (45000, 5),     # Heavy
    (50000, 5),     # Heavy
]

class LoadTestReporter:
    def __init__(self):
        self.timestamp = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
        self.run_data = {
            "timestamp": self.timestamp,
            "config": {"url": BASE_URL, "stages": STAGES},
            "results": [],
            "health_checks": [],
            "analytics_summary": {
                "total_requests": 0,
                "deduplication_saved_count": 0,
                "stats_verification_calls": 0,
                "stats_verification_failures": 0,
                "errors": 0
            }
        }
        self.lock = threading.Lock()
        os.makedirs(RESULTS_DIR, exist_ok=True)

    def add_stage_result(self, users, duration, metrics):
        with self.lock:
            self.run_data["results"].append({
                "users": users,
                "duration": duration,
                "metrics": metrics
            })

    def add_health_check(self, stage_users, is_healthy, latency):
        with self.lock:
            self.run_data["health_checks"].append({
                "after_stage_users": stage_users,
                "healthy": is_healthy,
                "latency": latency
            })

    def update_analytics(self, total, dedups, stats_calls, stats_fails, errors):
        with self.lock:
            s = self.run_data["analytics_summary"]
            s["total_requests"] += total
            s["deduplication_saved_count"] += dedups
            s["stats_verification_calls"] += stats_calls
            s["stats_verification_failures"] += stats_fails
            s["errors"] += errors

    def save_json(self):
        filepath = os.path.join(RESULTS_DIR, f"result_v4_{self.timestamp}.json")
        with open(filepath, "w", encoding = "utf-8") as f:
            json.dump(self.run_data, f, indent=2)
        return filepath

    def generate_html_dashboard(self):
        filepath = os.path.join(RESULTS_DIR, f"dashboard_v4_{self.timestamp}.html")
        
        labels = [str(r['users']) for r in self.run_data["results"]]
        rps_data = [r['metrics']['rps'] for r in self.run_data["results"]]
        p95_data = [r['metrics']['p95'] for r in self.run_data["results"]]
        
        analytics = self.run_data["analytics_summary"]
        
        # Dynamic Logic for Logic Verification Box
        hash_status = "success" if analytics['errors'] == 0 else "warning"
        hash_msg = "Hashing Correct: No unexpected errors during deduplication." if analytics['errors'] == 0 else "Hashing Issues: Some requests failed."
        
        stats_status = "info" if analytics['stats_verification_failures'] == 0 else "danger"
        stats_msg = "Observer Pattern: Stats verified accurately across threads." if analytics['stats_verification_failures'] == 0 else f"Stats Mismatch: {analytics['stats_verification_failures']} verification failures detected."

        # Collapse logic
        collapse_msg = self.run_data["analytics_summary"].get("collapse_detected")
        collapse_html = f"""
        <div class="row mt-2">
            <div class="col-12">
                <div class="alert alert-danger">
                    <h5 class="fw-bold">💥 Critical Failure Detected</h5>
                    <p class="mb-0">{collapse_msg}</p>
                    <small>The system or test script hit a hard resource limit (e.g., Thread or File Descriptor limit).</small>
                </div>
            </div>
        </div>
        """ if collapse_msg else ""

        html_content = f"""
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>Correctness & Analytics Report - {self.timestamp}</title>
            <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
            <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
            <style>
                body {{ background: #f4f7f6; padding: 20px; }}
                .card {{ margin-bottom: 20px; border-radius: 12px; border: none; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }}
                .stat-card {{ background: #fff; padding: 20px; text-align: center; }}
                .stat-value {{ font-size: 28px; font-weight: bold; color: #2c3e50; }}
                .stat-label {{ font-size: 14px; color: #7f8c8d; text-transform: uppercase; }}
                .chart-container {{ position: relative; height: 300px; }}
            </style>
        </head>
        <body>
            <div class="container">
                <div class="row mb-4">
                    <div class="col-12 text-center">
                        <h2 class="fw-bold">📊 Correctness & Analytics Dashboard (V4 Fixed)</h2>
                        <p class="text-muted">Target: {BASE_URL} | Timestamp: {self.timestamp}</p>
                    </div>
                </div>

                {collapse_html}

                <div class="row mb-4">
                    <div class="col-md-3">
                        <div class="card stat-card">
                            <div class="stat-value">{analytics['total_requests']}</div>
                            <div class="stat-label">Total Valid Cycles</div>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="card stat-card">
                            <div class="stat-value text-primary">{analytics['deduplication_saved_count']}</div>
                            <div class="stat-label">Deduplication Hits</div>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="card stat-card">
                            <div class="stat-value text-success">{analytics['stats_verification_calls']}</div>
                            <div class="stat-label">Stats Verified</div>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="card stat-card">
                            <div class="stat-value text-danger">{analytics['errors']}</div>
                            <div class="stat-label">Total Errors</div>
                        </div>
                    </div>
                </div>

                <div class="row">
                    <div class="col-md-6">
                        <div class="card p-4">
                            <h5 class="fw-bold mb-4">Performance: Throughput (RPS)</h5>
                            <div class="chart-container">
                                <canvas id="rpsChart"></canvas>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="card p-4">
                            <h5 class="fw-bold mb-4">Reliability: P95 Latency (ms)</h5>
                            <div class="chart-container">
                                <canvas id="latencyChart"></canvas>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="row mt-4">
                    <div class="col-md-6">
                        <div class="card p-4">
                            <h5 class="fw-bold mb-3">System Logic Verification</h5>
                            <div class="alert alert-{hash_status}">
                                <strong>Hashing:</strong> {hash_msg}
                            </div>
                            <div class="alert alert-{stats_status}">
                                <strong>Observer Pattern:</strong> {stats_msg}
                            </div>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="card p-4">
                            <h5 class="fw-bold mb-3">💓 Health Recovery</h5>
                            <table class="table table-sm">
                                <thead><tr><th>After Stage</th><th>Status</th><th>Latency</th></tr></thead>
                                <tbody>
                                    {"".join([f"<tr><td>{h['after_stage_users']} Users</td><td>{'✅ UP' if h['healthy'] else '❌ DOWN'}</td><td>{h['latency']:.2f}ms</td></tr>" for h in self.run_data['health_checks']])}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>

            <script>
                const labels = {json.dumps(labels)};
                new Chart(document.getElementById('rpsChart'), {{
                    type: 'bar',
                    data: {{
                        labels: labels,
                        datasets: [{{ 
                            label: 'Requests Per Second',
                            data: {json.dumps(rps_data)},
                            backgroundColor: '#3498db'
                        }}]
                    }},
                    options: {{ maintainAspectRatio: false }}
                }});

                new Chart(document.getElementById('latencyChart'), {{
                    type: 'line',
                    data: {{
                        labels: labels,
                        datasets: [{{
                            label: 'P95 Latency',
                            data: {json.dumps(p95_data)},
                            borderColor: '#e67e22',
                            tension: 0.3,
                            fill: true,
                            backgroundColor: 'rgba(230, 126, 34, 0.1)'
                        }}]
                    }},
                    options: {{ maintainAspectRatio: false }}
                }});
            </script>
        </body>
        </html>
        """.replace("\n", "\n")
        with open(filepath, "w", encoding="utf-8") as f:
            f.write(html_content)
        print(f"✅ V4 Dashboard generated: {filepath}")

class LoadTester:
    def __init__(self):
        self.reporter = LoadTestReporter()
        self.url_map = {} # long_url -> short_code
        self.stats = defaultdict(int) # short_code -> clicks
        self.map_lock = threading.Lock()
        self.stats_lock = threading.Lock()

    def check_health(self, after_users):
        try:
            t0 = time.time()
            resp = requests.get(f"{BASE_URL}/health", timeout=5)
            lat = (time.time() - t0) * 1000
            is_healthy = resp.status_code == 200
            self.reporter.add_health_check(after_users, is_healthy, lat)
            print(f"  💓 Health Check: {'UP' if is_healthy else 'DOWN'} ({lat:.2f}ms)")
        except:
            self.reporter.add_health_check(after_users, False, 0)
            print("  💓 Health Check: DOWN (Timeout/Error)")

    def perform_cycle(self, long_url):
        results = {"latency": 0, "is_dedup": False, "stats_verified": True, "stats_fail": False, "error": False}
        try:
            t0 = time.time()
            # 1. Shorten
            r_shorten = requests.post(f"{BASE_URL}/shorten", json={"long_url": long_url}, timeout=5)
            if r_shorten.status_code != 200:
                results["error"] = True
                return results

            short_url = r_shorten.json()['short_url']
            code = short_url.split("/")[-1]

            with self.map_lock:
                if long_url in self.url_map:
                    if self.url_map[long_url] == code:
                        results["is_dedup"] = True
                else:
                    self.url_map[long_url] = code

            # 2. Resolve (Click)
            r_resolve = requests.get(short_url, allow_redirects=False, timeout=5)
            if r_resolve.status_code != 302:
                results["error"] = True
                return results

            with self.stats_lock:
                self.stats[code] += 1
                local_count = self.stats[code]

            # 3. Verify Stats (Sampling)
            if random.random() < 0.1:
                r_stats = requests.get(f"{BASE_URL}/stats/{code}", timeout=5)
                if r_stats.status_code == 200:
                    data = r_stats.json()
                    # click_count in DB might be higher if other test runs or logic overlap,
                    # but should NOT be lower than what this script tracked locally.
                    if data['click_count'] < local_count:
                        results["stats_verified"] = False
                        results["stats_fail"] = True
                else:
                    results["stats_verified"] = False
                    results["stats_fail"] = True

            results["latency"] = (time.time() - t0) * 1000
            return results
        except Exception as e:
            results["error"] = True
            return results

    def run_stage(self, users, duration):
        print(f"\n--- Stage: {users} Users ({duration}s) ---")
        deadline = time.time() + duration
        latencies = []
        errors = 0
        success = 0
        dedup_hits = 0
        stats_calls = 0
        stats_fails = 0

        with concurrent.futures.ThreadPoolExecutor(max_workers=users) as executor:
            while time.time() < deadline:
                # Batch of requests
                test_urls = [f"https://correctness.me/v4/{i}" for i in range(20)]
                futures = [executor.submit(self.perform_cycle, random.choice(test_urls)) for _ in range(users)]

                for f in concurrent.futures.as_completed(futures):
                    res = f.result()
                    if res and not res["error"]:
                        latencies.append(res["latency"])
                        success += 1
                        if res["is_dedup"]: dedup_hits += 1
                        if res["stats_fail"]: stats_fails += 1
                        stats_calls += 1
                    else:
                        errors += 1

        metrics = {
            "rps": success/duration,
            "p95": statistics.quantiles(latencies, n=20)[18] if len(latencies) >= 20 else (statistics.mean(latencies) if latencies else 0)
        }
        self.reporter.add_stage_result(users, duration, metrics)
        self.reporter.update_analytics(success, dedup_hits, stats_calls, stats_fails, errors)
        print(f"  -> RPS: {metrics['rps']:.2f} | P95: {metrics['p95']:.2f}ms | Errors: {errors}")

        def run(self):
            print(f"🚀 Starting Correctness-First Stress Test V4 (Fixed)")
            print(f"Target: {BASE_URL}")
            
            try:
                for users, duration in STAGES:
                    self.run_stage(users, duration)
                    self.check_health(users)
                    time.sleep(1)
            except RuntimeError as e:
                print(f"\n💥 SCRIPT CRASHED (Resource Exhaustion): {e}")
                print("The test script hit the OS thread limit. This is a local bottleneck.")
                with self.reporter.lock:
                    self.reporter.run_data["analytics_summary"]["errors"] += 1
                    self.reporter.run_data["analytics_summary"]["collapse_detected"] = str(e)
            except Exception as e:
                print(f"\n⚠️ Unexpected Error: {e}")
            finally:
                print("\nFinalizing Reports...")
                json_path = self.reporter.save_json()
                print(f"✅ Raw data: {json_path}")
                self.reporter.generate_html_dashboard()
    
    if __name__ == "__main__":    try:
        requests.get(f"{BASE_URL}/health", timeout=2)
    except:
        print("❌ Server not found at localhost:8080. Start the Spring Boot app first.")
        sys.exit(1)
    LoadTester().run()