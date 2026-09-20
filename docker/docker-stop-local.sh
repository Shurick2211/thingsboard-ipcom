#!/bin/bash
# Script to stop infrastructure services for local IDE development
docker compose -f docker-compose.yml -f docker-compose.postgres.yml -f docker-compose.valkey.yml -f docker-compose.kafka.yml stop postgres zookeeper valkey kafka
