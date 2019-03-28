FROM ubuntu:18.04
LABEL maintainer="Mark Sutherland <mark.sutherland@epfl.ch>"

ENV PHP_VERSION 7.2
ENV ELGG_VERSION 2.3.8
ENV DEBIAN_FRONTEND noninteractive

RUN apt-get update && apt-get install -y \
	build-essential \
	git \
	nginx \
	wget \
	unzip \
	mysql-client \
	php$PHP_VERSION php$PHP_VERSION-gd \
	php$PHP_VERSION-mysql php$PHP_VERSION-curl \
	php$PHP_VERSION-common \
	php$PHP_VERSION-sqlite3 \
	php$PHP_VERSION-intl \
	php$PHP_VERSION-mbstring \
	php$PHP_VERSION-xmlrpc \
	php$PHP_VERSION-xml \
	php$PHP_VERSION-cli \
	php$PHP_VERSION-zip \
	php$PHP_VERSION-fpm php$PHP_VERSION-memcache
	
# Increase the open file limit
COPY files/limits.conf.append /tmp/
RUN cat /tmp/limits.conf.append >> /etc/security/limits.conf && rm -f /tmp/limits.conf.append

# Checkout the Elgg installation
RUN wget https://elgg.org/download/elgg-$ELGG_VERSION.zip \
    && unzip elgg-$ELGG_VERSION.zip \
    && mv elgg-$ELGG_VERSION /usr/share/nginx/html/elgg \
    && chown -R www-data /usr/share/nginx/html/elgg

WORKDIR /usr/share/nginx/html

# Copy over the settings.php
COPY files/settings.php elgg/elgg-config/settings.php

# Make the Elgg data directory
RUN mkdir /elgg_data
RUN chmod a+rw /elgg_data

# Copy over the Nginx Server configuration
COPY files/nginx_sites_avail.append /tmp/
RUN cat /tmp/nginx_sites_avail.append >> /etc/nginx/sites-available/default

RUN service nginx restart

RUN mkdir -p /var/run/php
RUN service php$PHP_VERSION-fpm restart

ADD bootstrap.sh /etc/bootstrap.sh
RUN chown root:root /etc/bootstrap.sh
RUN chmod 700 /etc/bootstrap.sh

EXPOSE 8080

CMD ["/etc/bootstrap.sh", "-d"]
