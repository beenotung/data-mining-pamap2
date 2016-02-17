#!/usr/bin/env bash
rm -rf ./rethinkdb_data
rethinkdb --join 58.96.176.223:29015 --bind all
