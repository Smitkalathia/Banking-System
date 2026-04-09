#!/bin/bash
set -e

if [ "$#" -ne 5 ]; then
    echo "Usage: $0 <sessions_dir> <frontend_current_in> <day_output_dir> <day_label> <repo_root>"
    exit 1
fi

SESSIONS_DIR="$1"
FRONTEND_CURRENT_IN="$2"
DAY_OUTPUT_DIR="$3"
DAY_LABEL="$4"
REPO_ROOT="$5"

mkdir -p "$DAY_OUTPUT_DIR"
mkdir -p "$DAY_OUTPUT_DIR/session_outputs"

MERGED_FILE="$DAY_OUTPUT_DIR/${DAY_LABEL}_merged.atf"
BACKEND_CURRENT_IN="$DAY_OUTPUT_DIR/${DAY_LABEL}_backend_input.txt"
BACKEND_CURRENT_OUT="$DAY_OUTPUT_DIR/${DAY_LABEL}_backend_output.txt"
FRONTEND_CURRENT_OUT="$DAY_OUTPUT_DIR/${DAY_LABEL}_currentaccounts_frontend.txt"
LOG_FILE="$DAY_OUTPUT_DIR/${DAY_LABEL}.log"

: > "$MERGED_FILE"
: > "$LOG_FILE"

echo "=== DAILY RUN $DAY_LABEL ===" | tee -a "$LOG_FILE"

# ensure session files exist
shopt -s nullglob
SESSION_FILES=("$SESSIONS_DIR"/*.txt)

if [ ${#SESSION_FILES[@]} -eq 0 ]; then
    echo "No session files found in $SESSIONS_DIR" | tee -a "$LOG_FILE"
    exit 1
fi

for SESSION_FILE in "${SESSION_FILES[@]}"; do
    BASE_NAME=$(basename "$SESSION_FILE" .txt)
    SESSION_ATF="$DAY_OUTPUT_DIR/session_outputs/${BASE_NAME}.atf"
    SESSION_OUT="$DAY_OUTPUT_DIR/session_outputs/${BASE_NAME}.out"

    echo "Running session: $SESSION_FILE" | tee -a "$LOG_FILE"

    # convert paths for Windows Java
    WIN_REPO_ROOT=$(wslpath -w "$REPO_ROOT")
    WIN_CURRENT=$(wslpath -w "$FRONTEND_CURRENT_IN")
    WIN_ATF=$(wslpath -w "$SESSION_ATF")

    # run front end using Windows Java
    cat "$SESSION_FILE" | cmd.exe /c java -cp "$WIN_REPO_ROOT\\Phase2\\bin" AtmApp "$WIN_CURRENT" "$WIN_ATF" > "$SESSION_OUT"

    if [ -f "$SESSION_ATF" ]; then
        cat "$SESSION_ATF" >> "$MERGED_FILE"
    else
        echo "WARNING: Missing ATF for $SESSION_FILE" | tee -a "$LOG_FILE"
    fi
done

# convert frontend -> backend format
python3 "$REPO_ROOT/Phase6/scripts/frontend_to_backend.py" "$FRONTEND_CURRENT_IN" "$BACKEND_CURRENT_IN"

# run backend
python3 "$REPO_ROOT/Phase5/backend/main.py" "$BACKEND_CURRENT_IN" "$MERGED_FILE" "$BACKEND_CURRENT_OUT" >> "$LOG_FILE"

# convert backend -> frontend format
python3 "$REPO_ROOT/Phase6/scripts/backend_to_frontend.py" "$BACKEND_CURRENT_OUT" "$FRONTEND_CURRENT_OUT"

echo "Merged ATF: $MERGED_FILE" | tee -a "$LOG_FILE"
echo "Next day current accounts: $FRONTEND_CURRENT_OUT" | tee -a "$LOG_FILE"
echo "=== DAILY COMPLETE ===" | tee -a "$LOG_FILE"