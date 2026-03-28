#!/bin/sh
set -eu

RUNNER_DIR="${RUNNER_DIR:-/app/runner-images}"

if [ ! -d "$RUNNER_DIR" ]; then
  echo "Runner image directory not found: $RUNNER_DIR"
  exit 0
fi

found_any=0
for image_tar in "$RUNNER_DIR"/*.tar; do
  if [ ! -f "$image_tar" ]; then
    continue
  fi
  found_any=1
  echo "Loading Docker image archive: $(basename "$image_tar")"
  docker load -i "$image_tar"
done

if [ "$found_any" -eq 0 ]; then
  echo "No runner image archives found in $RUNNER_DIR, skipping preload."
fi