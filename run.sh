#!/bin/bash

[[ -f ./.env ]] || { echo ".env file missing"; exit 1; }
set -a
. ./.env
set +a
[[ -n "${BGG_API_TOKEN:-}" ]] || { echo "BGG_API_TOKEN missing or empty"; exit 1; }

echo "Select an option to run the application:"
echo "1. Run with Docker"
echo "2. Run directly with Gradle (no installed tools required)"
echo "3. Only run tests."
read -p "Enter the number (1, 2 or 3): " choice

gradle_cmd() {
  if [[ "$OSTYPE" == "cygwin" || "$OSTYPE" == "msys" || "$OSTYPE" == "mingw"* ]]; then
    [[ -f ./gradlew.bat ]] || { echo "gradlew.bat not found. Exiting."; exit 1; }
    echo "./gradlew.bat"
  else
    [[ -f ./gradlew ]] || { echo "gradlew not found. Exiting."; exit 1; }
    echo "./gradlew"
  fi
}

run_gradle() {
  cmd="$(gradle_cmd)"
  $cmd "$@"
}

case $choice in
  1)
    command -v docker >/dev/null 2>&1 || { echo "Docker is not installed. Exiting."; exit 1; }
    docker info >/dev/null 2>&1 || { echo "Docker is installed but not running. Exiting."; exit 1; }
    docker build -t bggwallpaper:latest . && docker run -p 8080:8080 --rm bggwallpaper:latest
    ;;
  2)
    run_gradle bootRun
    ;;
  3)
    run_gradle test
    ;;
  *)
    echo "Invalid option. Exiting."
    exit 1
    ;;
esac
