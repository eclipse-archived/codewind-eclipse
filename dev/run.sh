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

cd /home

chmod 755 eclipse/eclipse

echo "Downloading eclipse..."

eclipse/eclipse -nosplash -application org.eclipse.equinox.p2.director -repository "https://download.eclipse.org/technology/swtbot/releases/2.8.0/" -installIU "org.eclipse.swtbot.eclipse.gef.feature.group, org.eclipse.swtbot.generator.feature.feature.group, org.eclipse.swtbot.ide.feature.group, org.eclipse.swtbot.eclipse.feature.group, org.eclipse.swtbot.forms.feature.group, org.eclipse.swtbot.feature.group, org.eclipse.swtbot.eclipse.test.junit.feature.group"

echo "Unzipping features..."

cd /development/ant_build/artifacts/

unzip codewind-*.zip -d code
chmod -R 777 code

unzip codewind_test-*.zip -d test
chmod -R 777 test

cd /home

echo "Installing codewind and codewind test..."

eclipse/eclipse -nosplash -application org.eclipse.equinox.p2.director -repository "file:/development/ant_build/artifacts/code" -installIU "org.eclipse.codewind.feature.group"
eclipse/eclipse -nosplash -application org.eclipse.equinox.p2.director -repository "file:/development/ant_build/artifacts/test" -installIU "org.eclipse.codewind.test.feature.feature.group"

echo "Run junit tests"

xvfb-run ./runTest.sh

return_code=$?

echo "Test result file is: /development/junit-results.xml"
ls -l /development

echo "Test finished with return code $return_code"
return $return_code


