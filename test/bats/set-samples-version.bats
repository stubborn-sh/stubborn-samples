#!/usr/bin/env bats
# Tests for scripts/set-samples-version.sh

setup() {
	REPO_ROOT="$(cd "$BATS_TEST_DIRNAME/../.." && pwd)"
	SCRIPT="$REPO_ROOT/scripts/set-samples-version.sh"
	TMP="$(mktemp -d)"

	# A throwaway sample layout: one Maven module and one Gradle module, each
	# referencing stubborn-contract, plus a sample's own project <version>.
	mkdir -p "$TMP/sample-a" "$TMP/sample-b/producer"
	cat >"$TMP/sample-a/pom.xml" <<-'POM'
		<project>
		  <version>0.1.0-SNAPSHOT</version>
		  <properties>
		    <stubborn-contract.version>0.1.0-SNAPSHOT</stubborn-contract.version>
		  </properties>
		</project>
	POM
	cat >"$TMP/sample-b/producer/build.gradle" <<-'GRADLE'
		plugins {
		    id 'sh.stubborn.contract' version '0.1.0-SNAPSHOT'
		}
		dependencies {
		    testImplementation 'sh.stubborn:stubborn-contract-starter-verifier:0.1.0-SNAPSHOT'
		}
	GRADLE
}

teardown() {
	rm -rf "$TMP"
}

@test "set-samples-version: rewrites every stubborn reference" {
	SAMPLES_ROOT="$TMP" run bash "$SCRIPT" 0.2.0
	[ "$status" -eq 0 ]
	grep -q '<stubborn-contract.version>0.2.0</stubborn-contract.version>' "$TMP/sample-a/pom.xml"
	grep -q "id 'sh.stubborn.contract' version '0.2.0'" "$TMP/sample-b/producer/build.gradle"
	grep -q "sh.stubborn:stubborn-contract-starter-verifier:0.2.0" "$TMP/sample-b/producer/build.gradle"
}

@test "set-samples-version: leaves each sample's own project version untouched" {
	SAMPLES_ROOT="$TMP" run bash "$SCRIPT" 0.2.0
	[ "$status" -eq 0 ]
	grep -q '<version>0.1.0-SNAPSHOT</version>' "$TMP/sample-a/pom.xml"
}

@test "set-samples-version: works for a SNAPSHOT version too" {
	SAMPLES_ROOT="$TMP" run bash "$SCRIPT" 0.3.0-SNAPSHOT
	[ "$status" -eq 0 ]
	grep -q '<stubborn-contract.version>0.3.0-SNAPSHOT</stubborn-contract.version>' "$TMP/sample-a/pom.xml"
	grep -q "sh.stubborn:stubborn-contract-starter-verifier:0.3.0-SNAPSHOT" "$TMP/sample-b/producer/build.gradle"
}

@test "set-samples-version: requires a version" {
	run bash "$SCRIPT"
	[ "$status" -ne 0 ]
}
