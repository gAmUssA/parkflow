# ParkFlow

Parking management system with real-time analytics and processing capabilities.

## Project Structure

- `parkflow-common`: Shared models and utilities
- `parkflow-stream`: Kafka Streams processor
- `parkflow-flink`: Flink processor implementation
- `parkflow-entry-exit`: Gate control service
- `parkflow-payment`: Payment processing service
- `parkflow-analytics`: DuckDB analytics engine
- `parkflow-gateway`: API Gateway
- `parkflow-dashboard`: Plotly Dash UI

## Technology Stack

- JVM Services: Kotlin with Gradle (Kotlin DSL)
- Python Services: Python 3.11+ with UV package manager
- Stream Processing: Kafka Streams 3.6.1, Apache Flink 1.19
- REST Services: FastAPI
- Analytics: DuckDB
- Dashboards: Plotly/Dash
- Event Schemas: Apache Avro
- Testing: Kotest (JVM), pytest (Python)

## Development Setup

### Prerequisites

- JDK 17+
- Python 3.11+
- Docker and Docker Compose
- UV package manager

### Building the Project

For JVM services:
```bash
./gradlew build
```

For Python services:
```bash
uv venv
source .venv/bin/activate
uv pip install -r requirements.txt
```

### Running Infrastructure

```bash
docker-compose up -d
```

## Testing

JVM Services:
```bash
./gradlew test
```

Python Services:
```bash
pytest
```

Coverage reports will be generated in the `build/reports` directory for JVM services and `.coverage` for Python services.
