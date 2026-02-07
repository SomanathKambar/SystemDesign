#!/bin/bash

# Exit on error
set -e

echo "🚀 Starting deployment to GitHub Pages..."

# Navigate to the frontend-lab directory
cd "$(dirname "$0")"

# 1. Build the project
echo "📦 Installing dependencies and building project..."
npm install
npm run build

# 2. Prepare deployment directory
echo "📂 Preparing deployment..."
# Create a temporary directory for deployment
DEPLOY_DIR=$(mktemp -d)
echo "Using temp dir: $DEPLOY_DIR"

# Check if build succeeded
if [ ! -d "dist" ]; then
    echo "❌ Build failed - dist/ directory not found"
    exit 1
fi

# 3. Handle the mono-repo structure manually
echo "🔨 Setting up gh-pages branch structure..."
cd $DEPLOY_DIR
git init
git remote add origin https://github.com/SomanathKambar/SystemDesign.git

# Create the folder structure
mkdir -p infra/rate_limiter
cp -r "$(cd - > /dev/null && pwd)/dist/"* infra/rate_limiter/

# Create a redirect at the root so people don't see a 404 or ABOUT.md
cat <<EOF > index.html
<!DOCTYPE html>
<html>
  <head>
    <meta http-equiv="refresh" content="0; url='/SystemDesign/infra/rate_limiter/'" />
    <script type="text/javascript">
      window.location.href = "/SystemDesign/infra/rate_limiter/"
    </script>
    <title>Redirecting to Rate Limiter Lab</title>
  </head>
  <body>
    If you are not redirected, <a href="/SystemDesign/infra/rate_limiter/">click here</a>.
  </body>
</html>
EOF

# Add .nojekyll to prevent GitHub from ignoring files
touch .nojekyll

# 4. Commit and Push
echo "⬆️ Pushing to GitHub..."
git config user.name "GitHub Action" || true
git config user.email "action@github.com" || true

git add .
git commit -m "Deploy Rate Limiter Lab to infra/rate_limiter (with absolute base path)"
git push -f origin HEAD:gh-pages

echo "✅ SUCCESS!"
echo "Your lab will be live shortly at: https://SomanathKambar.github.io/SystemDesign/infra/rate_limiter/"
echo "The root URL will now redirect there: https://SomanathKambar.github.io/SystemDesign/"

# Cleanup
rm -rf $DEPLOY_DIR
