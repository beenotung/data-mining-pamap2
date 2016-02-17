#!/usr/bin/env bash
bash ./run_db_seed.sh &
./opt/sbt runPublicSeed
