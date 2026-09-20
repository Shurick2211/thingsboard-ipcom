#!/bin/bash
# Script to start infrastructure services for local IDE development
docker compose -f docker-compose.yml -f docker-compose.postgres.yml -f docker-compose.valkey.yml -f docker-compose.kafka.yml up -d postgres zookeeper valkey kafka
