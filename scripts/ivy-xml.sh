#!/usr/bin/env bash

set -euo pipefail

project="${1}"

cat<<EOF > "${project}/ivy.xml"
<?xml version="1.0" encoding="UTF-8" ?>
<ivy-module version="2.0">
    <info organisation="com.example" module="${project}"/>
    <configurations>
        <conf name="compile" extends="runtime" description="compileClasspath + runtimeClasspath" />
        <conf name="runtime" description="runtimeClasspath"/>
    </configurations>
    <dependencies>
        <dependency name="spring-boot-starter-webflux" rev="2.7.4" org="org.springframework.boot" conf="runtime->compile" />
        <dependency name="annotations" rev="23.0.0" org="org.jetbrains" conf="compile->default" />
    </dependencies>
</ivy-module>
EOF
