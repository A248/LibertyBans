#!/bin/bash

EXTRA_ARGS=$@
mvn clean verify -DskipTests $EXTRA_ARGS
