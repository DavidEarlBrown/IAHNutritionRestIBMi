#!/bin/sh
# Start the Java optimization API on IBM i PASE.
# Database access is ILE RPG at IAH_DATA_BASE_URL, not JDBC.

set -e

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-ibmi}"
export SERVER_PORT="${SERVER_PORT:-8080}"
export IAH_DATA_BASE_URL="${IAH_DATA_BASE_URL:-http://127.0.0.1:10010/iahdata}"

JAR="${1:-iah-nutrition-rest.jar}"
exec java -jar "$JAR"
