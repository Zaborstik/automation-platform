# Convenience entry points for the most common dev/ops tasks.
#
# Run `make help` to see the available targets.

SHELL := /usr/bin/env bash
.SHELLFLAGS := -eu -o pipefail -c

REPO_ROOT := $(shell pwd)
SCRIPTS_DIR := $(REPO_ROOT)/scripts

.DEFAULT_GOAL := help

.PHONY: help
help: ## Show this help.
	@awk 'BEGIN {FS = ":.*##"; printf "Usage:\n  make <target>\n\nTargets:\n"} \
	      /^[a-zA-Z_-]+:.*?##/ { printf "  \033[36m%-22s\033[0m %s\n", $$1, $$2 }' $(MAKEFILE_LIST)

.PHONY: build
build: ## Build all Maven modules (skip tests).
	@$(SCRIPTS_DIR)/build-all.sh

.PHONY: test
test: ## Run all Maven tests.
	@cd $(REPO_ROOT) && ./mvnw -B test || mvn -B test

.PHONY: images
images: ## Build every Docker image (api, knowledge, executor, agent, playwright).
	@$(SCRIPTS_DIR)/build-images.sh

.PHONY: push
push: ## Push server-side images to $$REGISTRY (use REGISTRY=ghcr.io/your-org).
	@$(SCRIPTS_DIR)/push-images.sh

.PHONY: server-deploy
server-deploy: ## Bring the server stack up (postgres + api + knowledge).
	@$(SCRIPTS_DIR)/deploy-server.sh

.PHONY: server-pull
server-pull: ## Pull pre-built server images and start the stack.
	@$(SCRIPTS_DIR)/deploy-server.sh --pull

.PHONY: server-down
server-down: ## Stop the server stack.
	@bash -c 'source $(SCRIPTS_DIR)/_common.sh && \
	    if [[ -f "$$SERVER_ENV_FILE" ]]; then \
	        docker_compose --env-file "$$SERVER_ENV_FILE" -f "$$SERVER_COMPOSE_FILE" down; \
	    else \
	        docker_compose -f "$$SERVER_COMPOSE_FILE" down; \
	    fi'

.PHONY: server-logs
server-logs: ## Tail server-stack logs.
	@$(SCRIPTS_DIR)/logs-server.sh

.PHONY: local-up
local-up: ## Start the local stack (executor + agent + playwright).
	@$(SCRIPTS_DIR)/start-local.sh

.PHONY: local-down
local-down: ## Stop the local stack.
	@$(SCRIPTS_DIR)/stop-local.sh

.PHONY: local-logs
local-logs: ## Tail local-stack logs.
	@$(SCRIPTS_DIR)/logs-local.sh

.PHONY: smoke
smoke: ## Run the full distributed smoke test (server+local in one compose).
	@$(SCRIPTS_DIR)/smoke-test.sh

.PHONY: clean
clean: ## Maven clean.
	@cd $(REPO_ROOT) && (./mvnw -B clean 2>/dev/null || mvn -B clean)
