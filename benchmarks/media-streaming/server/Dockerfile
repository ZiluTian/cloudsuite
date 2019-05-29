FROM zilutian/nginx

MAINTAINER Alexandros Daglis <alexandros.daglis@epfl.ch>

# Increase the open file limit
COPY files/limits.conf.append /tmp/
RUN cat /tmp/limits.conf.append >> /etc/security/limits.conf

# Update nginx to serve /videos
RUN sed -i 's|/var/www/html|/videos|g' /etc/nginx/sites-available/default

CMD ["nginx", "-g", "daemon off;"]
