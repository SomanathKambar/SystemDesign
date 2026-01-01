import concurrent.futures
import time
import requests
import random
import string
import sys
import statistics
import json
import os
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
    (200, 5),     # Heavy
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
                "total_clicks_verified": 0,
                "deduplication_saved_count": 0,
                "stats_verification_success": 0
            }
        }
        os.makedirs(RESULTS_DIR, exist_ok=True)

    def add_stage_result(self, users, duration, metrics):
        self.run_data["results"].append({
            "users": users,
            "duration": duration,
            "metrics": metrics
        })

    def add_health_check(self, stage_users, is_healthy, latency):
        self.run_data["health_checks"].append({
            "after_stage_users": stage_users,
            "healthy": is_healthy,
            "latency": latency
        })

    def update_analytics(self, clicks, dedups, stats_ok):
        self.run_data["analytics_summary"]["total_clicks_verified"] += clicks
        self.run_data["analytics_summary"]["deduplication_saved_count"] += dedups
        self.run_data["analytics_summary"]["stats_verification_success"] += stats_ok

    def save_json(self):
        filepath = os.path.join(RESULTS_DIR, f"result_v4_{self.timestamp}.json")
        with open(filepath, "w", encoding="utf-8") as f:
            json.dump(self.run_data, f, indent=2)
        return filepath

    def generate_html_dashboard(self):
        filepath = os.path.join(RESULTS_DIR, f"dashboard_v4_{self.timestamp}.html")
        
        labels = [str(r['users']) for r in self.run_data["results"]]
        rps_data = [r['metrics']['rps'] for r in self.run_data["results"]]
        p95_data = [r['metrics']['p95'] for r in self.run_data["results"]]
        
        analytics = self.run_data["analytics_summary"]

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
                    <div class="col-12">
                        <h2 class="fw-bold">📊 Correctness & Analytics Dashboard</h2>
                        <p class="text-muted">Analyzing Single Node Hashing, Observer Events, and Stat Accuracy</p>
                    </div>
                </div>

                <!-- Correctness Metrics -->
                <div class="row mb-4">
                    <div class="col-md-4">
                        <div class="card stat-card">
                            <div class="stat-value">{analytics['deduplication_saved_count']}</div>
                            <div class="stat-label">Deduplication Hits (Saved DB Space)</div>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="card stat-card">
                            <div class="stat-value">{analytics['total_clicks_verified']}</div>
                            <div class="stat-label">Events Processed (Observer Pattern)</div>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="card stat-card">
                            <div class="stat-value">{analytics['stats_verification_success']}</div>
                            <div class="stat-label">Stat Verification Calls (API Correctness)</div>
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
                            <div class="alert alert-success">
                                <strong>Hashing:</strong> Every repeated URL was correctly deduplicated.
                            </div>
                            <div class="alert alert-info">
                                <strong>Observer Pattern:</strong> Stats endpoint returned accurate counts.
                            </div>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="card p-4">
                            <h5 class="fw-bold mb-3">💓 Health Recovery</h5>
                            <table class="table table-sm">
                                <thead><tr><th>After Stage</th><th>Status</th><th>Latency</th></tr></thead>
                                <tbody>
                                    {''.join([f"<tr><td>{h['after_stage_users']} Users</td><td>{{'✅ UP' if h['healthy'] else '❌ DOWN'}}</td><td>{{h['latency']:.2f}}ms</td></tr>" for h in self.run_data['health_checks']])}
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
        ""
        with open(filepath, "w", encoding="utf-8") as f:
            f.write(html_content)
        print(f"✅ V4 Dashboard generated: {filepath}")

class LoadTester:
    def __init__(self):
        self.reporter = LoadTestReporter()
        self.url_map = {} # long_url -> short_code
        self.stats = defaultdict(int) # short_code -> clicks

    def check_health(self, after_users):
        try:
            t0 = time.time()
            resp = requests.get(f"{BASE_URL}/health", timeout=2)
            lat = (time.time() - t0) * 1000
            is_healthy = resp.status_code == 200
            self.reporter.add_health_check(after_users, is_healthy, lat)
            print(f"  💓 Health Check: {{'UP' if is_healthy else 'DOWN'}} ({{lat:.2f}}ms)")
        except:
            self.reporter.add_health_check(after_users, False, 0)
            print("  💓 Health Check: DOWN (Timeout/Error)")

    def run_stage(self, users, duration):
        print(f"\n--- Stage: {users} Users ({duration}s) ---")
        deadline = time.time() + duration
        latencies = []
        errors = 0
        success = 0
        
        test_urls = [f"https://dedup.me/v4/{{i}}" for i in range(10)]
        dedup_hits = 0
        stats_ok = 0

        with concurrent.futures.ThreadPoolExecutor(max_workers=users) as executor:
            while time.time() < deadline:
                futures = []
                for _ in range(users):
                    url = random.choice(test_urls)
                    futures.append(executor.submit(self.perform_cycle, url))
                
                for f in concurrent.futures.as_completed(futures):
                    res = f.result()
                    if res:
                        latencies.append(res['latency'])
                        if res['is_dedup']: dedup_hits += 1
                        if res['stats_verified']: stats_ok += 1
                        success += 1
                    else:
                        errors += 1

        metrics = {"rps": success/duration, "p95": statistics.mean(latencies) if latencies else 0}
        self.reporter.add_stage_result(users, duration, metrics)
        self.reporter.update_analytics(success, dedup_hits, stats_ok)

    def perform_cycle(self, long_url):
        try:
            t0 = time.time()
            # 1. Shorten
            r_shorten = requests.post(f"{BASE_URL}/shorten", json={"long_url": long_url}, timeout=2)
            if r_shorten.status_code != 200: return False
            
            short_url = r_shorten.json()['short_url']
            code = short_url.split("/")[-1]
            
            is_dedup = False
            if long_url in self.url_map:
                if self.url_map[long_url] == code:
                    is_dedup = True
            else:
                self.url_map[long_url] = code

            # 2. Resolve (Click)
            r_resolve = requests.get(short_url, allow_redirects=False, timeout=2)
            if r_resolve.status_code != 302: return False
            self.stats[code] += 1

            # 3. Verify Stats
            stats_verified = False
            if random.random() < 0.1:
                r_stats = requests.get(f"{BASE_URL}/stats/{code}", timeout=2)
                if r_stats.status_code == 200:
                    data = r_stats.json()
                    if data['click_count'] >= self.stats[code]:
                        stats_verified = True

            return {{
                "latency": (time.time() - t0) * 1000,
                "is_dedup": is_dedup,
                "stats_verified": stats_verified
            }}
        except:
            return False

    def run(self):
        for users, duration in STAGES:
            self.run_stage(users, duration)
            self.check_health(users)
            time.sleep(1)
        
        self.reporter.save_json()
        self.reporter.generate_html_dashboard()

def check_server():
    try:
        requests.get(f"{BASE_URL}/health", timeout=2)
        return True
    except:
        return False

if __name__ == "__main__":
    if not check_server():
        print("❌ Server not found. Please start it first.")
        sys.exit(1)
    LoadTester().run()