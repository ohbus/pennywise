SHELL := /bin/sh

GRADLE ?= ./gradlew
COMPOSE ?= docker compose
LOCAL_COMPOSE := infra/local/docker-compose.yml
ACCOUNTS_COMPOSE := infra/local/docker-compose.accounts.yml
EXPENSE_CORE_COMPOSE := infra/local/docker-compose.expense-core.yml
NOTIFICATIONS_COMPOSE := infra/local/docker-compose.notifications.yml
BFF_COMPOSE := infra/local/docker-compose.bff.yml
DEV_COMPOSE := infra/local/docker-compose.dev.yml
PROD_COMPOSE := infra/deploy/docker-compose.prod.yml
SERVICES := accounts expense-core notifications bff

.DEFAULT_GOAL := help
.PHONY: help doctor bootstrap validate contracts lint test test-unit test-integration coverage build package check ci ci-e2e acceptance acceptance-live workflow-validate e2e smoke docs-diagrams docs-diagrams-config compose-config deps-config deps-up deps-status deps-logs deps-down accounts-deps-config accounts-deps-up accounts-deps-status accounts-deps-logs accounts-deps-down expense-core-deps-config expense-core-deps-up expense-core-deps-status expense-core-deps-logs expense-core-deps-down notifications-deps-config notifications-deps-up notifications-deps-status notifications-deps-logs notifications-deps-down bff-deps-config bff-deps-up bff-deps-status bff-deps-logs bff-deps-down full-config full-up full-status full-logs full-down compose-dev-up compose-dev-down compose-dev-logs compose-up compose-down docker-build-all docker-build-% prod-config clean clean-gradle status

help: ## Show available commands
	@awk 'BEGIN {FS = ":.*##"; printf "Pennywise commands\n\n"} /^[a-zA-Z0-9_.-]+:.*##/ {printf "  %-24s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

doctor: ## Check required local tools and versions
	@command -v docker >/dev/null || (echo "Docker is required"; exit 1)
	@command -v python3 >/dev/null || (echo "Python 3 is required"; exit 1)
	@command -v java >/dev/null || (echo "Java 25+ is required"; exit 1)
	@docker compose version
	@java -version
	@$(GRADLE) --version

bootstrap: doctor ## Resolve the Gradle wrapper and verify the scaffold
	@$(GRADLE) help

validate: contracts compose-config ## Run dependency-light repository checks
	@$(GRADLE) test

contracts: ## Validate contract JSON, GraphQL declarations, and task links
	@python3 tools/contracts/validate.py

docs-diagrams-config: ## Validate the Mermaid renderer Compose file
	@$(COMPOSE) -f infra/docs/docker-compose.yml config --quiet

docs-diagrams: docs-diagrams-config ## Render Mermaid source diagrams to ignored SVG outputs
	@$(COMPOSE) -f infra/docs/docker-compose.yml run --build --rm mermaid

lint: contracts ## Run contracts, compatible Spotless formatting baseline, and Gradle checks
	@$(GRADLE) spotlessCheck check


test: test-unit test-integration ## Run the complete JVM test suite

test-unit: ## Run fast unit tests
	@$(GRADLE) test --tests '*Test'

test-integration: ## Run Spring context and future Testcontainers tests
	@$(GRADLE) test

coverage: ## Run tests and produce JaCoCo XML/HTML reports
	@$(GRADLE) test jacocoTestReport

build: ## Compile all Kotlin and Java sources
	@$(GRADLE) compileKotlin compileJava

package: ## Build executable jars for every application
	@$(GRADLE) bootJar
	@find app -path '*/build/libs/*.jar' -type f -print

check: validate coverage package ## Validate, test, report coverage, and package

ci: contracts compose-config ## Run the hosted CI verification stages locally with parallel Gradle workers
	@$(GRADLE) test check jacocoTestReport bootJar --parallel --no-daemon

ci-e2e: ci e2e ## Run local CI verification plus the contract/deployment E2E smoke suite

acceptance: ## Run the public-interface acceptance harness and write a JSON report
	@bash tests/acceptance/run.sh

