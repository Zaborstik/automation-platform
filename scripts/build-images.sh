#!/usr/bin/env bash
# Build all four Docker images (api, knowledge, executor, agent) plus the
# playwright sidecar. Each image is tagged :local (and also :<git-sha> when
# the repo is a clean git checkout).
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./_common.sh
source "${SCRIPT_DIR}/_common.sh"

cd "${REPO_ROOT}"

GIT_SHA=""
if git rev-parse --short HEAD >/dev/null 2>&1; then
    GIT_SHA="$(git rev-parse --short HEAD)"
fi

build_image() {
    local image_name="$1"
    local dockerfile="$2"
    echo
    echo "[*] Building ${image_name}:local from ${dockerfile}"
    docker build \
        -f "${dockerfile}" \
        -t "${image_name}:local" \
        ${GIT_SHA:+-t "${image_name}:${GIT_SHA}"} \
        .
}

build_image platform-api        docker/server/Dockerfile.api
build_image platform-knowledge  docker/server/Dockerfile.knowledge
build_image platform-executor   docker/local/Dockerfile.executor
build_image platform-agent      docker/local/Dockerfile.agent
build_image platform-playwright docker/local/Dockerfile.playwright

echo
echo "[\u2713] All images built."
docker images | grep -E "^(platform-api|platform-knowledge|platform-executor|platform-agent|platform-playwright)\\b" || true
