FROM repo.dev.optio3.io:5000/optio3-jre:v11.0.6_10

ENV OPTIO3_TIMEZONE PST
ENV OPTIO3_MAX_MEM 600

# Copy external dependencies first, hoping they haven't changed.
COPY target/externalDependency/*.jar /app/

# Copy configuration second, since it doesn't change frequently.
COPY waypoint.yml docker-launch.sh /app/

# Copy internal dependencies last, which should generate a minimal delta.
COPY target/internalDependency/*.jar /app/
COPY target/*.jar /app/

WORKDIR /app

ENTRYPOINT ["sh", "/app/docker-launch.sh"]
