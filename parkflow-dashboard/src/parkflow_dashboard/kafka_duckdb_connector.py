import logging
import os
import json
import requests
from confluent_kafka import Consumer
from confluent_kafka.serialization import SerializationContext, MessageField
from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.avro import AvroDeserializer
from typing import Optional
from datetime import datetime

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

DUCKDB_API_URL = os.getenv('DUCKDB_API_URL', 'http://localhost:3000')
KAFKA_BOOTSTRAP_SERVERS = os.getenv('KAFKA_BOOTSTRAP_SERVERS', 'localhost:9092')
SCHEMA_REGISTRY_URL = os.getenv('SCHEMA_REGISTRY_URL', 'http://localhost:8081')

class KafkaToDuckDBConnector:
    def __init__(self):
        self.consumer = self._init_kafka_consumer()
        self.avro_deserializer = self._init_avro_deserializer()
        self._init_duckdb_table()
        
    def _init_kafka_consumer(self) -> Consumer:
        consumer_conf = {
            'bootstrap.servers': KAFKA_BOOTSTRAP_SERVERS,
            'group.id': 'parkflow-duckdb-connector',
            'auto.offset.reset': 'earliest'
        }
        return Consumer(consumer_conf)
    
    def _init_avro_deserializer(self) -> AvroDeserializer:
        schema_registry_conf = {'url': SCHEMA_REGISTRY_URL}
        schema_registry_client = SchemaRegistryClient(schema_registry_conf)
        
        # Get the latest schema for the topic
        try:
            schema_str = schema_registry_client.get_latest_version('parking.entry.events-value').schema.schema_str
        except Exception as e:
            logger.warning(f"Failed to get schema from registry: {e}. Using default schema.")
            schema_str = """{
                "type": "record",
                "name": "VehicleEntryEvent",
                "fields": [
                    {"name": "eventId", "type": "string"},
                    {"name": "timestamp", "type": "long"},
                    {"name": "licensePlate", "type": "string"},
                    {"name": "gateId", "type": "string"},
                    {"name": "laneId", "type": "string"},
                    {"name": "confidence", "type": "double"},
                    {"name": "imageUrl", "type": ["string", "null"]},
                    {"name": "vehicleType", "type": "string"}
                ]
            }"""
        
        return AvroDeserializer(schema_registry_client,
                               schema_str,
                               lambda x, ctx: {
                                   'eventId': str(x['eventId']),
                                   'timestamp': x['timestamp'],
                                   'licensePlate': str(x['licensePlate']),
                                   'gateId': str(x['gateId']),
                                   'laneId': str(x['laneId']),
                                   'confidence': x['confidence'],
                                   'imageUrl': x['imageUrl'],
                                   'vehicleType': str(x['vehicleType']) if isinstance(x['vehicleType'], str) else x['vehicleType'].value
                               })
    
    def _init_duckdb_table(self):
        query = """
        CREATE TABLE IF NOT EXISTS vehicle_entries (
            event_id VARCHAR,
            timestamp TIMESTAMP,
            license_plate VARCHAR,
            gate_id VARCHAR,
            lane_id VARCHAR,
            confidence DOUBLE,
            image_url VARCHAR,
            vehicle_type VARCHAR
        )
        """
        response = requests.post(
            f"{DUCKDB_API_URL}/query",
            headers={'Content-Type': 'application/json'},
            params={'query': query}
        )
        if response.status_code != 200:
            raise Exception(f"Failed to create table: {response.text}")
        logger.info("DuckDB table initialized")

    def start(self):
        self.consumer.subscribe(['parking.entry.events'])
        
        try:
            while True:
                msg = self.consumer.poll(1.0)
                if msg is None:
                    continue
                if msg.error():
                    logger.error(f"Consumer error: {msg.error()}")
                    continue
                
                try:
                    # Deserialize the Avro message
                    event = self.avro_deserializer(msg.value(), SerializationContext(msg.topic(), MessageField.VALUE))
                    
                    # Convert timestamp to ISO format
                    timestamp = datetime.fromtimestamp(event['timestamp'] / 1000.0).isoformat()
                    
                    # Insert into DuckDB via REST API
                    query = f"""
                    INSERT INTO vehicle_entries 
                    (event_id, timestamp, license_plate, gate_id, lane_id, confidence, image_url, vehicle_type)
                    VALUES 
                    ('{event['eventId']}', 
                     '{timestamp}', 
                     '{event['licensePlate']}',
                     '{event['gateId']}',
                     '{event['laneId']}',
                     {event['confidence']},
                     '{event['imageUrl'] if event['imageUrl'] else 'NULL'}',
                     '{event['vehicleType']}')
                    """
                    response = requests.post(
                        f"{DUCKDB_API_URL}/query",
                        headers={'Content-Type': 'application/json'},
                        params={'query': query}
                    )
                    if response.status_code != 200:
                        logger.error(f"Failed to insert data: {response.text}")
                    else:
                        logger.info(f"Processed entry event for vehicle {event['licensePlate']}")
                    
                except Exception as e:
                    logger.error(f"Error processing message: {e}")
                    
        except KeyboardInterrupt:
            pass
        finally:
            self.consumer.close()

if __name__ == '__main__':
    connector = KafkaToDuckDBConnector()
    connector.start()
