#!/bin/bash

EXTRA_ARGS=$@
mvn clean verify -DskipTests -P-docker-enabled $EXTRA_ARGS
