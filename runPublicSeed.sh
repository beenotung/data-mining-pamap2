#!/usr/bin/env bash
rethinkdb --bind all --no-http-admin &
./opt/sbt runPublicSeed
