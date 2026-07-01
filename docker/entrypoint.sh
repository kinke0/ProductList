#!/usr/bin/env bash
set -euo pipefail

JAVA_OPTS_VALUE=${JAVA_OPTS:-}
export SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-prod}

java ${JAVA_OPTS_VALUE} -jar /app/app.jar &
java_pid=$!

trap 'kill ${java_pid} ${nginx_pid:-} 2>/dev/null || true' INT TERM

nginx -g 'daemon off;' &
nginx_pid=$!

wait -n "${java_pid}" "${nginx_pid}"
status=$?

kill "${java_pid}" "${nginx_pid}" 2>/dev/null || true
wait "${java_pid}" 2>/dev/null || true
wait "${nginx_pid}" 2>/dev/null || true

exit ${status}