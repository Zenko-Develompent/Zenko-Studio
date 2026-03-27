#!/bin/sh
set -e

RUNNER_DIR=/app/runner-images

# Проверяем и загружаем образы, если они ещё не загружены
for image_tar in $RUNNER_DIR/*.tar; do
    image_name=$(basename "$image_tar" .tar)
    if ! docker image inspect "$image_name" > /dev/null 2>&1; then
        echo "Loading Docker image: $image_name"
        docker load -i "$image_tar"
    else
        echo "Image $image_name already exists, skipping..."
    fi
done