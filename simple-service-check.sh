#!/bin/bash
#
# This is a simple check script for HTTP services.
#

OK=0
CRITICAL=2
TIMEOUT=2 # seconds

if [ "$#" -ne 3 ]; then
    echo "Usage $0 [host] [port] [protocol]"
    exit 1
fi

host=$1
port=$2
protocol=$3

# 1. Try to connect to the port using nc
nc -z -v -w $TIMEOUT $host $port
if [ $? -ne 0 ]; then
    echo "Connection timed out" 
    exit $CRITICAL
fi
echo


# 2. Try to access the service with curl
output=$(curl -sD - \
    -o /dev/null \
    -A "consul-http-check" \
    --max-time $TIMEOUT \
    $host:$port)

if [ $? -ne 0 ]; then
    echo "Connection timed out" 
    exit $CRITICAL
fi

echo -n "$output" | grep -v "^\s*$"

