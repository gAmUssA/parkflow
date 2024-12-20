#!/usr/bin/env python3
import os
import sys
import time
import click
import subprocess
from rich.console import Console
from rich.table import Table
from rich.progress import Progress, SpinnerColumn, TextColumn
from rich.panel import Panel
from rich.live import Live

console = Console()

def run_command(cmd, cwd=None):
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

@cli.command()
def start():
    """🚀 Start all services"""
    with console.status("[bold blue]Starting ParkFlow services...") as status:
        run_command(["make", "start"])
    console.print(Panel.fit("✨ ParkFlow is ready!", border_style="green"))

@cli.command()
def stop():
    """🛑 Stop all services"""
    with console.status("[bold red]Stopping ParkFlow services..."):
        run_command(["make", "stop"])
    console.print("[red]Services stopped[/red]")

@cli.command()
def status():
    """📊 Show detailed services status"""
    table = Table(title="🚀 ParkFlow Services Status")
    table.add_column("Service", style="cyan")
    table.add_column("Status", style="green")
    table.add_column("Port", style="blue")
    table.add_column("Health", style="magenta")

    services = {
        "Kafka": {"port": 9092, "health_cmd": ["kcat", "-b", "localhost:9092", "-L"]},
        "Schema Registry": {"port": 8081, "health_cmd": ["curl", "-s", "http://localhost:8081/subjects"]},
        "DuckDB": {"port": 3000, "health_cmd": None}
    }

    for service, info in services.items():
        status = run_command(["docker-compose", "ps", service.lower().replace(" ", "-")])
        is_running = "Up" in status.stdout
        status_text = "[green]Running[/green]" if is_running else "[red]Stopped[/red]"
        
        health = "N/A"
        if is_running and info["health_cmd"]:
            try:
                run_command(info["health_cmd"])
                health = "[green]Healthy[/green]"
            except:
                health = "[red]Unhealthy[/red]"

        table.add_row(
            service,
            status_text,
            str(info["port"]),
            health
        )

    console.print(table)

@cli.command()
def validate():
    """🔍 Validate services connectivity"""
    with console.status("[bold blue]Validating services..."):
        run_command(["make", "validate"])

@cli.command()
def clean():
    """🧹 Clean up all containers and volumes"""
    if click.confirm("⚠️  This will remove all containers and volumes. Are you sure?"):
        with console.status("[bold red]Cleaning up..."):
            run_command(["make", "clean"])
        console.print("[green]Cleanup complete![/green]")

@cli.command()
def logs():
    """📝 Show service logs"""
    try:
        subprocess.run(["make", "logs"], check=True)
    except KeyboardInterrupt:
        pass

if __name__ == "__main__":
    cli()
