#!/usr/bin/env bash
rethinkdb --bind all &
./opt/sbt runLocalSeed
