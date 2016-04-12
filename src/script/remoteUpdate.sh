#!/usr/bin/env bash
grep -v '#' ips.txt | sed 's/ /\n/g' | xargs -I {} echo "echo sending to {};" sshpass -p node ssh -oStrictHostKeyChecking=no node@{} "'echo sent to {}; cd data-mining-pamap2; ./update.sh' &" | source /dev/stdin
