# Colors and emojis
GREEN := $(shell tput setaf 2)
YELLOW := $(shell tput setaf 3)
RED := $(shell tput setaf 1)
BLUE := $(shell tput setaf 4)
BOLD := $(shell tput bold)
RESET := $(shell tput sgr0)

ROCKET := 🚀
CHECK := ✅
WARNING := ⚠️
ERROR := ❌
GEAR := ⚙️
CLOCK := 🕒
KAFKA := 📬
DB := 🗄️
CLEAN := 🧹
PYTHON := 🐍
TOPIC := 📝

# Python environment
VENV_DIR := .venv
VENV_BIN := $(VENV_DIR)/bin
PYTHON := $(VENV_BIN)/python

# Check if we're in a virtual environment
ifeq ($(VIRTUAL_ENV),)
    PYTHON_CMD := $(PYTHON)
    PIP_CMD := uv pip
else
    PYTHON_CMD := python
    PIP_CMD := uv pip
endif

.PHONY: help install start stop restart status clean validate logs venv deps cli-install urls create-topics run-entry-api start-dashboard stop-dashboard simulate

help: ## Show this help message
	@echo '$(BOLD)$(BLUE)Available commands:$(RESET)'
	@echo ''
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(BLUE)%-15s$(RESET) %s\n", $$1, $$2}'

install: system-deps venv deps ## Install all dependencies

system-deps: ## Install system dependencies
	@echo "$(BOLD)$(BLUE)$(GEAR) Installing system dependencies...$(RESET)"
	@if ! command -v brew >/dev/null; then echo "$(ERROR) Homebrew not found. Please install it first."; exit 1; fi
	@brew install kcat jq uv docker docker-compose || true
	@echo "$(CHECK) System dependencies installed!"

venv: ## Create Python virtual environment using UV
	@echo "$(BOLD)$(BLUE)$(PYTHON) Creating virtual environment...$(RESET)"
	@uv venv $(VENV_DIR)
	@echo "$(CHECK) Virtual environment created!"

deps: venv ## Install Python dependencies using UV
	@echo "$(BOLD)$(BLUE)$(PYTHON) Installing Python dependencies...$(RESET)"
	@$(PIP_CMD) install -e parkflow_cli
	@$(PIP_CMD) install -e parkflow-dashboard[test]
	@echo "$(CHECK) Dependencies installed!"

start: ## Start all services
	@echo "$(BOLD)$(BLUE)$(ROCKET) Starting ParkFlow services...$(RESET)"
	@mkdir -p data logs
	@cd parkflow-entry-exit && ../gradlew buildImage && docker load -i build/jib-image.tar
	@docker compose pull kafka schema-registry duckdb
	@docker compose up -d
	@echo "$(CLOCK) Waiting for services to be healthy..."
	@until docker compose ps | grep -q "healthy" && ! docker compose ps | grep -q "starting"; do sleep 1; done
	@echo "$(CHECK) All services are up and running!"
	@make status
	@make create-topics
	@make send-one-event
	@$(MAKE) start-dashboard

start-dashboard: ## Start the ParkFlow dashboard
	@echo "$(BOLD)$(BLUE)$(ROCKET) Starting ParkFlow dashboard...$(RESET)"
	@echo "$(CLOCK) Checking dependencies..."
	@curl -s http://localhost:3000/health > /dev/null || (echo "$(ERROR) DuckDB is not running" && exit 1)
	@curl -s http://localhost:8081 > /dev/null || (echo "$(ERROR) Schema Registry is not running" && exit 1)
	@echo "$(CHECK) Dependencies are ready"
	@echo "$(BOLD)$(BLUE)$(ROCKET) Starting Kafka to DuckDB connector...$(RESET)"
	@KAFKA_BOOTSTRAP_SERVERS=localhost:29092 \
	 SCHEMA_REGISTRY_URL=http://localhost:8081 \
	 DUCKDB_API_URL=http://localhost:3000 \
	 $(PYTHON_CMD) -m parkflow_dashboard.kafka_duckdb_connector > logs/connector.log 2>&1 & echo $$! > logs/connector.pid
	@echo "$(CHECK) Connector started (PID: $$(cat logs/connector.pid))"
	@echo "$(BOLD)$(BLUE)$(ROCKET) Starting Dashboard UI...$(RESET)"
	@KAFKA_BOOTSTRAP_SERVERS=localhost:29092 \
	 SCHEMA_REGISTRY_URL=http://localhost:8081 \
	 DUCKDB_API_URL=http://localhost:3000 \
	 $(PYTHON_CMD) -m parkflow_dashboard.app > logs/dashboard.log 2>&1 & echo $$! > logs/dashboard.pid
	@echo "$(CHECK) Dashboard UI started (PID: $$(cat logs/dashboard.pid))"
	@echo "$(CHECK) Dashboard is available at http://localhost:8050"

stop: ## Stop all services
	@echo "$(BOLD)$(RED)$(CLEAN) Stopping ParkFlow services...$(RESET)"
	@if [ -f logs/connector.pid ]; then kill $$(cat logs/connector.pid) 2>/dev/null || true; rm logs/connector.pid; fi
	@if [ -f logs/dashboard.pid ]; then kill $$(cat logs/dashboard.pid) 2>/dev/null || true; rm logs/dashboard.pid; fi
	@docker compose down
	@echo "$(CHECK) Services stopped"

