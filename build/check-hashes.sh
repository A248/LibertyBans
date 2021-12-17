#!/bin/bash

mvn clean verify -DskipTests -Dinvoker.skip=true -Pcheck-hash,-docker-enabled
