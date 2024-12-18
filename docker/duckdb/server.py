"""DuckDB REST API server."""
import os
from typing import Dict, List, Optional, Union
from fastapi import FastAPI, HTTPException, UploadFile, Query
from fastapi.responses import JSONResponse
from pydantic import BaseModel
import duckdb

app = FastAPI(title="DuckDB Analytics API")

# Initialize DuckDB connection
DB_PATH = os.getenv("DUCKDB_DATABASE", ":memory:")
conn = duckdb.connect(DB_PATH)

class QueryRequest(BaseModel):
    query: str

@app.get("/health")
async def health_check() -> Dict[str, str]:
    """Health check endpoint."""
    try:
        conn.execute("SELECT 1")
        return {"status": "healthy"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/query")
async def execute_query(
    query: str = Query(None, description="SQL query to execute"),
    request: Optional[QueryRequest] = None
) -> Dict[str, Union[str, List[Dict]]]:
    """Execute a SQL query."""
    try:
        # Use query from either query parameter or request body
        sql_query = query or (request.query if request else None)
        if not sql_query:
            raise HTTPException(
                status_code=400,
                detail="Query must be provided either as a query parameter or in the request body"
            )
        
        result = conn.execute(sql_query).fetchdf()
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
            SELECT * FROM read_csv_auto('{file.filename}')
        """)
        return {"status": "success", "message": f"Table {table_name} created successfully"}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.get("/tables")
async def list_tables() -> Dict[str, List[str]]:
    """List all tables in the database."""
    try:
        tables = conn.execute("SHOW TABLES").fetchdf()
        return {
            "status": "success",
            "tables": tables["name"].tolist()
        }
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.get("/schema/{table_name}")
async def get_schema(table_name: str) -> Dict[str, Union[str, List[Dict]]]:
    """Get schema for a specific table."""
    try:
        schema = conn.execute(f"DESCRIBE {table_name}").fetchdf()
        return {
            "status": "success",
            "schema": schema.to_dict(orient="records")
        }
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.get("/analyze/{table_name}")
async def analyze_table(
    table_name: str,
    columns: Optional[List[str]] = None
) -> Dict[str, Union[str, Dict]]:
    """Get basic statistics for a table or specific columns."""
    try:
        if columns:
            col_list = ", ".join(columns)
        else:
            schema = conn.execute(f"DESCRIBE {table_name}").fetchdf()
            col_list = ", ".join(schema["column_name"].tolist())

        stats = {}
        for col in col_list.split(", "):
            result = conn.execute(f"""
                SELECT 
                    COUNT(*) as count,
                    COUNT(DISTINCT {col}) as unique_count,
                    MIN({col}) as min_value,
                    MAX({col}) as max_value
                FROM {table_name}
            """).fetchdf()
            stats[col] = result.to_dict(orient="records")[0]

        return {
            "status": "success",
            "statistics": stats
        }
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))
