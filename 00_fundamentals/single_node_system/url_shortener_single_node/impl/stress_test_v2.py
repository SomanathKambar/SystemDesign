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
    (10, 5),     # Warmup
    (50, 10),    # Light Load
    (200, 10),   # Medium Load
    (500, 10),   # Heavy Load
    (1000, 10),  # Stress
    (2000, 10)   # Breakpoint Search
]

class LoadTestReporter:
    def __init__(self):
        self.timestamp = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
        self.run_data = {
            "timestamp": self.timestamp,
            "config": {"url": BASE_URL, "stages": STAGES},
            "results": []
        }
        os.makedirs(RESULTS_DIR, exist_ok=True)

    def add_stage_result(self, users, duration, metrics):
        self.run_data["results"].append({
            "users": users,
            "duration": duration,
            "metrics": metrics
        })

    def save_json(self):
        filepath = os.path.join(RESULTS_DIR, f"result_{self.timestamp}.json")
        with open(filepath, "w") as f:
            json.dump(self.run_data, f, indent=2)
        print(f"✅ Raw data saved to: {filepath}")
        return filepath

    def generate_markdown_report(self):
        filepath = os.path.join(RESULTS_DIR, f"report_{self.timestamp}.md")
        lines = [
            f"# 🚀 Load Test Report",
            f"**Date:** {self.timestamp}  ",
            f"**Target:** `{BASE_URL}`",
            "",
            "## 1. Executive Summary",
            "| Concurrency | Throughput (RPS) | Avg Latency (ms) | P99 Latency (ms) | Error Rate % |",
            "|---|---|---|---|---|",
        ]

        for r in self.run_data["results"]:
            m = r["metrics"]
            # Flag rows where P99 > 1000ms or Errors > 1%
            status = "🔴" if m["p99"] > 1000 or m["error_rate"] > 1.0 else "🟢"
            lines.append(
                f"| {r['users']} | {m['rps']:.2f} | {m['avg']:.2f} | {m['p99']:.2f} | {m['error_rate']:.2f}% {status} |"
            )

        lines.extend([
            "",
            "## 2. Analysis",
            "**Bottleneck Detection:**",
            "- **Latency Knee:** Check where P99 latency jumps significantly.",
            "- **Throughput Cap:** Check where RPS stops increasing despite adding more users.",
            "",
            "## 3. Detailed Stage Logs",
        ])

        for r in self.run_data["results"]:
            m = r["metrics"]
            lines.append(f"### Stage: {r['users']} Concurrent Users")
            lines.append(f"- **Success:** {m['success_count']}")
            lines.append(f"- **Failures:** {m['error_count']}")
            lines.append(f"- **P50 (Median):** {m['p50']:.2f}ms")
            lines.append(f"- **P95:** {m['p95']:.2f}ms")
            lines.append("")

        with open(filepath, "w") as f:
            f.write("\n".join(lines))
        print(f"✅ Markdown report saved to: {filepath}")

    def generate_html_dashboard(self):
        filepath = os.path.join(RESULTS_DIR, f"dashboard_{self.timestamp}.html")
        
        # Extract data for charts
        labels = [str(r['users']) for r in self.run_data["results"]]
        rps_data = [r['metrics']['rps'] for r in self.run_data["results"]]
        p95_data = [r['metrics']['p95'] for r in self.run_data["results"]]
        p99_data = [r['metrics']['p99'] for r in self.run_data["results"]]
        avg_data = [r['metrics']['avg'] for r in self.run_data["results"]]

        html_content = f"""
        <!DOCTYPE html>
        <html>
        <head>
            <title>Load Test Dashboard - {self.timestamp}</title>
            <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
            <style>
                body {{ font-family: sans-serif; padding: 20px; background: #f4f4f9; }}
                .container {{ max_width: 1000px; margin: 0 auto; background: white; padding: 20px; box-shadow: 0 0 10px rgba(0,0,0,0.1); }}
                h1 {{ color: #333; }}
                .chart-box {{ margin-bottom: 40px; }}
            </style>
        </head>
        <body>
            <div class="container">
                <h1>Performance Dashboard</h1>
                <p>Run ID: {self.timestamp} | Target: {BASE_URL}</p>
                
                <div class="chart-box">
                    <canvas id="throughputChart"></canvas>
                </div>
                <div class="chart-box">
                    <canvas id="latencyChart"></canvas>
                </div>
            </div>

            <script>
                const labels = {json.dumps(labels)};
                
                new Chart(document.getElementById('throughputChart'), {{
                    type: 'line',
                    data: {{
                        labels: labels,
                        datasets: [{{
                            label: 'Throughput (Req/Sec)',
                            data: {json.dumps(rps_data)},
                            borderColor: 'rgb(75, 192, 192)',
                            tension: 0.1,
                            yAxisID: 'y'
                        }}]
                    }},
                    options: {{
                        responsive: true,
                        interaction: {{ mode: 'index', intersect: false }},
                        plugins: {{ title: {{ display: true, text: 'Throughput vs Concurrency' }} }},
                        scales: {{ x: {{ title: {{ display: true, text: 'Concurrent Users' }} }} }}
                    }}
                }});

                new Chart(document.getElementById('latencyChart'), {{
                    type: 'line',
                    data: {{
                        labels: labels,
                        datasets: [
                            {{
                                label: 'Avg Latency (ms)',
                                data: {json.dumps(avg_data)},
                                borderColor: 'rgb(54, 162, 235)',
                                tension: 0.1
                            }},
                            {{
                                label: 'P95 Latency (ms)',
                                data: {json.dumps(p95_data)},
                                borderColor: 'rgb(255, 205, 86)',
                                tension: 0.1
                            }},
                            {{
                                label: 'P99 Latency (ms)',
                                data: {json.dumps(p99_data)},
                                borderColor: 'rgb(255, 99, 132)',
                                tension: 0.1
                            }}
                        ]
                    }},
                    options: {{
                        responsive: true,
                        interaction: {{ mode: 'index', intersect: false }},
                        plugins: {{ title: {{ display: true, text: 'Latency vs Concurrency' }} }},
                        scales: {{ x: {{ title: {{ display: true, text: 'Concurrent Users' }} }} }}
                    }}
                }});
            </script>
        </body>
        </html>
"""
        with open(filepath, "w") as f:
            f.write(html_content)
        print(f"✅ HTML Dashboard generated at: {filepath}")

