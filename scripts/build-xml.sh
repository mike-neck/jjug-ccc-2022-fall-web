#!/usr/bin/env bash

set -eou

project="${1}"

if [[ -f "${project}/build.xml" ]]; then
    echo "build definition(${project}/build.xml) exists"
    exit 0
fi
echo "generating build definition(${project}/build.xml)"

cat<<EOF > "${project}/build.xml"
<?xml version="1.0" encoding="UTF-8" ?>
<project
        name="${project}"
        basedir="."
        xmlns:ivy="antlib:org.apache.ivy.ant">
    <property name="main-src" location="src"/>
    <property name="main-resources" location="resources"/>
    <property name="build-dir" location="build"/>
    <property name="classes-dir" location="\${build-dir}/classes"/>
    <property name="resources-dir" location="\${build-dir}/resources"/>
    <property name="lib-dir" location="\${build-dir}/libs"/>

    <path id="compile-path">
        <fileset dir="\${lib-dir}/compile"/>
    </path>
    <path id="runtime-path">
        <path location="\${classes-dir}"/>
        <path location="\${resources-dir}"/>
        <fileset dir="\${lib-dir}/runtime"/>
    </path>

    <ivy:settings file="../ivysettings.xml"/>

    <target name="resolve">
        <ivy:retrieve pattern="\${lib-dir}/[conf]/[organization]-[artifact]-[revision].[ext]"/>
    </target>

    <target name="compile" depends="resolve">
        <mkdir dir="\${classes-dir}"/>
        <javac
                classpathref="compile-path"
                srcdir="\${main-src}"
                destdir="\${classes-dir}"
                source="19"
                target="19"
        >
            <compilerarg value="--enable-preview"/>
        </javac>
    </target>

    <target name="resources">
        <mkdir dir="\${resources-dir}"/>
        <copy todir="\${resources-dir}">
            <fileset dir="\${main-resources}"/>
        </copy>
    </target>

    <target name="run" depends="resources,compile">
        <java
                classpathref="runtime-path"
                classname="com.example.Background"
                fork="true"
        >
            <jvmarg value="--enable-preview"/>
        </java>
    </target>

    <target name="clean">
        <delete dir="\${classes-dir}"/>
        <delete dir="\${resources-dir}"/>
    </target>
    <target name="clean-deps">
        <delete dir="\${lib-dir}"/>
    </target>
</project>
EOF