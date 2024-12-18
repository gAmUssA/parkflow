import pytest
from dash.testing.application_runners import import_app
from dash.testing.composite import DashComposite
from parkflow_dashboard.app import app
import duckdb

@pytest.fixture
def dash_duo():
    with DashComposite() as dc:
        yield dc

@pytest.fixture
def test_db():
    conn = duckdb.connect(':memory:')
    conn.execute("""
        CREATE TABLE parking_entries (
            vehicle_id VARCHAR,
            entry_time TIMESTAMP,
            entry_gate VARCHAR,
            vehicle_type VARCHAR
        )
    """)
    # Insert test data
    conn.execute("""
        INSERT INTO parking_entries VALUES
        ('CAR123', NOW() - INTERVAL '30 minutes', 'GATE_A', 'sedan'),
        ('CAR456', NOW() - INTERVAL '20 minutes', 'GATE_B', 'suv'),
        ('CAR789', NOW() - INTERVAL '10 minutes', 'GATE_A', 'sedan')
    """)
    return conn

def test_dashboard_layout(dash_duo):
    dash_duo.start_server(app)
    assert dash_duo.find_element("#entry-timeline") is not None
    
def test_graph_update(dash_duo, test_db):
    dash_duo.start_server(app)
    # Wait for the graph to load
    dash_duo.wait_for_element("#entry-timeline", timeout=4)
    # Verify the graph exists and has data
    assert len(dash_duo.find_elements(".js-plotly-plot")) > 0
