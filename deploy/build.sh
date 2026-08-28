#!/usr/bin/env bash
# Builds the app's Docker image and saves it as a tarball Ansible can copy
# to the VM and `docker load` there (the VM has no registry access set up,
# so image transfer goes over the same SSH connection Ansible already uses).
set -euo pipefail

cd "$(dirname "$0")/.."

echo "==> Building Docker image"
docker build -t gamegallerybuddy:latest .

echo "==> Saving image to deploy/gamegallerybuddy-image.tar"
docker save -o gamegallerybuddy-image.tar gamegallerybuddy:latest

echo "==> Done: gamegallerybuddy-image.tar"
