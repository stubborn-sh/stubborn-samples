#!/usr/bin/env bash
#
# open-bump-pr.sh
#
# Open (or refresh) an auto-merging PR that pins the samples to a stubborn-contract
# version. Assumes set-samples-version.sh has already rewritten the working tree.
# Called from .github/workflows/bump-stubborn-version.yml.
#
# If the working tree is unchanged (already on the target version) it is a no-op.
# Otherwise it commits to chore/bump-stubborn-<version>, pushes, opens a PR and
# enables auto-merge (which waits for the samples CI to go green).
#
# Usage: open-bump-pr.sh <version>
#
# Environment:
#   GH    gh binary override (for tests)
#   GIT   git binary override (for tests)
#
set -euo pipefail

VERSION="${1:?usage: open-bump-pr.sh <version>}"
GH="${GH:-gh}"
GIT="${GIT:-git}"

if "${GIT}" diff --quiet; then
	echo "Samples already pinned to ${VERSION}; nothing to do."
	exit 0
fi

branch="chore/bump-stubborn-${VERSION}"
"${GIT}" config user.name "github-actions[bot]"
"${GIT}" config user.email "41898282+github-actions[bot]@users.noreply.github.com"
"${GIT}" checkout -b "${branch}"
"${GIT}" commit -am "chore: pin samples to stubborn-contract ${VERSION}"
"${GIT}" push -f -u origin "${branch}"

"${GH}" pr create --base main --head "${branch}" \
	--title "chore: pin samples to stubborn-contract ${VERSION}" \
	--body "Automated: stubborn-contract ${VERSION} was released, so every sample is pinned to it and CI verifies against the release. Auto-merges once CI is green." \
	|| echo "PR already exists for ${branch}"

"${GH}" pr merge --auto --squash "${branch}" || echo "Auto-merge not enabled; merge manually once green."