acceptance-live: ## Run the acceptance test harness requiring live running services
	@python3 tests/acceptance/runner.py --require-services

workflow-validate: ## Parse all GitHub Actions workflow YAML files
	@ruby -e 'require "yaml"; Dir[".github/workflows/*.yml"].each { |file| YAML.load_file(file); puts "valid workflow: #{file}" }'

e2e: ## Run the contract and deployment E2E smoke checks
	@tests/e2e/contract-smoke.sh

smoke: validate e2e ## Run safe local smoke checks without starting containers

compose-config: deps-config accounts-deps-config expense-core-deps-config notifications-deps-config bff-deps-config full-config ## Validate every local Compose topology

deps-config: ## Validate dependency-only infra/local/docker-compose.yml
	@$(COMPOSE) -f $(LOCAL_COMPOSE) config --quiet

deps-up: ## Start PostgreSQL, RabbitMQ, and Mailpit from infra/local/docker-compose.yml
	@$(COMPOSE) -f $(LOCAL_COMPOSE) up -d --wait

deps-status: ## Show dependency-only services from infra/local/docker-compose.yml
	@$(COMPOSE) -f $(LOCAL_COMPOSE) ps

deps-logs: ## Follow dependency-only logs from infra/local/docker-compose.yml
	@$(COMPOSE) -f $(LOCAL_COMPOSE) logs -f

deps-down: ## Stop dependency-only services from infra/local/docker-compose.yml
	@$(COMPOSE) -f $(LOCAL_COMPOSE) down

accounts-deps-config: ## Validate native Accounts prerequisites in docker-compose.accounts.yml
	@$(COMPOSE) -f $(ACCOUNTS_COMPOSE) config --quiet

accounts-deps-up: ## Start native Accounts prerequisites from docker-compose.accounts.yml
	@$(COMPOSE) -f $(ACCOUNTS_COMPOSE) up -d --wait

accounts-deps-status: ## Show native Accounts prerequisites from docker-compose.accounts.yml
	@$(COMPOSE) -f $(ACCOUNTS_COMPOSE) ps

accounts-deps-logs: ## Follow native Accounts prerequisite logs from docker-compose.accounts.yml
	@$(COMPOSE) -f $(ACCOUNTS_COMPOSE) logs -f

accounts-deps-down: ## Stop native Accounts prerequisites from docker-compose.accounts.yml
	@$(COMPOSE) -f $(ACCOUNTS_COMPOSE) down

expense-core-deps-config: ## Validate native Expense Core prerequisites in docker-compose.expense-core.yml
	@$(COMPOSE) -f $(EXPENSE_CORE_COMPOSE) config --quiet

expense-core-deps-up: ## Start native Expense Core prerequisites from docker-compose.expense-core.yml
	@$(COMPOSE) -f $(EXPENSE_CORE_COMPOSE) up -d --wait

expense-core-deps-status: ## Show native Expense Core prerequisites from docker-compose.expense-core.yml
	@$(COMPOSE) -f $(EXPENSE_CORE_COMPOSE) ps

expense-core-deps-logs: ## Follow native Expense Core prerequisite logs from docker-compose.expense-core.yml
	@$(COMPOSE) -f $(EXPENSE_CORE_COMPOSE) logs -f

expense-core-deps-down: ## Stop native Expense Core prerequisites from docker-compose.expense-core.yml
	@$(COMPOSE) -f $(EXPENSE_CORE_COMPOSE) down

notifications-deps-config: ## Validate native Notifications prerequisites in docker-compose.notifications.yml
	@$(COMPOSE) -f $(NOTIFICATIONS_COMPOSE) config --quiet

notifications-deps-up: ## Start native Notifications prerequisites from docker-compose.notifications.yml
	@$(COMPOSE) -f $(NOTIFICATIONS_COMPOSE) up -d --wait

notifications-deps-status: ## Show native Notifications prerequisites from docker-compose.notifications.yml
	@$(COMPOSE) -f $(NOTIFICATIONS_COMPOSE) ps

