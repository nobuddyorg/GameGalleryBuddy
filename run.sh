#!/bin/bash

if ! command -v docker >/dev/null 2>&1; then
    echo "Docker is not installed. Exiting."
    exit 1
fi

if ! docker info >/dev/null 2>&1; then
    echo "Docker is installed but not running. Exiting."
    exit 1
fi

docker build -t bggwallpaper:latest . && docker run -p 8080:8080 --rm bggwallpaper:latest
