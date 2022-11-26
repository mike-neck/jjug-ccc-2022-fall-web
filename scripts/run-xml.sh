#!/usr/bin/env bash

set -eu

project="${1}"

if [[ -z "${project}" ]]; then
    echo "invalid project name project=${project}"
    exit 1
fi

outputFile=".run/${project}-run.run.xml"

if [[ -f "${outputFile}" ]]; then
    echo "Run file exists"
    exit 0
fi

echo "Generating run file for project=${project}(file=${outputFile})"

cat<<EOF > "${outputFile}"
<component name="ProjectRunConfigurationManager">
  <configuration
          default="false"
          name="${project}-run"
          type="MAKEFILE_TARGET_RUN_CONFIGURATION"
          factoryName="Makefile">
    <makefile filename="\$PROJECT_DIR\$/Makefile"
              target="run"
              workingDirectory=""
              arguments="">
      <envs>
        <env name="PROJECT" value="${project}" />
      </envs>
    </makefile>
    <method v="2" />
  </configuration>
</component>
EOF
