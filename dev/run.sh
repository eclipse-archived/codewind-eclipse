#!/bin/sh
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

set -e

export SCRIPT_DIR=`dirname $0`
export SCRIPT_DIR=`cd $SCRIPT_DIR; pwd`
cd $SCRIPT_DIR

if [ -z $CODE_TESTS_WORKSPACE ]; then
    export CODE_TESTS_WORKSPACE="${PWD}/cw-test-workspace/"
fi

mkdir -p $CODE_TESTS_WORKSPACE
cd $CODE_TESTS_WORKSPACE

wget --no-verbose http://www.eclipse.org/external/technology/epp/downloads/release/2019-12/R/eclipse-jee-2019-12-R-linux-gtk-x86_64.tar.gz
tar xzf eclipse-jee-2019-12-R-linux-gtk-x86_64.tar.gz

chmod 755 eclipse/eclipse

echo "Downloading eclipse..."

eclipse/eclipse -nosplash -application org.eclipse.equinox.p2.director -repository "https://download.eclipse.org/technology/swtbot/releases/2.8.0/" -installIU "org.eclipse.swtbot.eclipse.gef.feature.group, org.eclipse.swtbot.generator.feature.feature.group, org.eclipse.swtbot.ide.feature.group, org.eclipse.swtbot.eclipse.feature.group, org.eclipse.swtbot.forms.feature.group, org.eclipse.swtbot.feature.group, org.eclipse.swtbot.eclipse.test.junit.feature.group"

echo "Unzipping features..."

cd $SCRIPT_DIR/ant_build/artifacts/

unzip codewind-*.zip -d code
chmod -R 777 code

unzip codewind_test-*.zip -d test
chmod -R 777 test

cd $CODE_TESTS_WORKSPACE

echo "Installing codewind and codewind test..."

eclipse/eclipse -nosplash -application org.eclipse.equinox.p2.director -repository "file:$SCRIPT_DIR/ant_build/artifacts/code" -installIU "org.eclipse.codewind.feature.group"
eclipse/eclipse -nosplash -application org.eclipse.equinox.p2.director -repository "file:$SCRIPT_DIR/ant_build/artifacts/test" -installIU "org.eclipse.codewind.test.feature.feature.group"

cd $SCRIPT_DIR

set +e

echo "Run junit tests"

xvfb-run -a ./runTest.sh

return_code=$?

echo "Test result file is: /development/junit-results.xml"
ls -l /development

echo "Test finished with return code $return_code"
return $return_code


