FROM gitlab-registry.cern.ch/db/spark-service/docker-registry/spark:v2.3.0-hadoop2.7

MAINTAINER Piotr Mrowczynski <piotr.mrowczynski@cern.ch>

# Get the dependencies for building xrootd-connector
RUN yum group install -y "Development Tools" && \
    # cleanup
    yum clean all && rm -rf /var/cache/yum/*

COPY . /data

WORKDIR /data

# build the connector on docker run
RUN make clean 2>/dev/null && \
    make all && \
    cp /data/EOSfs.jar ${HADOOP_HOME}/share/hadoop/common/lib/EOSfs.jar && \
    cp /data/EOSfs.jar ${SPARK_HOME}/jars/EOSfs.jar && \
    cp /data/libjXrdCl.so ${HADOOP_HOME}/lib/native/libjXrdCl.so && \
    make clean 2>/dev/null

# bash on docker run
ENTRYPOINT echo '' && echo '** Run Spark-Shell with any required packages **' && echo 'spark-shell --packages org.diana-hep:spark-root_2.11:0.1.15' && bash

LABEL \
  org.label-schema.version="0.1" \
  org.label-schema.name="Hadoop-XRootD-Connector Dockerfile" \
  org.label-schema.vendor="CERN" \
  org.label-schema.schema-version="1.2"