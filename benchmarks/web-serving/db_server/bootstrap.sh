#!/bin/bash

if [ $# -lt 1 ]; then
  echo "Web server IP is a mandatory parameter."
  exit 1
fi

WEB_SERVER_IP=$1

# Update the hostname/IP to that of the webserver
chmod a+x /execute.sh
sync
bash -c "/execute.sh root" #root is the root pass
