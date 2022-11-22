#!/usr/bin/env bash

set -eou

project="${1}"

cat<<EOF > "${project}/${project}.iml"
<?xml version="1.0" encoding="UTF-8"?>
<module type="JAVA_MODULE" version="4">
  <component name="NewModuleRootManager" inherit-compiler-output="true">
    <exclude-output />
    <content url="file://\$MODULE_DIR\$">
      <sourceFolder url="file://\$MODULE_DIR\$/resources" type="java-resource" />
      <sourceFolder url="file://\$MODULE_DIR\$/src" isTestSource="false" />
      <excludeFolder url="file://\$MODULE_DIR\$/build" />
    </content>
    <orderEntry type="inheritedJdk" />
    <orderEntry type="sourceFolder" forTests="false" />
    <orderEntry type="module-library" scope="COMPILE">
      <library>
        <CLASSES>
          <root url="file://\$MODULE_DIR\$/build/libs/compile" />
        </CLASSES>
        <JAVADOC />
        <SOURCES />
        <jarDirectory url="file://\$MODULE_DIR\$/build/libs/compile" recursive="false" />
      </library>
    </orderEntry>
    <orderEntry type="module-library" scope="RUNTIME">
      <library>
        <CLASSES>
          <root url="file://\$MODULE_DIR\$/build/libs/runtime" />
        </CLASSES>
        <JAVADOC />
        <SOURCES />
        <jarDirectory url="file://\$MODULE_DIR\$/build/libs/runtime" recursive="false" />
      </library>
    </orderEntry>
  </component>
</module>
EOF
