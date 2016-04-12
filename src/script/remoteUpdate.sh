#!/usr/bin/env bash
grep -v '#' ips.txt | sed 's/ /\n/g' | xargs -I {} echo sshpass -p node ssh node@{} "'cd data-mining-pamap2; ./update.sh' &" | source /dev/stdin
