# GIT RULES — System Design Mono-Repo

This repository is a canonical architecture knowledgebase. All commits must follow these rules to maintain structural integrity and clarity.

---

## 1. Branching Policy

### Rules
*   **Feature Isolation:** All work must be done in feature branches.
*   **Main Stability:** `main` must remain stable and reviewable at all times.
*   **Cleanup:** Delete the feature branch immediately after a successful merge.

### Branch Naming
*   `feat/<system-name>-<version>`
*   `fix/<system-name>-<issue>`
*   `docs/<system-name>-<topic>`

---

## 2. Commit Message Standard
## All commits must follow the below format:  
## `<type>(<scope>): <short summary>`

## Type -> Meaning
### fea -> New system / design
### fix	-> Corrections
### docs -> Documentation
### refactor -> Structure cleanup
### infra ->  Repo structure
### test ->  Load / chaos / perf tests
## Example:
###  feat(url-shortener): add single-node system design

## 3. Folder Governance
###   Rule 1: Every system must live inside its designated phase folder.
###   Rule 2: Design docs must follow the standard repository template.
###   Rule 3: Code must live only under the /services/ directory.
###   Rule 4: Never mix conceptual design documentation and runnable code in the same folder.

## 4. Design Completion Checklist
###    Before merging any system, the following files must be present:
###   requirements.md
###   api_contracts.md
###   db_schema.md
###   capacity_estimation.md
###   architecture.md
###   failure_modes.md
###   scaling_strategy.md
###   tradeoffs.md

## 5. Quality Gate
###   Every design must strictly adhere to these quality standards:
###   Explicit Assumptions: List all technical and business assumptions.
###   Failure Analysis: Explicitly mention failure scenarios.
###   Growth Plan: Include a clear scaling strategy.
###   Critical Thinking: Include architectural tradeoffs.
###   No Placeholders: Partial designs are not allowed.

## 6. Future Code Rules
###   All runnable systems (services) must:
###   Contain a Dockerfile.
###   Provide docker-compose or k8s orchestration files.
###   Be capable of running independently.
###   Include a README.md with specific run instructions.

## 7. Repo Philosophy
###    This repo is a learning artifact, not a code dump. Every commit must improve architectural clarity.