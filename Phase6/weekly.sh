#!/bin/bash
set -e

REPO_ROOT="$(pwd)"
PHASE6_DIR="$REPO_ROOT/Phase6"
OUTPUT_DIR="$PHASE6_DIR/output"

mkdir -p "$OUTPUT_DIR"

CURRENT_INPUT="$PHASE6_DIR/data/frontend_currentaccounts.txt"

# check initial file exists
if [ ! -f "$CURRENT_INPUT" ]; then
    echo "ERROR: Initial current accounts file not found: $CURRENT_INPUT"
    exit 1
fi

echo "=== STARTING WEEKLY RUN ==="
echo "Initial accounts file: $CURRENT_INPUT"
echo ""

for DAY in 1 2 3 4 5 6 7; do
    DAY_LABEL="day${DAY}"
    DAY_SESSIONS="$PHASE6_DIR/sessions/$DAY_LABEL"
    DAY_OUTPUT="$OUTPUT_DIR/$DAY_LABEL"

    echo "=========================================="
    echo "RUNNING $DAY_LABEL"
    echo "Sessions folder: $DAY_SESSIONS"
    echo "Input accounts: $CURRENT_INPUT"
    echo "=========================================="

    # check sessions folder exists
    if [ ! -d "$DAY_SESSIONS" ]; then
        echo "ERROR: Missing sessions directory: $DAY_SESSIONS"
        exit 1
    fi

    bash "$PHASE6_DIR/daily.sh" \
        "$DAY_SESSIONS" \
        "$CURRENT_INPUT" \
        "$DAY_OUTPUT" \
        "$DAY_LABEL" \
        "$REPO_ROOT"

    NEXT_INPUT="$DAY_OUTPUT/${DAY_LABEL}_currentaccounts_frontend.txt"

    # verify output was created
    if [ ! -f "$NEXT_INPUT" ]; then
        echo "ERROR: Expected output file not found: $NEXT_INPUT"
        exit 1
    fi

    CURRENT_INPUT="$NEXT_INPUT"

    echo "Completed $DAY_LABEL"
    echo "Next day input: $CURRENT_INPUT"
    echo ""
done

echo "=========================================="
echo "WEEKLY RUN COMPLETE"
echo "Final current accounts file:"
echo "$CURRENT_INPUT"
echo "=========================================="