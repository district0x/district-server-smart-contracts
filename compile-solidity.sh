#!/usr/bin/env bash
cd resources/public/contracts/src

function solc-err-only {
    solc "$@" 2>&1 | grep -A 2 -i "Error"
}

solc-err-only --overwrite --optimize --bin --abi MyContract.sol -o ../build/

cd ../build
wc -c MyContract.bin | awk '{print "MyContract: " $1}'
