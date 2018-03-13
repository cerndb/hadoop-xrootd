FROM gitlab-registry.cern.ch/linuxsupport/cc7-base
MAINTAINER Piotr Mrowczynski <piotr.mrowczynski@cern.ch>

# Get the dependencies for building xrootd-connector
ENV JAVA_VERSION=1.8.0.181-3.b13.el7_5.x86_64
ENV JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-${JAVA_VERSION}
ENV PATH $PATH:${JAVA_HOME}/bin
RUN yum group install -y \
    "Development Tools" && \
    yum install -y \
    which \
    java-1.8.0-openjdk-devel-${JAVA_VERSION} \
    xrootd-client \
    xrootd-client-libs \
    xrootd-client-devel \
    yum clean all && rm -rf /var/cache/yum/*

# Install hadoop - required to build xrootd-connector
ENV HADOOP_VERSION=2.7.4
ENV HADOOP_URL=https://syscontrol.cern.ch/rpms/hdp/hadoop/soft7/hadoop-${HADOOP_VERSION}.tar.gz
ENV PATH $PATH:/usr/lib/hadoop/bin
ENV PATH $PATH:/usr/lib/hadoop/sbin
RUN curl -s ${HADOOP_URL} | tar -xzvf - -C /usr/lib/ && \
    cd /usr/lib && ln -s ./hadoop-${HADOOP_VERSION} hadoop

# Build connector
ARG BUILD_DATE
COPY . /data
WORKDIR /data
RUN make clean 2>/dev/null && \
    make all && \
    mv /data/EOSfs.jar /usr/lib/hadoop/share/hadoop/common/lib/EOSfs.jar && \
    mv /data/libjXrdCl.so /usr/lib/hadoop/lib/native/libjXrdCl.so && \
    make clean 2>/dev/null

CMD echo "Running integration tests" && make test

LABEL \
  org.label-schema.version="0.1" \
  org.label-schema.build-date=${BUILD_DATE} \
  org.label-schema.name="Hadoop-XRootD-Connector Dockerfile" \
  org.label-schema.vendor="CERN" \
  org.label-schema.schema-version="1.2"