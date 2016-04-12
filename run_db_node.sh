#!/usr/bin/env bash
#rm -rf ./rethinkdb_data

# aliyun
# rethinkdb --join 58.96.176.223:29015 --bind all

# janus
# rethinkdb --join 128.199.97.91:29015 --bind all

# polyu
rethinkdb --join 158.132.8.147:29015 --bind all --server-name $(ip addr | grep inet6 | tail -n 1 | awk '{print "pamap2_node_" $2}' | sed 's/\s//g' | sed 's/\///g' | sed 's/://g' )