class LoadTester:
    def __init__(self):
        self.reporter = LoadTestReporter()

    def generate_random_url(self):
        return f"https://example.com/{''.join(random.choices(string.ascii_letters, k=10))}"

    def single_request(self):
        try:
            long_url = self.generate_random_url()
            t0 = time.time()
            resp = requests.post(f"{BASE_URL}/shorten", json={"long_url": long_url}, timeout=5) 
            
            if resp.status_code == 200:
                short_url = resp.json()['short_url']
                short_code = short_url.split("/")[-1]
                requests.get(f"{BASE_URL}/{short_code}", allow_redirects=False, timeout=5)
                t2 = time.time()
                return (t2 - t0) * 1000 # ms
            return None
        except:
            return None

    def run(self):
        print(f"Starting Load Test on {BASE_URL}...")
        
        try:
            for users, duration in STAGES:
                self.run_stage(users, duration)
                time.sleep(2) # Cooldown
        finally:
            # Always generate reports, even if interrupted
            print("\nGenerating Reports...")
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
                    if res:
                        latencies.append(res)
                        success += 1
                    else:
                        errors += 1

        # Calculate Metrics
        metrics = {
            "success_count": success,
            "error_count": errors,
            "total": success + errors,
            "rps": 0,
            "avg": 0,
            "p50": 0,
            "p95": 0,
            "p99": 0,
            "error_rate": 0
        }

        if latencies:
            metrics["rps"] = success / duration
            metrics["avg"] = statistics.mean(latencies)
            metrics["p50"] = statistics.median(latencies)
            metrics["p95"] = statistics.quantiles(latencies, n=20)[18] if len(latencies) > 20 else metrics["avg"]
            metrics["p99"] = statistics.quantiles(latencies, n=100)[98] if len(latencies) > 100 else metrics["avg"]
        
        if metrics["total"] > 0:
            metrics["error_rate"] = (errors / metrics["total"]) * 100

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
        print("❌ Server not found at localhost:8080")
        sys.exit(1)
    
    LoadTester().run()