notifications-deps-logs: ## Follow native Notifications prerequisite logs from docker-compose.notifications.yml
	@$(COMPOSE) -f $(NOTIFICATIONS_COMPOSE) logs -f

notifications-deps-down: ## Stop native Notifications prerequisites from docker-compose.notifications.yml
	@$(COMPOSE) -f $(NOTIFICATIONS_COMPOSE) down

bff-deps-config: ## Validate native BFF upstreams in docker-compose.bff.yml
	@$(COMPOSE) -f $(BFF_COMPOSE) config --quiet

bff-deps-up: ## Start Accounts and Expense Core upstreams for a native BFF
	@$(COMPOSE) -f $(BFF_COMPOSE) up -d --build --wait

bff-deps-status: ## Show native BFF upstreams from docker-compose.bff.yml
	@$(COMPOSE) -f $(BFF_COMPOSE) ps

bff-deps-logs: ## Follow native BFF upstream logs from docker-compose.bff.yml
	@$(COMPOSE) -f $(BFF_COMPOSE) logs -f

bff-deps-down: ## Stop native BFF upstreams from docker-compose.bff.yml
	@$(COMPOSE) -f $(BFF_COMPOSE) down

full-config: ## Validate the complete stack in infra/local/docker-compose.dev.yml
	@$(COMPOSE) -f $(DEV_COMPOSE) config --quiet

full-up: ## Build and run all applications and dependencies from docker-compose.dev.yml
	@$(COMPOSE) -f $(DEV_COMPOSE) up --build

full-status: ## Show all applications and dependencies from docker-compose.dev.yml
	@$(COMPOSE) -f $(DEV_COMPOSE) ps

full-logs: ## Follow all application and dependency logs from docker-compose.dev.yml
	@$(COMPOSE) -f $(DEV_COMPOSE) logs -f

full-down: ## Stop all applications and dependencies from docker-compose.dev.yml
	@$(COMPOSE) -f $(DEV_COMPOSE) down

compose-dev-up: full-up ## Alias for full-up

compose-dev-down: full-down ## Alias for full-down

compose-dev-logs: full-logs ## Alias for full-logs

compose-up: compose-dev-up ## Alias for the development stack
compose-down: compose-dev-down ## Alias for stopping the development stack

docker-build-all: $(addprefix docker-build-,$(SERVICES)) ## Build all production JVM images

docker-build-%: ## Build one production image, e.g. make docker-build-accounts
	@test -n "$*" || (echo "Provide a service name"; exit 2)
	@docker build --build-arg APP_PROJECT=$* -f infra/docker/Dockerfile.jvm -t pennywise-$*:local .

DOCKER_FAST_TARGETS = $(addprefix docker-fast-,$(SERVICES))

docker-fast-all: package $(DOCKER_FAST_TARGETS) ## Build all local runtime images quickly from host jars

$(DOCKER_FAST_TARGETS): docker-fast-%:
	@docker build --build-arg APP_PROJECT=$* -f infra/docker/Dockerfile.fast -t pennywise-$*:local .

prod-config: ## Validate production Compose using image variables from the environment
	@test -n "$(PENNYWISE_ACCOUNTS_IMAGE)" || (echo "Set PENNYWISE_ACCOUNTS_IMAGE"; exit 2)
	@test -n "$(PENNYWISE_EXPENSE_CORE_IMAGE)" || (echo "Set PENNYWISE_EXPENSE_CORE_IMAGE"; exit 2)
	@test -n "$(PENNYWISE_NOTIFICATIONS_IMAGE)" || (echo "Set PENNYWISE_NOTIFICATIONS_IMAGE"; exit 2)
	@test -n "$(PENNYWISE_BFF_IMAGE)" || (echo "Set PENNYWISE_BFF_IMAGE"; exit 2)
	@$(COMPOSE) -f $(PROD_COMPOSE) config --quiet

clean: ## Remove generated Gradle/build outputs
	@$(GRADLE) clean

clean-gradle: ## Stop Gradle daemons and remove generated outputs
	@$(GRADLE) --stop || true
	@$(GRADLE) clean

status: ## Show Git state and recent commits
	@git status --short
	@git log --oneline -5
