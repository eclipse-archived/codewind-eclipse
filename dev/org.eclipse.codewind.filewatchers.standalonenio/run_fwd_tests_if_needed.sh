#!/usr/bin/env bash
#
#*******************************************************************************
# Copyright (c) 2020 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v2.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v20.html
#
# Contributors:
#     IBM Corporation - initial API and implementation
#*******************************************************************************

export SCRIPT_LOCT=`dirname $0`
export SCRIPT_LOCT=`cd $SCRIPT_LOCT; pwd`
cd $SCRIPT_LOCT

if [[ -z "$CHANGE_TARGET" ]]; then
	echo "* Filewatcher test script detected a non-PR build, so exiting."
    exit 0
fi

GIT_DIFF=`git diff remotes/origin/"$CHANGE_TARGET"`

CHANGE_COUNT=`printf %s "$GIT_DIFF" | grep "diff --git" | grep "eclipse.codewind.filewatchers" | grep -v "filewatchers.eclipse" |  wc -l`

if [ "$CHANGE_COUNT" == "0" ]; then
	echo "* No filewatcherd changes detected in Git diff list."
    exit 0
fi

# Output Git Diff for debug purposes, until the above code matures.
echo "Git Diff:"
printf %s "$GIT_DIFF"


set -euo pipefail


echo 
echo "Download Maven and add to path"
echo
cd "$SCRIPT_LOCT"
curl -LO http://mirror.dsrg.utoronto.ca/apache/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz
tar xzf apache-maven-3.6.3-bin.tar.gz
cd apache-maven-3.6.3/bin
export PATH=`pwd`:$PATH


echo
echo "Building Java filewatcher"
echo
cd "$SCRIPT_LOCT/../org.eclipse.codewind.filewatchers.core"
mvn install
cd "$SCRIPT_LOCT"
mvn package


echo
echo "Build and run test suite"
echo
cd "$SCRIPT_LOCT"
git clone "https://github.com/eclipse/codewind-filewatchers"
cd codewind-filewatchers
git checkout "$CHANGE_TARGET"
cd Tests/

./run_tests_java_filewatcher_on_target.sh "$SCRIPT_LOCT/../.."

