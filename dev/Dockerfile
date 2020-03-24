################################################################################
# Copyright (c) 2020 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v2.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v20.html
#
# Contributors:
#     IBM Corporation - initial API and implementation
################################################################################

FROM sgrio/java:jre_8_ubuntu 
WORKDIR /home
RUN apt-get update
RUN apt-get install -y net-tools
RUN apt-get install -y telnet
RUN apt-get install -y wget xvfb unzip libgtk-3-dev
# Set locale
RUN apt-get clean && apt-get update && apt-get install -y locales
RUN locale-gen en_US.UTF-8
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8
# Install dbus (needed by SWTBot)
RUN apt-get clean && apt-get update && apt-get install -y dbus && apt-get install -y dbus-x11
# Install Eclipse
RUN wget http://www.eclipse.org/external/technology/epp/downloads/release/2019-12/R/eclipse-jee-2019-12-R-linux-gtk-x86_64.tar.gz
RUN tar xzf eclipse-jee-2019-12-R-linux-gtk-x86_64.tar.gz
# Copy the test scripts
ADD run.sh /home
ADD runTest.sh /home
RUN chmod 755 /home/run.sh
RUN chmod 755 /home/runTest.sh
# Codewind CLI needs docker and docker-compose
RUN curl -fsSL https://get.docker.com -o get-docker.sh
RUN sh get-docker.sh
RUN curl -fsSL "https://github.com/docker/compose/releases/download/1.25.4/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
RUN chmod 755 /usr/local/bin/docker-compose

CMD ./run.sh
