# ============================================
# 运行时镜像 - 仅包含运行环境，不含业务代码
# jar / dist / db / uploads 全部通过卷挂载注入
# ============================================
FROM --platform=linux/amd64 eclipse-temurin:17-jre
ENV DEBIAN_FRONTEND=noninteractive

RUN sed -i 's/deb.debian.org/mirrors.tuna.tsinghua.edu.cn/g' /etc/apt/sources.list.d/debian.sources 2>/dev/null || true && \
    apt-get update \
    && apt-get install -y --no-install-recommends bash ca-certificates curl nginx \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# nginx 配置
RUN rm -f /etc/nginx/sites-enabled/default /etc/nginx/sites-available/default /etc/nginx/conf.d/default.conf
COPY docker/proethos2 /etc/nginx/sites-available/proethos2
RUN ln -s /etc/nginx/sites-available/proethos2 /etc/nginx/sites-enabled/default

# entrypoint
COPY docker/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh \
    && mkdir -p /var/cache/nginx /var/run /app/uploads /app/generated-docs

# 以下通过运行时卷挂载注入:
#   /app/app.jar             <- host: $REMOTE_DIR/app.jar
#   /usr/share/nginx/html    <- host: $REMOTE_DIR/dist/
#   /app/uploads             <- host: $REMOTE_DIR/data/uploads/
#   /app/generated-docs      <- host: $REMOTE_DIR/data/docs/

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS=""

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 CMD curl -fsS http://127.0.0.1/ || exit 1
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
