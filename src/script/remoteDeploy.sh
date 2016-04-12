#!/usr/bin/env bash
tail -n 1 ips.txt | sed 's/ /\n/g' | xargs -I {} echo sshpass -p node ssh node@{} "'cd data-mining-pamap2; ./update.sh; ./runCompute.sh' &" | source /dev/stdin
