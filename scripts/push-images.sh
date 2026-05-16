#!/usr/bin/env bash
# Push the server-side images (platform-api, platform-knowledge) to a
# remote registry. Local-side images (executor, agent, playwright) can be
# pushed too by passing --include-local.
#
# Requires:
#   REGISTRY  e.g. ghcr.io/your-org   (no trailing slash)
#   TAG       (optional, defaults to current git short SHA or "local")
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

if [[ -z "${REGISTRY:-}" ]]; then
    echo "REGISTRY env var must be set (e.g. REGISTRY=ghcr.io/your-org)." >&2
    exit 1
fi

INCLUDE_LOCAL=false
for arg in "$@"; do
    case "$arg" in
        --include-local) INCLUDE_LOCAL=true ;;
        *) echo "Unknown arg: $arg" >&2; exit 1 ;;
    esac
done

if [[ -z "${TAG:-}" ]]; then
    if git rev-parse --short HEAD >/dev/null 2>&1; then
        TAG="$(git rev-parse --short HEAD)"
    else
        TAG="local"
    fi
fi

push_one() {
    local image="$1"
    local remote="${REGISTRY}/${image}:${TAG}"
    echo "[*] Tagging ${image}:local -> ${remote}"
    docker tag "${image}:local" "${remote}"
    echo "[*] Pushing ${remote}"
    docker push "${remote}"
}

push_one platform-api
push_one platform-knowledge

if $INCLUDE_LOCAL; then
    push_one platform-executor
    push_one platform-agent
    push_one platform-playwright
fi

echo "[\u2713] Done. Tag used: ${TAG}"
