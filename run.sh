#!/bin/bash

[[ -f ./.env ]] || { echo ".env file missing"; exit 1; }
set -a
. ./.env
set +a
[[ -n "${BGG_API_TOKEN:-}" ]] || { echo "BGG_API_TOKEN missing or empty"; exit 1; }

echo "Select an option to run the application:"
echo "1. Run with Docker"
echo "2. Run directly with Gradle (no installed tools required)"
read -p "Enter the number (1 or 2): " choice

case $choice in
    1)
        if ! command -v docker >/dev/null 2>&1; then
            echo "Docker is not installed. Exiting."
            exit 1
        fi

        if ! docker info >/dev/null 2>&1; then
            echo "Docker is installed but not running. Exiting."
            exit 1
        fi

        docker build -t bggwallpaper:latest . && docker run -p 8080:8080 --rm bggwallpaper:latest
        ;;
    2)
        if [ ! -f "./gradlew" ]; then
            echo "gradlew not found. Exiting."
            exit 1
        fi

        run_gradle() {
            if [[ "$OSTYPE" == "linux-gnu"* || "$OSTYPE" == "darwin"* ]]; then
                # Unix-based (Linux/macOS)
                ./gradlew bootRun
            elif [[ "$OSTYPE" == "cygwin" || "$OSTYPE" == "msys" || "$OSTYPE" == "mingw"* ]]; then
                # Windows-based (Cygwin, Git Bash, MinGW)
                ./gradlew.bat bootRun
            else
                echo "Unsupported OS: $OSTYPE"
                exit 1
            fi
        }

        run_gradle
        ;;
    *)
        echo "Invalid option. Exiting."
        exit 1
        ;;
esac
