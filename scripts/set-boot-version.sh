#!/usr/bin/env bash
#
# set-boot-version.sh
#
# Point every sample at a given Spring Boot version, so the suite can be run
# against more than the one Boot line it is pinned to. Rewrites both forms:
#
#   * Maven:  the spring-boot-starter-parent <version>
#   * Gradle: id 'org.springframework.boot' version 'X'
#
# Boot's version cannot be a property in the samples: spring-boot-starter-parent
# is the POM parent, a parent version has to be a literal, and its inherited
# dependencyManagement beats an imported spring-boot-dependencies BOM — so a
# profile cannot retarget it either. Rewriting the literal is what works, which
# is why this is a script rather than a -D flag.
#
# Used by the boot-compatibility CI job. Not part of a release: nothing calls
# this on main, and the rewrite is meant to be thrown away with the checkout.
#
# Usage: scripts/set-boot-version.sh <version>
#   e.g. scripts/set-boot-version.sh 4.0.9
#
set -euo pipefail

VERSION="${1:?usage: set-boot-version.sh <version>}"
# SAMPLES_ROOT lets the BATS suite point the rewrite at a throwaway fixture.
ROOT="${SAMPLES_ROOT:-$(cd "$(dirname "$0")/.." && pwd)}"
cd "$ROOT"

# Maven: the spring-boot-starter-parent version, and only that one. Matched as the
# <version> that directly follows the starter-parent artifactId, so no other
# <version> in the file can be caught by accident.
find . -name pom.xml -not -path '*/target/*' -print0 | while IFS= read -r -d '' pom; do
	# \${1} and \${2}, braced: a bare \$1 followed by a version starting with a digit
	# reads as capture group \$14 and silently expands to nothing.
	perl -0pi -e "s{(<artifactId>spring-boot-starter-parent</artifactId>\s*\n\s*<version>)[^<]+(</version>)}{\${1}${VERSION}\${2}}g" "$pom"
done

# Gradle: the Spring Boot plugin version.
find . -type f -name '*.gradle' \
	-not -path '*/.gradle/*' -not -path '*/build/*' -not -path '*/target/*' -exec sed -i -E \
	"s#(id ['\"]org\.springframework\.boot['\"] version ['\"])[^'\"]+#\1${VERSION}#g" {} +

echo "Set Spring Boot version to ${VERSION} across the samples."
