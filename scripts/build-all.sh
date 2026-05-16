#!/usr/bin/env bash
# Build every Maven module without running tests. Useful as a smoke check
# before producing Docker images.
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

cd "${REPO_ROOT}"

if [[ -x "${REPO_ROOT}/mvnw" ]]; then
    MVN_CMD="${REPO_ROOT}/mvnw"
elif command -v mvn >/dev/null 2>&1; then
    MVN_CMD="mvn"
else
    echo "Neither ./mvnw nor mvn found on PATH; install Maven first." >&2
    exit 1
fi

echo "[*] Building all modules with: ${MVN_CMD}"
"${MVN_CMD}" -B -DskipTests clean install "$@"
