#!/usr/bin/env bash

set -eu

project="${1}"

classname="$(\
  grep classname "${project}/build.xml" | \
  head -1 | \
  tr -d '"' | \
  cut -d'=' -f2
)"

echo "Shutdown..."
echo "Project: ${project}"
echo "main: ${classname}"

klass="$(\
  echo "${classname}" | \
  tr '.' '\n' | \
  tail -1
)"

kill -2 "$(jps | grep "${klass}" | cut -d' ' -f1)"
