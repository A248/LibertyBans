#!/bin/bash

#
# LibertyBans
# Copyright © 2026 Anand Beh
#
# LibertyBans is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# LibertyBans is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with LibertyBans. If not, see <https://www.gnu.org/licenses/>
# and navigate to version 3 of the GNU Affero General Public License.
#

set -eo pipefail

if [ -z "$(git status --porcelain)" ]; then
    echo "Working directory is clean. Proceeding..."
else
    echo "Working directory must be clean (git) before releasing."
    exit 1
fi

if [ "$1" = "" ] || [ "$2" = "" ]
then
  echo "Usage: ./orchestrate-release.sh <release version> <next snapshot>. For example, if the current release is supposed to be 1.2.1, run build/orchestrate-release.sh 1.2.1 1.2.2-SNAPSHOT. This script requires commands git, mkdir, mv, sha512sum, cut, cat."
  exit 1
fi

echo "Picked release number { $1 } and next snapshot { $2 }. Is this correct? (y/n)"
read answer
if [ "$answer" != "${answer#[Yy]}" ] ; then
  echo "Executing release..."
else
  echo "Aborting."
  exit 1
fi

PROG_NAME="orchestrate-release"
OUT_DIR="build/$PROG_NAME/$1"
MVN_OUT_DIR="$OUT_DIR/mvn-exec"
mkdir -p $MVN_OUT_DIR

./mvnw versions:set -DnewVersion=$1 > $MVN_OUT_DIR/1-versions-set
echo "Updated build file versions"

./mvnw clean verify -Pbuild-reproducible,update-dependency-hashes,-docker-enabled,skip-all-tests -Daether.artifactResolver.postProcessor.trustedChecksums.failIfMissing=false > $MVN_OUT_DIR/2-prepare-and-verify
DL_HASH=`sha512sum bans-distribution/download/target/bans-download-$1.jar | cut -d ' ' -f 1`
echo "Prepared build and self-implementation checksum: $DL_HASH"

# Update checksums. Must be done separately because bans-boostrap/bootstrap includes DL_HASH in its jar (circular dependency)
./mvnw clean verify -Dgpg.skip=true -Pbuild-reproducible,build-release,update-dependency-hashes,-docker-enabled,skip-all-tests -Ddeployment.self-impl-hash=$DL_HASH -Daether.artifactResolver.postProcessor.trustedChecksums.failIfMissing=false -Daether.artifactResolver.postProcessor.trustedChecksums.record=true > $MVN_OUT_DIR/3-update-check-hash

# Should be equal to DL_HASH. Check for integrity, or abort
DL_HASH_CHECK=`cat .mvn/artifact-checksums/org/libertybans/bans-download/$1/bans-download-$1.jar.sha512`
if [ "$DL_HASH" != "$DL_HASH_CHECK" ]
then
  echo "Unexpected hash recomputation: $DL_HASH_CHECK"
  echo "This indicates a non-reproducible build. Aborting release process."
  exit 1 
fi

echo "Updated checksum records. Moving logs to target/ and committing..."
mkdir -p target/$PROG_NAME
mv $OUT_DIR target/$PROG_NAME/$1
rm -R build/$PROG_NAME

# Add commit and tag
git add .
git commit -m "Release $1"
git tag -s -m "Release $1 (automatic tag via script)" $1

echo "Committed files. Deploying release..."
./mvnw clean deploy -Pbuild-reproducible,build-release,-docker-enabled,skip-all-tests -Ddeployment.self-impl-hash=$DL_HASH
echo "Release success. Must log-in to Maven Central's Sonatype website and publish the package."

./mvnw -q versions:set -DnewVersion=$2
git add .
git commit -m "Update to next snapshot version"
echo "Updated to and committed next snapshot version. Don't forget to release $1 on central."

