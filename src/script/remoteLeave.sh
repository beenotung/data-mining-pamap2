#!/usr/bin/env bash
grep -v '#' ips.txt | sed 's/ /\n/g' | xargs -I {} echo sshpass -p node ssh -oStrictHostKeyChecking=no node@{} "'killall rethinkdb' &" | source /dev/stdin
