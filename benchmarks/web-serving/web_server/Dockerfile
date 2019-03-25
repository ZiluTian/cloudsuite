FROM ubuntu:18.04
LABEL maintainer="Mark Sutherland <mark.sutherland@epfl.ch>"

USER root

ENV DEBIAN_FRONTEND=noninteractive 
ENV TZ=Europe/Zurich
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN	apt-get update && \
	apt-get install -y --force-yes software-properties-common && \
	apt-key adv --recv-keys --keyserver hkp://keyserver.ubuntu.com:80 0xb4112585d386eb94 && \
	add-apt-repository "deb http://dl.hhvm.com/ubuntu $(lsb_release -sc) main" && \ 
	apt-get update && \
	apt-get install -y \
	build-essential \
	git \
	nginx \
	php7.2 php7.2-gd \
	php7.2-mysql php7.2-curl \
	php7.2-fpm php-memcache \
	hhvm

# Increase the open file limit
COPY files/limits.conf.append /tmp/
RUN cat /tmp/limits.conf.append >> /etc/security/limits.conf && rm -f /tmp/limits.conf.append

# Checkout the Elgg installation
COPY files/elgg_installation /usr/share/nginx/html/elgg

WORKDIR /usr/share/nginx/html
# Copy over the settings.php
COPY files/settings.php elgg/engine/settings.php

# Make the Elgg data directory
RUN mkdir /elgg_data
RUN chmod a+rw /elgg_data

# Copy over the Nginx Server configuration
COPY files/nginx_sites_avail.append /tmp/

# Copy over the Nginx HHVM Server configuration
COPY files/nginx_sites_avail_hhvm.append /tmp/
COPY files/configure_hhvm.sh /tmp/

RUN service nginx restart

RUN service php7.2-fpm start

ADD bootstrap.sh /etc/bootstrap.sh
RUN chown root:root /etc/bootstrap.sh
RUN chmod 700 /etc/bootstrap.sh

EXPOSE 8080

CMD ["/etc/bootstrap.sh", "-d"]
