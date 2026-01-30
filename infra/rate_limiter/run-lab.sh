#!/bin/bash

# Script to run the Rate Limiter Lab Frontend
echo "🚀 Starting Rate Limiter Lab Frontend..."

cd frontend-lab

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    echo "📦 Installing dependencies..."
    npm install
fi

echo "✨ Starting Vite development server..."
npm run dev
