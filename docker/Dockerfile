FROM gitlab-registry.cern.ch/linuxsupport/cc7-base
MAINTAINER Piotr Mrowczynski <piotr.mrowczynski@cern.ch>
MAINTAINER Zbigniew Baranowski <zbigniew.baranowski@cern.ch>

ARG SPARK_VERSION=2.4
ARG SPARK_HADOOP_VERSION=2.7.5

ENV JAVA_HOME /usr/lib/jvm/java-1.8.0-oracle.x86_64

ENV SPARK_HOME /usr/hdp/spark
ENV PATH $PATH:$SPARK_HOME/bin
ENV PATH $PATH:$SPARK_HOME/sbin

ENV HADOOP_HOME /usr/hdp/hadoop
ENV PATH $PATH:$HADOOP_HOME/bin
ENV PATH $PATH:$HADOOP_HOME/sbin

COPY Cern-Hadoop-Components.repo /etc/yum.repos.d/Cern-Hadoop-Components.repo

# Get the dependencies for building xrootd-connector
RUN yum group install -y \
    "Development Tools" && \
    yum install -y \
    which \
    wget \
    xrootd-client \
    xrootd-client-libs \
    xrootd-client-devel \
    java-1.8.0-oracle-devel \
    hadoop-bin-$SPARK_HADOOP_VERSION \
    spark-bin-$SPARK_VERSION \
    yum clean all && rm -rf /var/cache/yum/*

RUN wget http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -O /etc/yum.repos.d/epel-apache-maven.repo && \
    sed -i s/\$releasever/6/g /etc/yum.repos.d/epel-apache-maven.repo && \
    yum install -y apache-maven

COPY log4j.properties /root/log4j.properties

WORKDIR /build

LABEL \
  org.label-schema.version="0.1" \
  org.label-schema.name="Hadoop-XRootD Dockerfile for builds" \
  org.label-schema.vendor="CERN" \
  org.label-schema.schema-version="1.2"
