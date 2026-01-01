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
    (50, 10),     # Baseline
    (200, 10),    # Moderate
    (500, 10),    # Heavy
    (1000, 10),   # Stress
    (2000, 10),   # Saturation
    (3000, 5)     # Breakpoint Attempt
]

# Attack Payloads for Penetration Testing
ATTACK_PAYLOADS = [
    "' OR '1'='1",                      # Basic SQL Injection
    "<script>alert('XSS')</script>",    # XSS
    "A" * 10000,                        # Buffer/Large Payload
    "{{7*7}}",                          # Template Injection
    "../../etc/passwd"                  # Path Traversal
]

class LoadTestReporter:
    def __init__(self):
        self.timestamp = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
        self.run_data = {
            "timestamp": self.timestamp,
            "config": {"url": BASE_URL, "stages": STAGES},
            "results": [],
            "health_checks": [],
            "security_report": {"attempts": 0, "blocked": 0, "leaked_500": 0, "successful_200": 0}
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

    def record_security_event(self, status_code):
        self.run_data["security_report"]["attempts"] += 1
        if 400 <= status_code < 500:
            self.run_data["security_report"]["blocked"] += 1
        elif status_code >= 500:
            self.run_data["security_report"]["leaked_500"] += 1
        elif status_code == 200:
            self.run_data["security_report"]["successful_200"] += 1

    def save_json(self):
        filepath = os.path.join(RESULTS_DIR, f"result_v3_{self.timestamp}.json")
        with open(filepath, "w", encoding="utf-8") as f:
            json.dump(self.run_data, f, indent=2)
        print(f"✅ Raw data saved to: {filepath}")
        return filepath

    def generate_markdown_report(self):
        filepath = os.path.join(RESULTS_DIR, f"report_v3_{self.timestamp}.md")
        lines = [
            f"# 🚀 Reliability & Scale Report",
            f"**Date:** {self.timestamp}  ",
            f"**Target:** `{BASE_URL}`",
            "",
            "## 1. Executive Summary",
            "| Concurrency | Throughput (RPS) | Avg Latency (ms) | P99 Latency (ms) | Error Rate % |",
            "|---|---|---|---|---|",
        ]

        for r in self.run_data["results"]:
            m = r["metrics"]
            status = "🔴" if m["p99"] > 2000 or m["error_rate"] > 5.0 else "🟢"
            lines.append(
                f"| {r['users']} | {m['rps']:.2f} | {m['avg']:.2f} | {m['p99']:.2f} | {m['error_rate']:.2f}% {status} |"
            )

        lines.extend([
            "",
            "## 2. Security Report",
            f"- **Total Attempts:** {self.run_data['security_report']['attempts']}",
            f"- **Blocked (4xx):** {self.run_data['security_report']['blocked']}",
            f"- **Leaked (500):** {self.run_data['security_report']['leaked_500']}",
            f"- **Successful (200):** {self.run_data['security_report']['successful_200']}",
            "",
            "## 3. Detailed Stage Logs",
        ])

        for r in self.run_data["results"]:
            m = r["metrics"]
            lines.append(f"### Stage: {r['users']} Concurrent Users")
            lines.append(f"- **RPS:** {m['rps']:.2f}")
            lines.append(f"- **Avg Latency:** {m['avg']:.2f}ms")
            lines.append(f"- **P95:** {m['p95']:.2f}ms")
            lines.append(f"- **P99:** {m['p99']:.2f}ms")
            lines.append(f"- **Error Rate:** {m['error_rate']:.2f}%")
            lines.append("")

        with open(filepath, "w", encoding="utf-8") as f:
            f.write("\n".join(lines))
        print(f"✅ Markdown report saved to: {filepath}")

    def determine_recommendation(self):
        # Heuristic Logic
        max_rps = 0
        max_users_at_peak = 0
        breakpoint_users = None
        failure_reason = "None"

        for r in self.run_data["results"]:
            m = r["metrics"]
            if m['rps'] > max_rps:
                max_rps = m['rps']
                max_users_at_peak = r['users']
            
            # Detect Breakpoint
            if m['error_rate'] > 5.0:
                breakpoint_users = r['users']
                failure_reason = "High Error Rate (>5%) - Availability Failure"
                break
            if m['p99'] > 2000:
                breakpoint_users = r['users']
                failure_reason = "High Latency (>2s) - Performance Degradation"
                break

        rec = ""
        if not breakpoint_users:
            rec = "✅ **Healthy:** System handled all load stages without breaking. Scale tests higher."
        elif "Latency" in failure_reason:
            rec = f"⚠️ **Vertical Scaling Required:** System slows down at {breakpoint_users} users. <br>The database or CPU is likely the bottleneck. Increase RAM/CPU or optimize DB queries."
        elif "Error" in failure_reason:
            rec = f"⚠️ **Horizontal Scaling Required:** System throws errors at {breakpoint_users} users. <br>Thread pool or connection limits reached. Add more nodes/pods behind a Load Balancer."

        return breakpoint_users, failure_reason, max_rps, rec

    def generate_html_dashboard(self):
        filepath = os.path.join(RESULTS_DIR, f"dashboard_v3_{self.timestamp}.html")
        
        labels = [str(r['users']) for r in self.run_data["results"]]
        rps_data = [r['metrics']['rps'] for r in self.run_data["results"]]
        p95_data = [r['metrics']['p95'] for r in self.run_data["results"]]
        error_data = [r['metrics']['error_rate'] for r in self.run_data["results"]]
        
        breakpoint_val, failure_reason, peak_rps, recommendation = self.determine_recommendation()
        
        sec_report = self.run_data["security_report"]
        sec_color = "red" if sec_report["leaked_500"] > 0 else "orange" if sec_report["successful_200"] > 0 else "green"

        html_content = f"""
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>Reliability & Scale Report - {self.timestamp}</title>
            <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
            <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
            <style>
                body {{ background: #f8f9fa; padding: 20px; }}
                .card {{ margin-bottom: 20px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); border: none; }}
                .card-header {{ font-weight: bold; background: #fff; border-bottom: 1px solid #eee; }}
                .rec-box {{ background: #e8f4fd; border-left: 5px solid #2196f3; padding: 15px; }}
                .kpi {{ font-size: 24px; font-weight: bold; }}
                .kpi-label {{ font-size: 12px; color: #666; text-transform: uppercase; }}
            </style>
        </head>
        <body>
            <div class="container">
                <div class="row mb-4">
                    <div class="col-12">
                        <h2 class="text-dark">🚀 System Reliability & Scale Report</h2>
                        <p class="text-muted">Run ID: {self.timestamp} | Target: {BASE_URL}</p>
                    </div>
                </div>

                <!-- KPIs -->
                <div class="row mb-4">
                    <div class="col-md-3">
                        <div class="card p-3 text-center">
                            <div class="kpi">{peak_rps:.0f}</div>
                            <div class="kpi-label">Peak RPS</div>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="card p-3 text-center">
                            <div class="kpi" style="color: {'red' if breakpoint_val else 'green'}">
                                {breakpoint_val if breakpoint_val else 'None'}
                            </div>
                            <div class="kpi-label">Breakpoint (Users)</div>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="card p-3">
                            <div class="rec-box">
                                <h5>💡 Recommendation</h5>
                                <p class="mb-0">{recommendation}</p>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Charts -->
                <div class="row">
                    <div class="col-md-6">
                        <div class="card">
                            <div class="card-header">Throughput & Error Rate</div>
                            <div class="card-body">
                                <canvas id="throughputChart"></canvas>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="card">
                            <div class="card-header">Latency Degradation</div>
                            <div class="card-body">
                                <canvas id="latencyChart"></canvas>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Security & Health -->
                <div class="row">
                    <div class="col-md-6">
                        <div class="card">
                            <div class="card-header">🛡️ Penetration Test (Basic DAST)</div>
                            <div class="card-body">
                                <ul class="list-group list-group-flush">
                                    <li class="list-group-item d-flex justify-content-between align-items-center">
                                        Total Injection Attempts
                                        <span class="badge bg-primary rounded-pill">{sec_report['attempts']}</span>
                                    </li>
                                    <li class="list-group-item d-flex justify-content-between align-items-center">
                                        Blocked (HTTP 4xx) - Good
                                        <span class="badge bg-success rounded-pill">{sec_report['blocked']}</span>
                                    </li>
                                    <li class="list-group-item d-flex justify-content-between align-items-center">
                                        Server Crashes (HTTP 500) - Bad
                                        <span class="badge bg-danger rounded-pill">{sec_report['leaked_500']}</span>
                                    </li>
                                    <li class="list-group-item d-flex justify-content-between align-items-center">
                                        Accepted (HTTP 200) - Risky
                                        <span class="badge bg-warning text-dark rounded-pill">{sec_report['successful_200']}</span>
                                    </li>
                                </ul>
                                <small class="text-muted mt-2 d-block">* Checks for SQLi, XSS, and Large Buffers.</small>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="card">
                            <div class="card-header">💓 Health Recovery</div>
                            <div class="card-body">
                                <table class="table table-sm">
                                    <thead><tr><th>After Stage</th><th>Status</th><th>Latency</th></tr></thead>
                                    <tbody>
                                        {''.join([f"<tr><td>{h['after_stage_users']} Users</td><td>{'✅ UP' if h['healthy'] else '❌ DOWN'}</td><td>{h['latency']:.2f}ms</td></tr>" for h in self.run_data['health_checks']])}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <script>
                const labels = {json.dumps(labels)};
                
                new Chart(document.getElementById('throughputChart'), {{
                    type: 'line',
                    data: {{
                        labels: labels,
                        datasets: [
                            {{
                                label: 'Throughput (RPS)',
                                data: {json.dumps(rps_data)},
                                borderColor: '#2196f3',
                                yAxisID: 'y'
                            }},
                            {{
                                label: 'Error Rate (%)',
                                data: {json.dumps(error_data)},
                                borderColor: '#f44336',
                                yAxisID: 'y1',
                                borderDash: [5, 5]
                            }}
                        ]
                    }},
                    options: {{
                        scales: {{
                            y: {{ type: 'linear', display: true, position: 'left', title: {{display: true, text: 'Req/Sec'}} }},
                            y1: {{ type: 'linear', display: true, position: 'right', title: {{display: true, text: 'Errors %'}}, grid: {{drawOnChartArea: false}} }}
                        }}
                    }}
                }});

                new Chart(document.getElementById('latencyChart'), {{
                    type: 'line',
                    data: {{
                        labels: labels,
                        datasets: [
                            {{
                                label: 'P95 Latency (ms)',
                                data: {json.dumps(p95_data)},
                                borderColor: '#ff9800',
                                backgroundColor: 'rgba(255, 152, 0, 0.1)',
                                fill: true
                            }}
                        ]
                    }},
                    options: {{
                        scales: {{ y: {{ title: {{display: true, text: 'Milliseconds'}} }} }}
                    }}
                }});
            </script>
        </body>
        </html>
        """.replace("\n", "\n") # Ensure newlines are preserved
        with open(filepath, "w", encoding="utf-8") as f:
            f.write(html_content)
        print(f"✅ Dashboard generated: {filepath}")

class LoadTester:
    def __init__(self):
        self.reporter = LoadTestReporter()

    def check_health(self, after_users):
        try:
            t0 = time.time()
            resp = requests.get(f"{BASE_URL}/api-docs", timeout=2)
            lat = (time.time() - t0) * 1000
            is_healthy = resp.status_code == 200
            self.reporter.add_health_check(after_users, is_healthy, lat)
            print(f"  💓 Health Check: {'UP' if is_healthy else 'DOWN'} ({lat:.2f}ms)")
        except:
            self.reporter.add_health_check(after_users, False, 0)
            print("  💓 Health Check: DOWN (Timeout/Error)")

    def generate_payload(self, is_attack=False):
        if is_attack:
            return random.choice(ATTACK_PAYLOADS)
        return f"https://example.com/{''.join(random.choices(string.ascii_letters, k=10))}"

    def single_request(self):
        # 5% chance of being an attack request
        is_attack = random.random() < 0.05
        payload = self.generate_payload(is_attack)
        
        try:
            t0 = time.time()
            resp = requests.post(f"{BASE_URL}/shorten", json={"long_url": payload}, timeout=5)
            
            # Security Reporting
            if is_attack:
                self.reporter.record_security_event(resp.status_code)
                return None # Don't count attacks towards latency stats directly

            # Standard Flow
            if resp.status_code == 200:
                short_url = resp.json()['short_url']
                short_code = short_url.split("/")[-1]
                requests.get(f"{BASE_URL}/{short_code}", allow_redirects=False, timeout=5)
                t2 = time.time()
                return (t2 - t0) * 1000 # ms
            else:
                return False # Error
        except:
            return False # Error

    def run(self):
        print(f"Starting System Reliability Test on {BASE_URL}...")
        print(f"Attacks Enabled: SQLi, XSS, Buffer Overflow")
        
        try:
            for users, duration in STAGES:
                self.run_stage(users, duration)
                self.check_health(users)
                time.sleep(2) 
        finally:
            print("\nGenerating Reliability Reports...")
            self.reporter.save_json()
            self.reporter.generate_markdown_report()
            self.reporter.generate_html_dashboard()

    def run_stage(self, concurrent_users, duration):
        print(f"\n--- Stage: {concurrent_users} Users ({duration}s) ---")
        deadline = time.time() + duration
        latencies = []
        errors = 0
        success = 0
        
        with concurrent.futures.ThreadPoolExecutor(max_workers=concurrent_users) as executor:
            while time.time() < deadline:
                futures = [executor.submit(self.single_request) for _ in range(concurrent_users)]
                for f in concurrent.futures.as_completed(futures):
                    res = f.result()
                    if res is None:
                        continue # Attack request, ignored for perf stats
                    if res is not False:
                        latencies.append(res)
                        success += 1
                    else:
                        errors += 1

        # Calculate Metrics
        metrics = {"rps": 0, "avg": 0, "p95": 0, "p99": 0, "error_rate": 0}
        
        if latencies:
            metrics["rps"] = success / duration
            metrics["avg"] = statistics.mean(latencies)
            metrics["p95"] = statistics.quantiles(latencies, n=20)[18] if len(latencies) > 20 else metrics["avg"]
            metrics["p99"] = statistics.quantiles(latencies, n=100)[98] if len(latencies) > 100 else metrics["avg"]
        
        total = success + errors
        if total > 0:
            metrics["error_rate"] = (errors / total) * 100

        print(f"  -> RPS: {metrics['rps']:.2f} | P99: {metrics['p99']:.2f}ms | Errors: {metrics['error_rate']:.2f}%")
        self.reporter.add_stage_result(concurrent_users, duration, metrics)

def check_server():
    try:
        requests.get(f"{BASE_URL}/api-docs", timeout=2)
        return True
    except:
        return False

if __name__ == "__main__":
    if not check_server():
        print("❌ Server not found. Please start it first.")
        sys.exit(1)
    
    LoadTester().run()
