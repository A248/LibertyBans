#!/bin/bash

TEST=$1
if [ -z "$TEST" ]; then
  echo "No integration test specified"
  exit 1
fi
mvn clean verify -Dit.test=$TEST -Dit.failIfNoSpecifiedTests=false -Dinvoker.skip=true
