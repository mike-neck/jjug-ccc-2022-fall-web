#!/usr/bin/env bash

set -e

nettyArch="${1:-"aarch_64"}"
nettyVersion="4.1.81.Final"
nettyResolver="netty-resolver-dns-native-macos-${nettyVersion}-osx-${nettyArch}.jar"
nettyResolverUrl="https://repo1.maven.org/maven2/io/netty/netty-resolver-dns-native-macos/${nettyVersion}/${nettyResolver}"

curl -sL "${nettyResolverUrl}" -o "async-server/build/libs/runtime/${nettyResolver}"
