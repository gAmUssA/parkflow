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

.PHONY: help install start stop restart status clean validate logs venv deps cli-install urls

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
	@$(PIP_CMD) install -e .
	@echo "$(CHECK) Dependencies installed!"

start: ## Start all services
	@echo "$(BOLD)$(BLUE)$(ROCKET) Starting ParkFlow services...$(RESET)"
	@mkdir -p data
	@docker-compose pull
	@docker-compose up -d
	@echo "$(CLOCK) Waiting for services to be healthy..."
	@until docker-compose ps | grep -q "healthy" && ! docker-compose ps | grep -q "starting"; do sleep 1; done
	@echo "$(CHECK) All services are up and running!"
	@make status

stop: ## Stop all services
	@echo "$(BOLD)$(RED)$(WARNING) Stopping ParkFlow services...$(RESET)"
	@docker-compose down
	@echo "$(CHECK) All services stopped"

restart: stop start ## Restart all services

status: ## Show services status
	@echo "$(BOLD)$(BLUE)Services Status:$(RESET)"
	@echo "$(KAFKA) Kafka: $$(docker-compose ps kafka | grep -q "Up" && echo "$(GREEN)Running$(RESET)" || echo "$(RED)Stopped$(RESET)")"
	@echo "$(KAFKA) Schema Registry: $$(docker-compose ps schema-registry | grep -q "Up" && echo "$(GREEN)Running$(RESET)" || echo "$(RED)Stopped$(RESET)")"
	@echo "$(DB) DuckDB: $$(docker-compose ps duckdb | grep -q "Up" && echo "$(GREEN)Running$(RESET)" || echo "$(RED)Stopped$(RESET)")"

clean: ## Clean up all containers, volumes, and virtual environment
	@echo "$(BOLD)$(RED)$(CLEAN) Cleaning up ParkFlow...$(RESET)"
	@docker-compose down -v || true
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

logs: ## Show logs from all services
	@echo "$(BOLD)$(BLUE)$(GEAR) Showing logs...$(RESET)"
	@docker-compose logs --tail=100 -f

cli: deps ## Run the ParkFlow CLI (after installation)
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
