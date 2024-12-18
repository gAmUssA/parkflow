FROM ubuntu:22.04

# Install required packages
RUN apt-get update && \
    apt-get install -y \
    wget \
    unzip \
    python3 \
    python3-pip \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Install DuckDB CLI
RUN wget https://github.com/duckdb/duckdb/releases/download/v1.1.0/duckdb_cli-linux-amd64.zip && \
    unzip duckdb_cli-linux-amd64.zip -d /usr/local/bin && \
    chmod +x /usr/local/bin/duckdb && \
    rm duckdb_cli-linux-amd64.zip

# Install Python packages for the REST API
RUN pip3 install \
    fastapi \
    uvicorn \
    python-multipart \
    duckdb==1.1.0 \
    numpy \
    pandas \
    httpx

# Create app directory
WORKDIR /app

# Copy server code
COPY server.py .

# Create data directory
RUN mkdir -p /data

# Expose port
EXPOSE 3000

# Set environment variables
ENV DUCKDB_DATABASE=/data/analytics.db
ENV PYTHONUNBUFFERED=1

# Health check
HEALTHCHECK --interval=10s --timeout=5s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:3000/health || exit 1

# Run server
CMD ["uvicorn", "server:app", "--host", "0.0.0.0", "--port", "3000"]