stop-dashboard: ## Stop only the dashboard components
	@echo "$(BOLD)$(RED)$(CLEAN) Stopping ParkFlow dashboard...$(RESET)"
	@if [ -f logs/connector.pid ]; then \
		echo "Stopping connector (PID: $$(cat logs/connector.pid))..."; \
		kill $$(cat logs/connector.pid) 2>/dev/null || true; \
		rm logs/connector.pid; \
	fi
	@if [ -f logs/dashboard.pid ]; then \
		echo "Stopping dashboard UI (PID: $$(cat logs/dashboard.pid))..."; \
		kill $$(cat logs/dashboard.pid) 2>/dev/null || true; \
		rm logs/dashboard.pid; \
	fi
	@echo "$(CHECK) Dashboard components stopped"

restart: stop start ## Restart all services

status: ## Show services status
	@echo "$(BOLD)$(BLUE)Services Status:$(RESET)"
	@echo "$(KAFKA) Kafka: $$(docker compose ps kafka | grep -q "Up" && echo "$(GREEN)Running$(RESET)" || echo "$(RED)Stopped$(RESET)")"
	@echo "$(KAFKA) Schema Registry: $$(docker compose ps schema-registry | grep -q "Up" && echo "$(GREEN)Running$(RESET)" || echo "$(RED)Stopped$(RESET)")"
	@echo "$(DB) DuckDB: $$(docker compose ps duckdb | grep -q "Up" && echo "$(GREEN)Running$(RESET)" || echo "$(RED)Stopped$(RESET)")"

clean: ## Clean up all containers, volumes, and virtual environment
	@echo "$(BOLD)$(RED)$(CLEAN) Cleaning up ParkFlow...$(RESET)"
	@docker compose down -v || true
	@rm -rf $(VENV_DIR) data
	@echo "$(CHECK) Cleanup complete"

validate: ## Validate services connectivity
	@echo "$(BOLD)$(BLUE)$(GEAR) Validating services...$(RESET)"
	@echo "Testing Kafka connectivity..."
	@kcat -b localhost:9092 -L >/dev/null 2>&1 && echo "$(CHECK) Kafka is accessible" || echo "$(ERROR) Kafka is not accessible"
	@echo "Testing Schema Registry..."
	@curl -s http://localhost:8081/subjects >/dev/null 2>&1 && echo "$(CHECK) Schema Registry is accessible" || echo "$(ERROR) Schema Registry is not accessible"
	@echo "Testing DuckDB API..."
	@curl -s http://localhost:3000/health >/dev/null 2>&1 && echo "$(CHECK) DuckDB API is accessible" || echo "$(ERROR) DuckDB API is not accessible"

logs: ## View service logs
	@echo "$(BOLD)$(BLUE)Logs:$(RESET)"
	@echo "$(BOLD)$(BLUE)Connector logs:$(RESET)"
	@tail -f logs/connector.log &
	@echo "$(BOLD)$(BLUE)Dashboard logs:$(RESET)"
	@tail -f logs/dashboard.log

create-topics: ## Create required Kafka topics
	@echo "$(BOLD)$(BLUE)$(TOPIC) Creating Kafka topics...$(RESET)"
	@docker compose exec -T kafka kafka-topics.sh --create --if-not-exists \
		--bootstrap-server localhost:9092 \
		--topic parking.entry.events \
		--partitions 1 \
		--replication-factor 1
	@echo "$(CHECK) Topics created!"

run-entry-api: ## Run the Entry/Exit API service
	@echo "$(BOLD)$(BLUE)$(ROCKET) Starting Entry/Exit API service...$(RESET)"
	@cd parkflow-entry-exit && ../gradlew run

send-one-event: ## Send one event to the Kafka topic
	@echo "$(BOLD)$(BLUE)$(ROCKET) Sending one event to Kafka...$(RESET)"
	@curl -s -X POST http://localhost:8085/api/v1/entry/event

simulate: ## Simulate vehicle entries (EVENTS=10 DELAY=1000)
	@echo "$(BOLD)$(BLUE)$(ROCKET) Starting simulation with $(EVENTS) events, $(DELAY)ms delay...$(RESET)"
	@curl -s -X POST http://localhost:8085/api/v1/entry/simulate \
		-H "Content-Type: application/json" \
		-d '{"numberOfEvents": $(EVENTS), "delayBetweenEventsMs": $(DELAY)}' \
		&& echo -e "\n$(CHECK) Simulation started!" \
		|| echo "$(ERROR) Failed to start simulation"
	@echo "$(CLOCK) Events will complete in ~$$(( $(EVENTS) * $(DELAY) / 1000 )) seconds"

# Simulation defaults
EVENTS ?= 10
DELAY ?= 1000

cli: ## Run the ParkFlow CLI (after installation)
	@echo "$(BOLD)$(BLUE)$(PYTHON) Running ParkFlow CLI...$(RESET)"
	@$(PYTHON_CMD) -m parkflow_cli

urls:
	@echo "\033[1;36m🔗 Service URLs:\033[0m"
	@echo "\033[1;33m📦 Kafka:\033[0m"
	@echo "  Internal: kafka:9092"
	@echo "  External: localhost:29092"
	@echo "\033[1;33m📋 Schema Registry:\033[0m"
	@echo "  http://localhost:8081"
	@echo "\033[1;33m🗄️  DuckDB API:\033[0m"
	@echo "  http://localhost:3000"
	@echo "\n\033[1;32m✨ Use these URLs to connect to the services\033[0m"
