import os
import logging
import requests
import pandas as pd
import plotly.express as px
import plotly.graph_objects as go
from dash import Dash, html, dcc, Input, Output
from plotly.subplots import make_subplots

# Set up logging
logger = logging.getLogger(__name__)

# DuckDB service configuration
DUCKDB_API_URL = os.getenv('DUCKDB_API_URL', 'http://localhost:3000')

# Initialize Dash app
app = Dash(__name__)

# Layout
app.layout = html.Div([
    html.H1('ParkFlow Entry Dashboard', className='header'),
    html.Div([
        dcc.Graph(id='vehicle-entries-graph'),
        dcc.Interval(
            id='interval-component',
            interval=5*1000,  # in milliseconds
            n_intervals=0
        )
    ], className='graph-container')
], className='app-container')

# Add CSS
app.index_string = '''
<!DOCTYPE html>
<html>
    <head>
        {%metas%}
        <title>ParkFlow Dashboard</title>
        {%css%}
        <style>
            .app-container {
                max-width: 1200px;
                margin: 0 auto;
                padding: 20px;
                font-family: Arial, sans-serif;
            }
            .header {
                text-align: center;
                color: #2c3e50;
                margin-bottom: 30px;
            }
            .graph-container {
                flex: 1;
                min-width: 500px;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                padding: 15px;
                border-radius: 8px;
                background: white;
            }
        </style>
    </head>
    <body>
        {%app_entry%}
        <footer>
            {%config%}
            {%scripts%}
            {%renderer%}
        </footer>
    </body>
</html>
'''

# Callback to update the vehicle entries graph
@app.callback(
    Output('vehicle-entries-graph', 'figure'),
    Input('interval-component', 'n_intervals')
)
def update_graph(n):
    # Query DuckDB for latest data
    query = """
    SELECT 
        timestamp,
        license_plate,
        gate_id,
        vehicle_type,
        confidence
    FROM vehicle_entries
    ORDER BY timestamp DESC
    LIMIT 100
    """
    
    try:
        response = requests.post(
            f"{DUCKDB_API_URL}/query",
            params={"query": query}
        )
        response.raise_for_status()
        data = response.json()
        
        if not data or 'data' not in data:
            fig = go.Figure()
            fig.add_annotation(
                text="No data available",
                xref="paper",
                yref="paper",
                x=0.5,
                y=0.5,
                showarrow=False,
                font=dict(size=20)
            )
            return fig
        
        df = pd.DataFrame(data['data'], columns=['timestamp', 'license_plate', 'gate_id', 'vehicle_type', 'confidence'])
        
        # Create time series plot
        fig = make_subplots(
            rows=2, cols=2,
            subplot_titles=('Entries by Gate', 'Vehicle Types', 'Entry Timeline', 'Recognition Confidence'),
            specs=[[{"type": "bar"}, {"type": "pie"}],
                  [{"type": "scatter"}, {"type": "histogram"}]]
        )
        
        # Gate distribution
        gate_counts = df['gate_id'].value_counts()
        fig.add_trace(
            go.Bar(x=gate_counts.index, y=gate_counts.values, name='Entries by Gate'),
            row=1, col=1
        )
        
        # Vehicle type distribution
        type_counts = df['vehicle_type'].value_counts()
        fig.add_trace(
            go.Pie(labels=type_counts.index, values=type_counts.values, name='Vehicle Types'),
            row=1, col=2
        )
        
        # Timeline of entries
        df['timestamp'] = pd.to_datetime(df['timestamp'])
        entries_timeline = df.groupby('timestamp').size().reset_index(name='count')
        fig.add_trace(
            go.Scatter(x=entries_timeline['timestamp'], y=entries_timeline['count'], 
                      mode='lines+markers', name='Entries'),
            row=2, col=1
        )
        
        # Confidence distribution
        fig.add_trace(
            go.Histogram(x=df['confidence'], name='Recognition Confidence'),
            row=2, col=2
        )
        
        fig.update_layout(height=800, showlegend=False)
        
        return fig
        
    except Exception as e:
        logger.error(f"Error updating graph: {e}")
        fig = go.Figure()
        fig.add_annotation(
            text=f"Error loading data: {str(e)}",
            xref="paper",
            yref="paper",
            x=0.5,
            y=0.5,
            showarrow=False,
            font=dict(size=20, color='red')
        )
        return fig

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8050, debug=True)
