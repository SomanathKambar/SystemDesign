# Rate Limiter Infrastructure Playground

## Overview
This application is a **System Design Infrastructure Playground** designed to visualize, test, and compare various rate-limiting strategies in real-time.

It is built with a **Strategy-First Architecture**, allowing engineers to toggle between different algorithms to observe how they handle traffic bursts, window boundaries, and load distribution.

## Core Features
- **Algorithm Agnostic**: Support for multiple strategies (Fixed Window, Sliding Window) through a unified interface.
- **Dynamic Configuration**: Hot-swap limits and window sizes without restarting the server.
- **Visual Analytics**: Real-time progress bars and strategy-specific metrics (e.g., Estimated Counts for Sliding Window).
- **Extensibility**: Designed to easily accommodate future strategies like *Token Bucket* or *Leaky Bucket*.
