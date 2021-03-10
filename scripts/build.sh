#!/bin/bash

if [[ ! -z $1 ]]; then
    CMD=$1;
else
    CMD="compile"
fi

if [[ ! -f "scripts/build.sh" ]]; then
    echo "not currently in DevSearch top level directory, quitting";
    exit;
fi

if [ "$CMD" = "compile" ]; then
    find src -iregex .*java | xargs javac -classpath "lib/*" -d bin;
    exit;
elif [ "$CMD" = "run" ]; then
    java -classpath "lib/*:bin" com.vinayemani.devsearch.cli.CLIWrapper;
    exit;
elif [ "$CMD" = "clean" ]; then
    rm -rf bin && mkdir bin;
    exit;
fi
