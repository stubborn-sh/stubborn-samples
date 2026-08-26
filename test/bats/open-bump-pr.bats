#!/usr/bin/env bats
# Tests for scripts/open-bump-pr.sh

setup() {
	REPO_ROOT="$(cd "$BATS_TEST_DIRNAME/../.." && pwd)"
	SCRIPT="$REPO_ROOT/scripts/open-bump-pr.sh"
	TMP="$(mktemp -d)"

	export GIT_LOG="$TMP/git.log"
	export GH_LOG="$TMP/gh.log"
	: >"$GIT_LOG"; : >"$GH_LOG"

	# Fake git: `diff --quiet` exits with $GIT_DIRTY (0 = clean, 1 = has changes);
	# every other subcommand is recorded and succeeds.
	cat >"$TMP/git" <<-'STUB'
		#!/usr/bin/env bash
		if [ "$1" = "diff" ] && [ "$2" = "--quiet" ]; then
		  exit "${GIT_DIRTY:-0}"
		fi
		printf '%s\n' "$*" >>"$GIT_LOG"
		exit 0
	STUB
	chmod +x "$TMP/git"

	# Fake gh: record every invocation.
	cat >"$TMP/gh" <<-'STUB'
		#!/usr/bin/env bash
		printf '%s\n' "$*" >>"$GH_LOG"
		exit 0
	STUB
	chmod +x "$TMP/gh"

	export GIT="$TMP/git" GH="$TMP/gh"
}

teardown() {
	rm -rf "$TMP"
}

@test "open-bump-pr: no-op when the working tree is clean" {
	GIT_DIRTY=0 run bash "$SCRIPT" 0.2.0
	[ "$status" -eq 0 ]
	[[ "$output" == *"nothing to do"* ]]
	[ ! -s "$GH_LOG" ]   # no PR was opened
}

@test "open-bump-pr: commits, pushes and opens an auto-merging PR when dirty" {
	GIT_DIRTY=1 run bash "$SCRIPT" 0.2.0
	[ "$status" -eq 0 ]
	grep -q "checkout -b chore/bump-stubborn-0.2.0" "$GIT_LOG"
	grep -q "commit -am chore: pin samples to stubborn-contract 0.2.0" "$GIT_LOG"
	grep -q "push -f -u origin chore/bump-stubborn-0.2.0" "$GIT_LOG"
	grep -q "pr create" "$GH_LOG"
	grep -q "pr merge --auto --squash chore/bump-stubborn-0.2.0" "$GH_LOG"
}

@test "open-bump-pr: requires a version" {
	run bash "$SCRIPT"
	[ "$status" -ne 0 ]
}
