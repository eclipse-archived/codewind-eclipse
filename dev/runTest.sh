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

# Running Test

cd $CODE_TESTS_WORKSPACE

export TEST_CLASS_NAME=org.eclipse.codewind.test.AllTests
export TEST_PLUGIN_NAME=org.eclipse.codewind.test
export ECLIPSE_PATH=$CODE_TESTS_WORKSPACE
export LAUNCHER_LIBRARY=`ls eclipse/plugins/org.eclipse.equinox.launcher.gtk.linux.x86_64*/*.so`

export LAUNCHER_JAR=`ls eclipse/plugins/org.eclipse.equinox.launcher_*.jar`

export APPLICATION_TYPE=org.eclipse.swtbot.eclipse.junit.headless.swtbottestapplication

export INSECURE_KEYRING=true

export ADDITIONAL_ENV_VARS="-Dorg.eclipse.tips.startup.default=disable $ADDITIONAL_ENV_VARS"
if [ "$ENV_VAR_PROPERTIES" != ""  ]; then
	echo \* The ENV_VAR_PROPERTIES value is $ENV_VAR_PROPERTIES
	export ADDITIONAL_ENV_VARS="$ENV_VAR_PROPERTIES $ADDITIONAL_ENV_VARS"
fi
export ADDITIONAL_ENV_VARS="-Duser.timezone=America/Toronto  $ADDITIONAL_ENV_VARS"

echo TEST CLASS NAME is $TEST_CLASS_NAME
echo TEST PLUG-IN NAME is $TEST_PLUGIN_NAME
echo APPLICATION_TYPE is $APPLICATION_TYPE
echo Additional Environment Variables Added: $ADDITIONAL_ENV_VARS
echo
echo
echo Java version is:
java -version
echo
echo
echo
echo
export ECLIPSE_RUN_CMDLINE="java  \
-Xms256m  \
-Xmx1024m  \
$ADDITIONAL_ENV_VARS \
-Dorg.eclipse.swtbot.keyboard.layout=EN_US \
-Xmx1024m  \
-jar $ECLIPSE_PATH/$LAUNCHER_JAR  \
-os linux  \
-ws gtk  \
-arch x86_64  \
-launcher $ECLIPSE_PATH/eclipse/eclipse  \
-name Eclipse  \
--launcher.library $DIR/$LAUNCHER_LIBRARY  \
-startup $ECLIPSE_PATH/$LAUNCHER_JAR  \
--launcher.appendVmargs  \
-product org.eclipse.epp.package.jee.product  \
-application $APPLICATION_TYPE  \
-data /tmp/workspace-location  \
-testPluginName $TEST_PLUGIN_NAME  \
-className $TEST_CLASS_NAME formatter=org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter,$SCRIPT_DIR/junit-results.xml  \
-vmargs  \
-Xms256m  \
-Xmx1024m  \
-Dorg.eclipse.swt.internal.gtk.disablePrinting \
$ADDITIONAL_ENV_VARS \
-Dorg.eclipse.swtbot.keyboard.layout=EN_US \
-Xmx1024m  \
-jar $DIR/$LAUNCHER_JAR"

echo "Eclipse launch command is: $ECLIPSE_RUN_CMDLINE"
echo
echo
echo "* Test has started"

$ECLIPSE_RUN_CMDLINE
