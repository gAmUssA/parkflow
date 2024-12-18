"""ParkFlow CLI implementation."""
import subprocess
import sys
import time
import json
import httpx

import click
from rich.console import Console
from rich.panel import Panel
from rich.progress import Progress, SpinnerColumn, TextColumn
from rich.table import Table

console = Console()

def run_command(cmd, cwd=None):
    """Run a shell command and handle errors."""
    try:
        return subprocess.run(cmd, cwd=cwd, check=True, capture_output=True, text=True)
    except subprocess.CalledProcessError as e:
        console.print(f"[red]Error running command:[/red] {' '.join(cmd)}")
        console.print(f"[red]Error output:[/red] {e.stderr}")
        sys.exit(1)

@click.group()
def cli():
    """🚀 ParkFlow Development Tools"""
    pass

@cli.group()
def entry():
    """🚗 Vehicle Entry Commands"""
    pass

@entry.command()
def event():
    """📬 Send a single vehicle entry event"""
    with console.status("[bold blue]🚀 Sending vehicle entry event...") as status:
        try:
            response = httpx.post("http://localhost:8085/api/v1/entry/event")
            response.raise_for_status()
            console.print("✅ Event sent successfully!")
        except Exception as e:
            console.print(f"[red]❌ Failed to send event: {str(e)}[/red]")
            sys.exit(1)

@entry.command()
@click.option('--events', '-e', default=10, help='Number of events to simulate')
@click.option('--delay', '-d', default=1000, help='Delay between events in milliseconds')
def simulate(events, delay):
    """🎮 Simulate multiple vehicle entries"""
    with console.status(f"[bold blue]🚀 Starting simulation with {events} events, {delay}ms delay...") as status:
        try:
            data = {
                "numberOfEvents": events,
                "delayBetweenEventsMs": delay
            }
            response = httpx.post(
                "http://localhost:8085/api/v1/entry/simulate",
                json=data
            )
            response.raise_for_status()
            console.print("✅ Simulation started!")
            total_time = events * delay / 1000
            console.print(f"🕒 Events will complete in ~{total_time} seconds")
        except Exception as e:
            console.print(f"[red]❌ Failed to start simulation: {str(e)}[/red]")
            sys.exit(1)

@cli.command()
def start():
    """🚀 Start all services"""
    with console.status("[bold blue]Starting ParkFlow services...") as status:
        run_command(["docker", "compose", "up", "-d"])
        
        with Progress(
            SpinnerColumn(),
            TextColumn("[progress.description]{task.description}"),
            console=console,
        ) as progress:
            task = progress.add_task("Waiting for services to be healthy...", total=None)
            while True:
                result = run_command(["docker", "compose", "ps"])
                if "unhealthy" not in result.stdout and "starting" not in result.stdout:
                    break
                time.sleep(1)
            progress.update(task, completed=True)
            
    console.print(Panel.fit("✨ ParkFlow is ready!", border_style="green"))
    status()

@cli.command()
def stop():
    """🛑 Stop all services"""
    with console.status("[bold red]Stopping ParkFlow services...") as status:
        run_command(["docker", "compose", "down"])
    console.print("✨ All services stopped!")

@cli.command()
def status():
    """📊 Show services status"""
    table = Table(title="ParkFlow Services Status")
    table.add_column("Service", style="cyan")
    table.add_column("Status", style="green")
    
    services = ["kafka", "schema-registry", "duckdb"]
    for service in services:
        try:
            result = run_command(["docker", "compose", "ps", service])
            status = "Running 🟢" if "Up" in result.stdout else "Stopped 🔴"
            table.add_row(service, status)
        except:
            table.add_row(service, "Error ⚠️")
    
    console.print(table)

@cli.command()
def validate():
    """🔍 Validate services connectivity"""
    with console.status("[bold blue]Validating services...") as status:
        services = {
            "Kafka": "localhost:9092",
            "Schema Registry": "http://localhost:8081",
            "DuckDB": "http://localhost:3000",
            "Entry API": "http://localhost:8085"
        }
        
        table = Table(title="Services Connectivity")
        table.add_column("Service", style="cyan")
        table.add_column("Status", style="green")
        
        for service, url in services.items():
            try:
                if url.startswith("http"):
                    response = httpx.get(f"{url}/health")
                    status = "Healthy 🟢" if response.status_code == 200 else "Unhealthy 🔴"
                else:
                    # For Kafka, just check if port is open
                    import socket
                    host, port = url.split(":")
                    sock = socket.create_connection((host, int(port)), timeout=1)
                    sock.close()
                    status = "Connected 🟢"
            except:
                status = "Error ⚠️"
            table.add_row(service, status)
        
        console.print(table)

@cli.command()
def clean():
    """🧹 Clean up all containers and volumes"""
    with console.status("[bold red]Cleaning up...") as status:
        run_command(["docker", "compose", "down", "-v"])
    console.print("✨ All cleaned up!")

@cli.command()
def logs():
    """📝 Show service logs"""
    run_command(["docker", "compose", "logs", "--tail=100", "-f"])

if __name__ == "__main__":
    cli()
