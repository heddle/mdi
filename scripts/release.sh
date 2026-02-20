#!/bin/bash

set -e

# -----------------------------
# Configuration
# -----------------------------
REQUIRED_BRANCH="develop"

# -----------------------------
# Usage
# -----------------------------
if [ -z "$1" ]; then
  echo "Usage: ./scripts/release.sh X.Y.Z"
  exit 1
fi

RELEASE_VERSION=$1
NEXT_SNAPSHOT=$(echo $RELEASE_VERSION | awk -F. '{print $1"."$2"."$3+1}')-SNAPSHOT

# -----------------------------
# Safety Checks
# -----------------------------

# Check branch
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$CURRENT_BRANCH" != "$REQUIRED_BRANCH" ]; then
  echo "ERROR: Must be on branch '$REQUIRED_BRANCH' (currently on '$CURRENT_BRANCH')"
  exit 1
fi

# Check clean working tree
if ! git diff-index --quiet HEAD --; then
  echo "ERROR: Working tree is not clean. Commit or stash changes first."
  exit 1
fi

# Confirm
echo "====================================="
echo "Releasing version: $RELEASE_VERSION"
echo "Next snapshot:     $NEXT_SNAPSHOT"
echo "Branch:            $CURRENT_BRANCH"
echo "====================================="
read -p "Continue? (y/n) " confirm
if [ "$confirm" != "y" ]; then
  echo "Aborted."
  exit 1
fi

# -----------------------------
# Release Process
# -----------------------------

echo
echo "Setting release version..."
mvn versions:set -DnewVersion=$RELEASE_VERSION -DgenerateBackupPoms=false

echo "Committing release version..."
git commit -am "Release $RELEASE_VERSION"

echo "Tagging release..."
git tag -a v$RELEASE_VERSION -m "$RELEASE_VERSION"

echo "Building and deploying to Sonatype..."
mvn clean deploy

echo "Bumping to next snapshot version..."
mvn versions:set -DnewVersion=$NEXT_SNAPSHOT -DgenerateBackupPoms=false

echo "Committing next snapshot version..."
git commit -am "Start $NEXT_SNAPSHOT"

echo "Pushing commits..."
git push

echo "Pushing tags..."
git push --tags

echo
echo "Release process complete."
echo "Now log into Sonatype and Close/Release the staging repository."
echo
