#!/usr/bin/env bash
killall rethinkdb
ps -o "%r %c" | grep rethinkdb | awk '{print $1}' | xargs -I {} kill -TERM -{}