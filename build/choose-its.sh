#!/bin/bash

TEST=$1
shift 1
EXTRA_ARGS=$@

if [ -z "$TEST" ]; then
  echo "No integration test specified"
  exit 1
fi
mvn verify -Dit.test=$TEST -Dit.failIfNoSpecifiedTests=false -Dinvoker.skip=true $EXTRA_ARGS
