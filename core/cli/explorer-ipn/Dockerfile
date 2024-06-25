FROM repo.dev.optio3.io:5000/optio3-jre:v11.0.6_10

RUN mkdir /explorer

# Copy external dependencies first, hoping they haven't changed.
COPY target/externalDependency/*.jar /explorer/

# Copy internal dependencies last, which should generate a minimal delta.
COPY target/internalDependency/*.jar /explorer/
COPY target/*.jar /explorer/

WORKDIR /explorer

ENTRYPOINT java -cp "*:/opt/oracle-server-jre/lib/tools.jar" com.optio3.protocol.ipn.explorer.ObdiiExplorer $*
