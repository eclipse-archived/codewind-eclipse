#!/usr/bin/env bash

export SCRIPT_LOCT=`dirname $0`
export SCRIPT_LOCT=`cd $SCRIPT_LOCT; pwd`
cd $SCRIPT_LOCT


echo pre


GIT_DIFF=`git diff remotes/origin/"$CHANGE_TARGET"`

printf %s "$GIT_DIFF"

CHANGE_COUNT=`printf %s "$GIT_DIFF" | grep "diff --git" | grep "eclipse.codewind.filewatchers" | grep -v "filewatchers.eclipse" |  wc -l`

if [ "$CHANGE_COUNT" == "0" ]; then
	echo "* No filewatcherd changes detected in Git diff list."
    exit 0
fi

set -euo pipefail

echo change count $CHANGE_COUNT


cd "$SCRIPT_LOCT"


echo 
echo "Download Maven and add to path"
echo
cd $STEP_ROOT_PATH
curl -LO http://mirror.dsrg.utoronto.ca/apache/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz
tar xzf apache-maven-3.6.3-bin.tar.gz
cd apache-maven-3.6.3/bin
export PATH=`pwd`:$PATH


cd "$SCRIPT_LOCT"

mvn install org.eclipse.codewind.filewatchers.standalonenio

cd "$SCRIPT_LOCT/../org.eclipse.codewind.filewatchers.core"

mvn package

ls -l target/


echo post


# git diff remotes/origin/"$CHANGE_TARGET" master

# git diff "$BRANCH_NAME" "$CHANGE_TARGET"


# PR-595 and master
#echo "jgw: $BRANCH_NAME and $CHANGE_TARGET"

# git clone git@github.com:eclipse/codewind-filewatchers
# cd codewind-filewatchers
# git checkout "$BRANCH_NAME"
