#!/usr/bin/env bash

export SCRIPT_LOCT=`dirname $0`
export SCRIPT_LOCT=`cd $SCRIPT_LOCT; pwd`
cd $SCRIPT_LOCT
set -euo pipefail


echo pre

GIT_DIFF=`git diff remotes/origin/"$CHANGE_TARGET"`

CHANGE_COUNT=`printf %s "$GIT_DIFF" | grep "Jenkins" | grep -v "filewatchers.eclipse" |  wc -l"`

echo change count $CHANGE_COUNT

echo post

# git diff remotes/origin/"$CHANGE_TARGET" master

# git diff "$BRANCH_NAME" "$CHANGE_TARGET"


# PR-595 and master
#echo "jgw: $BRANCH_NAME and $CHANGE_TARGET"

# git clone git@github.com:eclipse/codewind-filewatchers
# cd codewind-filewatchers
# git checkout "$BRANCH_NAME"
