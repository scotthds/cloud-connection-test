#!/bin/bash
mvn dependency:resolve-plugins
mvn clean
mvn install
mvn dependency:copy-dependencies
