#!/usr/bin/env bash
set -e

# NexusOS Agent - Direct GitHub Push Script
# Usage:
#   ./push_to_github.sh <GITHUB_REPOSITORY_URL>
# Example:
#   ./push_to_github.sh https://github.com/iqbal1122qq00/NexusOSAgent.git
# With Personal Access Token (PAT):
#   ./push_to_github.sh https://<YOUR_GITHUB_TOKEN>@github.com/iqbal1122qq00/NexusOSAgent.git

REPO_URL="$1"

if [ -z "$REPO_URL" ]; then
    echo "=========================================================="
    echo "⚠️ অনুগ্রহ করে আপনার GitHub রিপোজিটরি লিংক দিন!"
    echo "ব্যবহার পদ্ধতি:"
    echo "  ./push_to_github.sh https://github.com/YOUR_USERNAME/NexusOSAgent.git"
    echo ""
    echo "অথবা GitHub Personal Access Token (PAT) সহ:"
    echo "  ./push_to_github.sh https://YOUR_TOKEN@github.com/YOUR_USERNAME/NexusOSAgent.git"
    echo "=========================================================="
    exit 1
fi

echo "🚀 GitHub রিমোট কনফিগার করা হচ্ছে..."
git remote remove origin 2>/dev/null || true
git remote add origin "$REPO_URL"
git branch -M main

echo "📤 গিটহাবে পুশ করা হচ্ছে..."
git push -u origin main --force

echo "✅ সফলভাবে GitHub এ কোড পুশ সম্পন্ন হয়েছে!"
