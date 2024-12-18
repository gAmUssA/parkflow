FROM python:3.11-slim

WORKDIR /app

# Copy only the necessary files
COPY pyproject.toml .
COPY src/ ./src/

# Install dependencies and the package
RUN pip install --no-cache-dir -e .

# Expose the port
EXPOSE 8050

# Run the app
CMD ["python", "-m", "parkflow_dashboard.app"]
