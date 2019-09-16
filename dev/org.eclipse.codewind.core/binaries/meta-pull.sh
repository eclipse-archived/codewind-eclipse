#!/usr/bin/env bash

set -e

cd $(dirname $0)

branch=$1
if [[ -z $branch ]]; then
    branch=$CW_VSCODE_BRANCH
fi
if [[ -z $branch ]]; then
    branch="master"
fi

echo
echo "Downloading scripts from branch codewind-vscode/$branch"

for file in "pull.sh" "appsody-pull.sh" "cli-pull.sh"; do
    curl -fsSL "https://raw.githubusercontent.com/eclipse/codewind-vscode/$branch/dev/bin/$file" -o "$file"
    chmod ugo+x $file
done
curl -fsSL "https://raw.githubusercontent.com/eclipse/codewind-vscode/$branch/dev/bin/README.txt" -o "README-pull.txt"

echo "Scripts downloaded.  Run ./pull.sh to download the binary dependencies"
echo

cd -
