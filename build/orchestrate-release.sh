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

set -euo pipefail

if [ -z "$(git status --porcelain)" ]; then
    echo "Working directory is clean. Proceeding..."
else
    echo "Working directory must be clean (git) before releasing."
    exit 1
fi

if [ "$1" = "" ]
then
  echo "Usage: ./orchestrate-release.sh <version>. Requires commands git, mkdir, mv, cat."
  exit 1
fi

echo "Picked release number { $1 }. Is this correct? (y/n)"
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

./mvnw versions:set -DnewVersion=$1 > $MVN_OUT_DIR/versions-set
echo "Updated build file versions"

./mvnw clean verify -Dgpg.skip=true -Pbuild-reproducible,update-dependency-hashes,-docker-enabled,skip-all-tests -Daether.artifactResolver.postProcessor.trustedChecksums.failIfMissing=false -Daether.artifactResolver.postProcessor.trustedChecksums.record=true > $MVN_OUT_DIR/prepare-and-verify
# These are equal. We use the second one, whose checkums are computed by the bans-distribution/release-integrator module
#DL_HASH=`sha512sum bans-distribution/download/target/bans-download-$1.jar | cut -d ' ' -f 1`
DL_HASH=`cat .mvn/artifact-checksums/org/libertybans/bans-download/$1/bans-download-$1.jar.sha512`
echo "Prepared build and computed new checksums."

# Verifies the hash before release. Logically redundant step
#./mvnw clean verify -Dgpg.skip=true -Pbuild-reproducible,build-release,-docker-enabled,skip-all-tests -Ddeployment.self-impl-hash=$DL_HASH > $MVN_OUT_DIR/check-hash

# Add commit and tag
git add .
git commit -m "Release $1"
git tag -s -m "Release $1 (automatic tag via script)" $1

echo "Committed files. Deploying release..."
./mvnw clean deploy -Pbuild-reproducible,build-release,-docker-enabled,skip-all-tests -Ddeployment.self-impl-hash=$DL_HASH > $MVN_OUT_DIR/deploy-final
echo "Release deployed. Must log-in to Maven Central's Sonatype website and publish the package."

mkdir -p target/$PROG_NAME
mv $OUT_DIR target/$PROG_NAME/$1
rm -R build/$PROG_NAME

echo "Success."

