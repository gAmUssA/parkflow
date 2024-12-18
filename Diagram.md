# ParkFlow Project Structure

```mermaid
graph TD
    A[ParkFlow] --> B[parkflow-entry-exit]
    A --> C[parkflow-dashboard]
    A --> D[docker]
    
    B --> B1[Kotlin Service]
    B --> B2[build.gradle.kts]
    B --> B3[src/]
    
    C --> C1[Python Service]
    C --> C2[src/]
    C2 --> C21[app.py]
    C2 --> C22[kafka_duckdb_connector.py]
    
    D --> D1[duckdb/]
    D1 --> D11[server.py]
    D1 --> D12[Dockerfile]
    
    style A fill:#f9f,stroke:#333,stroke-width:2px
    style B1 fill:#bbf,stroke:#333
    style C1 fill:#bbf,stroke:#333

%% Data Flow
    subgraph Flow
    direction LR
        E[Entry API<br>:8085] -->|events| F[Kafka<br>:9092]
        F -->|consume| G[Kafka-DuckDB<br>Connector]
        G -->|write| H[DuckDB<br>:3000]
        H -->|query| I[Dashboard<br>:8050]
    end

    style E fill:#85C1E9
    style F fill:#F8C471
    style G fill:#82E0AA
    style H fill:#BB8FCE
    style I fill:#F1948A
```

## Components

1. **Entry API** (`:8085`): Receives vehicle entry events
2. **Kafka** (`:9092`): Message broker for event streaming
3. **Kafka-DuckDB Connector**: Consumes events and writes to DuckDB
4. **DuckDB** (`:3000`): Analytics database
5. **Dashboard** (`:8050`): Real-time visualization
