#!/usr/bin/env bash

set -u

interrupted=0
command_running=0
handle_interrupt() {
    if (( command_running )); then
        interrupted=1
        printf "\nInterrupted; returning to prompt.\n"
    else
        printf "\nExiting.\n"
        exit 130
    fi
}
trap handle_interrupt INT
trap 'exit 143' TERM

while true; do
    printf '\nPress Enter to build and launch, or Ctrl+C to exit. '
    if ! IFS= read -r _; then
        if (( interrupted )); then
            interrupted=0
            continue
        fi
        exit 0
    fi

    interrupted=0
    command_running=1
    ./gradlew installDebug && \
        adb shell am start -n com.sahilrajput.pomodoro/com.sahilrajput.pomodoro.MainActivity
    command_running=0

    if (( interrupted )); then
        interrupted=0
        continue
    fi

    printf '\nBuild command finished (exit status %s).\n' "$?"
done
