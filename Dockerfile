FROM gitlab-registry.cern.ch/linuxsupport/cc7-base:latest

MAINTAINER Piotr Mrowczynski <piotr.mrowczynski@cern.ch>

ARG HADOOP_VERSION=2.7.5
ARG HADOOP_URL=http://mirror.switch.ch/mirror/apache/dist/hadoop/common/hadoop-${HADOOP_VERSION}/hadoop-${HADOOP_VERSION}.tar.gz
ARG SPARK_VERSION=2.2.0
ARG SPARK_URL=http://mirror.switch.ch/mirror/apache/dist/spark/spark-${SPARK_VERSION}/spark-${SPARK_VERSION}-bin-without-hadoop.tgz

ENV JAVA_HOME /usr/lib/jvm/java-1.8.0-openjdk-1.8.0.161-0.b14.el7_4.x86_64
ENV PATH $PATH:/usr/lib/hadoop/bin
ENV PATH $PATH:/usr/lib/hadoop/sbin
ENV PATH $PATH:${JAVA_HOME}/bin
ENV HADOOP_PREFIX /usr/lib/hadoop
ENV SPARK_HOME /usr/lib/spark
ENV PYTHON_DIR_PATH ${SPARK_HOME}/python/
ENV SPARK_DIST_CLASSPATH /usr/lib/hadoop/etc/hadoop:/usr/lib/hadoop/share/hadoop/common/lib/*:/usr/lib/hadoop/share/hadoop/common/*:/usr/lib/hadoop/share/hadoop/hdfs:/usr/lib/hadoop/share/hadoop/hdfs/lib/*:/usr/lib/hadoop/share/hadoop/hdfs/*:/usr/lib/hadoop/share/hadoop/yarn/lib/*:/usr/lib/hadoop/share/hadoop/yarn/*:/usr/lib/hadoop/share/hadoop/mapreduce/lib/*:/usr/lib/hadoop/share/hadoop/mapreduce/*:/contrib/capacity-scheduler/*.jar

# Get the dependencies for building xrootd-connector
RUN yum update -y && \
    # Install base tools
    yum install -y \
    tar \
    which \
    sudo \
    wget \
    curl \
    python-devel \
    python-pip \
    # Install java jdk
    java-1.8.0-openjdk-devel \
    # required dependencies for executor to communicate with EOS via xrootd
    xrootd-client \
    xrootd-client-libs \
    xrootd-client-devel && \
    # Build tools for xrootd-connector, e.g. gcc-c++-4.8.5-16.el7_4.1.x86_64
    yum group install -y "Development Tools" && \
    # cleanup
    yum clean all && rm -rf /var/cache/yum/*

# Install hadoop - required to build xrootd-connector
RUN curl -s ${HADOOP_URL} | tar -xzvf - -C /usr/lib/ && \
    cd /usr/lib && ln -s ./hadoop-${HADOOP_VERSION} hadoop

# Install spark - required to run pyspark
RUN curl -s ${SPARK_URL} | tar -xzvf - -C /usr/lib/ && \
    cd /usr/lib && ln -s ./spark-${SPARK_VERSION}-bin-without-hadoop spark

COPY . /data

WORKDIR /data

# build the connector on docker run
RUN make clean 2>/dev/null && \
    make all && \
    mv /data/EOSfs.jar /usr/lib/hadoop/share/hadoop/common/lib/EOSfs.jar && \
    mv /data/libjXrdCl.so /usr/lib/hadoop/lib/native/libjXrdCl.so && \
    make clean 2>/dev/null

# bash on docker run
CMD echo '' && echo '** Run Spark-Shell with any required packages **' && echo '/usr/lib/spark/bin/pyspark --packages org.diana-hep:spark-root_2.11:0.1.15' && bash

LABEL \
  org.label-schema.version="0.1" \
  org.label-schema.name="Hadoop-XRootD-Connector Dockerfile" \
  org.label-schema.vendor="CERN" \
  org.label-schema.schema-version="1.2"