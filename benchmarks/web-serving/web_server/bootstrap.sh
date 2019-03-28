#!/bin/bash

DB_SERVER_IP=${1:-"mysql_server"}
MEMCACHE_SERVER_IP=${2:-"memcache_server"}

sed -i -e"s/mysql_server/${DB_SERVER_IP}/" elgg/elgg-config/settings.php
sed -i -e"s/'memcache_server'/'${MEMCACHE_SERVER_IP}'/" elgg/elgg-config/settings.php


if [[ ! -z "${HHVM}" && "${HHVM}" = "true" ]]; then
	chmod 700 /tmp/configure_hhvm.sh
	/tmp/configure_hhvm.sh
else
	FPM_CHILDREN=${3:-80}
	sed -i -e"s/pm.max_children = 5/pm.max_children = ${FPM_CHILDREN}/" /etc/php/$PHP_VERSION/fpm/pool.d/www.conf
	service php$PHP_VERSION-fpm restart
fi

service nginx restart
bash
