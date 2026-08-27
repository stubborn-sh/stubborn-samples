#!/usr/bin/env bash
#
# set-samples-version.sh
#
# Point every sample at a given stubborn-contract version. Rewrites the three
# reference forms the samples use, in both Maven and Gradle builds:
#
#   * Maven:  <stubborn-contract.version>X</stubborn-contract.version>
#   * Gradle: id 'sh.stubborn.contract' version 'X'      (the contract plugin)
#   * Gradle: 'sh.stubborn:<artifact>:X'                 (starters / building blocks)
#
# Each sample's own project <version> is left untouched. Idempotent.
#
# Usage: scripts/set-samples-version.sh <version>
#   e.g. scripts/set-samples-version.sh 0.1.3
#        scripts/set-samples-version.sh 0.2.0-SNAPSHOT
#
set -euo pipefail

VERSION="${1:?usage: set-samples-version.sh <version>}"
# SAMPLES_ROOT lets the BATS suite point the rewrite at a throwaway fixture.
ROOT="${SAMPLES_ROOT:-$(cd "$(dirname "$0")/.." && pwd)}"
cd "$ROOT"

# Maven: the stubborn-contract.version property (matches whatever it currently holds).
find . -name pom.xml -not -path '*/target/*' -exec sed -i -E \
  "s#(<stubborn-contract\.version>)[^<]+(</stubborn-contract\.version>)#\1${VERSION}\2#g" {} +

# Gradle: the sh.stubborn.contract plugin version, and sh.stubborn:* coordinates.
# -type f so the gradle cache directory (.gradle) is never matched; skip build/ + target/.
find . -type f -name '*.gradle' \
  -not -path '*/.gradle/*' -not -path '*/build/*' -not -path '*/target/*' -exec sed -i -E \
  "s#(sh\.stubborn:[a-z-]+:)[0-9][^'\"]*#\1${VERSION}#g; s#(id ['\"]sh\.stubborn\.contract['\"] version ['\"])[^'\"]+#\1${VERSION}#g" {} +

echo "Set stubborn-contract version to ${VERSION} across the samples."
