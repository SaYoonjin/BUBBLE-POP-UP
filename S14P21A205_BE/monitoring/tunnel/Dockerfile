FROM alpine:3.20

RUN apk add --no-cache autossh openssh-client

CMD ["sh", "-c", "autossh -M 0 -N -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -i /run/secrets/exporter_tunnel_id_rsa -L 0.0.0.0:${LOCAL_PORT}:${REMOTE_HOST}:${REMOTE_PORT} ${SSH_USER}@${SSH_HOST}"]
