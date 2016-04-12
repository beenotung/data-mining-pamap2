#!/usr/bin/env bash
grep -v '#' ips.txt | sed 's/ /\n/g' | xargs -I {} echo sshpass -p node ssh node@{} "'killall java; cd data-mining-pamap2; ./leave.sh' &" | source /dev/stdin
