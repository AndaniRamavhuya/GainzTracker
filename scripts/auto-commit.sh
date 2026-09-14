#!/usr/bin/env bash
set -u

# Commits stable working-tree changes locally. Push separately with AUTO_PUSH=1.
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
INTERVAL_SECONDS="${AUTO_COMMIT_INTERVAL_SECONDS:-30}"
cd "$REPO_ROOT" || exit 1

while true; do
  if [[ -n "$(git status --porcelain)" ]]; then
    before="$(git status --porcelain)"
    sleep 5
    after="$(git status --porcelain)"
    if [[ "$before" == "$after" ]] && git diff --check; then
      git add -A
      if ! git diff --cached --quiet; then
        git commit -m "chore(auto): save workspace changes ($(date -u '+%Y-%m-%dT%H:%M:%SZ'))" || true
        if [[ "${AUTO_PUSH:-0}" == "1" ]]; then git push origin HEAD || true; fi
      fi
    fi
  fi
  sleep "$INTERVAL_SECONDS"
done
