#!/usr/bin/env bash
# Run the full Maven reactor test suite (./mvnw / mvn / IntelliJ IDEA bundle).
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

cd "${REPO_ROOT}"

MVN_CMD="$(detect_mvn)" || exit 1

echo "[*] Running tests with: ${MVN_CMD}"
"${MVN_CMD}" -B test "$@"
