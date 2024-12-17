"""DuckDB REST API server."""
import os
from typing import Dict, List, Optional, Union
from fastapi import FastAPI, HTTPException, UploadFile
from fastapi.responses import JSONResponse
import duckdb

app = FastAPI(title="DuckDB Analytics API")

# Initialize DuckDB connection
DB_PATH = os.getenv("DUCKDB_DATABASE", ":memory:")
conn = duckdb.connect(DB_PATH)

@app.get("/health")
async def health_check() -> Dict[str, str]:
    """Health check endpoint."""
    try:
        conn.execute("SELECT 1")
        return {"status": "healthy"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/query")
async def execute_query(query: str) -> Dict[str, Union[str, List[Dict]]]:
    """Execute a SQL query."""
    try:
        result = conn.execute(query).fetchdf()
        return {
            "status": "success",
            "data": result.to_dict(orient="records")
        }
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.post("/upload")
async def upload_csv(
    file: UploadFile,
    table_name: str,
    if_exists: str = "fail"
) -> Dict[str, str]:
    """Upload a CSV file to DuckDB."""
    if not file.filename.endswith('.csv'):
        raise HTTPException(
            status_code=400,
            detail="Only CSV files are supported"
        )
    
    try:
        # Create a temporary table from the CSV
        conn.execute(f"""
            CREATE TABLE IF NOT EXISTS {table_name} AS 
            SELECT * FROM read_csv_auto(?)
        """, [file.file])
        
        return {
            "status": "success",
            "message": f"Data loaded into table {table_name}"
        }
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.get("/tables")
async def list_tables() -> Dict[str, List[str]]:
    """List all tables in the database."""
    try:
        tables = conn.execute("""
            SELECT table_name 
            FROM information_schema.tables 
            WHERE table_schema = 'main'
        """).fetchall()
        return {
            "tables": [table[0] for table in tables]
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/schema/{table_name}")
async def get_schema(table_name: str) -> Dict[str, List[Dict]]:
    """Get schema for a specific table."""
    try:
        schema = conn.execute(f"""
            SELECT column_name, data_type
            FROM information_schema.columns
            WHERE table_name = ?
        """, [table_name]).fetchdf()
        return {
            "schema": schema.to_dict(orient="records")
        }
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.post("/analyze/{table_name}")
async def analyze_table(
    table_name: str,
    columns: Optional[List[str]] = None
) -> Dict[str, Dict]:
    """Get basic statistics for a table or specific columns."""
    try:
        if columns is None:
            # Get all columns
            columns = conn.execute(f"""
                SELECT column_name
                FROM information_schema.columns
                WHERE table_name = ?
            """, [table_name]).fetchall()
            columns = [col[0] for col in columns]

        stats = {}
        for col in columns:
            stats[col] = conn.execute(f"""
                SELECT 
                    COUNT(*) as count,
                    COUNT(DISTINCT {col}) as unique_count,
                    MIN({col}) as min_value,
                    MAX({col}) as max_value
                FROM {table_name}
            """).fetchone()

        return {"statistics": stats}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))
