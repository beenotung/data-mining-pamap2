#!/usr/bin/env bash
bash ./run_db_node.sh &
./opt/sbt runCompute
