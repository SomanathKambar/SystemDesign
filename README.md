# System Design Mono-repo

A modular repository for scalable system design solutions, emphasizing clean implementation, rigorous documentation (HLD/LLD), and failure mode analysis.

## 🚀 Live Demo

### **[Rate Limiter Interactive Lab](https://SomanathKambar.github.io/SystemDesign/infra/rate_limiter/)**
Explore and compare different rate-limiting algorithms (Token Bucket, Leaky Bucket, Sliding Window) in a real-time, deterministic simulation.

---

## 🏗️ Project Modules

### 1. Infrastructure: Rate Limiter Lab & Library
A pluggable, algorithm-agnostic library for request throttling in distributed systems, accompanied by a visual simulation lab.
- **Location**: `infra/rate_limiter/`
- **Interactive Lab**: [Open Lab](https://SomanathKambar.github.io/SystemDesign/infra/rate_limiter/)
- **Features**:
  - **Visual Simulation**: Deterministic replay of traffic scenarios (Burst, High Load, Boundary Stress).
  - **Algorithm Comparison**: Side-by-side analysis of Token Bucket, Leaky Bucket, and Sliding Window.
  - **Modular Architecture**: Separate `core` interfaces from implementation `strategies`.
  - **Persistence Agnostic**: Support for `InMemory` (Single Node) and `Redis` (Distributed).
- **Tech Stack**: Kotlin, React, Vite, Tailwind CSS, Redis, Spring Boot.

### 2. URL Shortener (Single Node)
A high-performance URL shortening service designed for single-node efficiency.
- **Location**: `00_fundamentals/single_node_system/url_shortener_single_node/`
- **Features**:
  - Base-62 encoding for URL compression.
  - RESTful API contracts.
  - Comprehensive LLD for caching and DB schema.
- **Tech Stack**: Kotlin, Spring Boot, SQLite/H2, Python (Testing).

---

## 📚 Design Standards

Every component in this repo adheres to a strict documentation lifecycle:

1.  **Architecture (HLD)**: System boundaries, high-level components, and data flow.
2.  **Low-Level Design (LLD)**: Specific algorithm logic, class diagrams, and complexity analysis.
3.  **Failure Modes**: Analysis of what happens when dependencies (Redis/DB) fail.
4.  **Trade-offs**: Detailed comparison of different approaches (e.g., memory vs. accuracy).

---

## 🛠️ Technology Stack

| Layer | Technology |
| :--- | :--- |
| **Language** | Kotlin 1.9+, TypeScript |
| **Frontend** | React 19, Vite, Tailwind CSS |
| **Build System** | Gradle (Multi-module, KTS) |
| **Infrastructure** | Redis, GitHub Pages (Hosting) |
| **CI/CD** | GitHub Actions |

---

## 📂 Repository Structure

```text
.
├── 00_fundamentals/           # Base system design concepts
├── infra/                     # Shared infrastructure libraries
│   └── rate_limiter/          # Rate Limiter Lab & Library
│       ├── core/              # API and Models
│       ├── strategies/        # Algorithm implementations
│       ├── frontend-lab/      # React-based Visual Lab
│       └── runner/            # Offline simulation generator
├── _template/                 # Standard templates for design docs
├── docs/                      # Global documentation
└── LICENSE                    # Apache 2.0
```

## 📄 License
This project is licensed under the **Apache License 2.0**.
