#!/usr/bin/env bash

set -ex

cd $(dirname $0)

branch=$GIT_BRANCH
if [[ "$1" != "" ]]; then
    branch=$1
fi
if [[ "$branch" = "" ]]; then
    branch=master
fi

for file in "pull.sh" "appsody-pull.sh" "cli-pull.sh"; do
    curl -fsSL "https://raw.githubusercontent.com/eclipse/codewind-vscode/$branch/dev/bin/$file" -o "$file"
    chmod ugo+x $file
done

./pull.sh

cd -
