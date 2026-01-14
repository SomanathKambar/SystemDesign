# System Design Mono-repo

A modular repository for scalable system design solutions, emphasizing clean implementation, rigorous documentation (HLD/LLD), and failure mode analysis.

## 🚀 Overview

This project is built to demonstrate production-grade system design components. Each solution starts with requirements and architecture before moving to implementation.

---

## 🏗️ Project Modules

### 1. URL Shortener (Single Node)
A high-performance URL shortening service designed for single-node efficiency.
- **Location**: `00_fundamentals/single_node_system/url_shortener_single_node/`
- **Features**:
  - Base-62 encoding for URL compression.
  - RESTful API contracts.
  - Comprehensive LLD for caching and DB schema.
  - Stress testing suite (Python).
- **Tech Stack**: Kotlin, Spring Boot, Gradle, SQLite/H2 (Embedded), Python (Testing).

### 2. Infrastructure: Rate Limiter Library
A pluggable, algorithm-agnostic library for request throttling in distributed systems.
- **Location**: `infra/rate_limiter/`
- **Features**:
  - **Modular Architecture**: Separate `core` interfaces from implementation `strategies`.
  - **Fixed Window Strategy**: Memory-efficient counting per time bucket.
  - **Sliding Window Log Strategy**: 100% accurate throttling using timestamp logs to prevent boundary bursts.
  - **Persistence Agnostic**: Support for `InMemory` (Single Node) and extensible for `Redis` (Distributed).
- **Tech Stack**: Kotlin, Multi-module Gradle, JUnit 5, MockK.

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
| **Language** | Kotlin 1.9+ |
| **Build System** | Gradle (Multi-module, KTS) |
| **Testing** | JUnit 5, MockK, Python (Stress Testing) |
| **Documentation** | Markdown, Mermaid.js |
| **CI/CD** | GitHub Actions (Planned) |

---

## 📂 Repository Structure

```text
.
├── 00_fundamentals/           # Base system design concepts
│   └── single_node_system/    # Single-node implementation patterns
├── infra/                     # Shared infrastructure libraries
│   └── rate_limiter/          # Multi-module Rate Limiter project
│       ├── core/              # API and Models
│       └── strategies/        # Algorithm implementations (Fixed, Sliding)
├── _template/                 # Standard templates for design docs
├── docs/                      # Global documentation
└── LICENSE                    # Apache 2.0
```

## 🛠️ Getting Started

### Build the Rate Limiter
```bash
cd infra/rate_limiter
./gradlew build
```

### Run Rate Limiter Tests
```bash
./gradlew test
```

## 📄 License
This project is licensed under the **Apache License 2.0**. See the [LICENSE](LICENSE) file for details.