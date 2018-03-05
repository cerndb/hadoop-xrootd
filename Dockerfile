FROM gitlab-registry.cern.ch/linuxsupport/cc7-base:latest

MAINTAINER Piotr Mrowczynski <piotr.mrowczynski@cern.ch>

ARG HADOOP_VERSION=2.7.4
ARG HADOOP_URL=https://syscontrol.cern.ch/rpms/hdp/hadoop/soft7/hadoop-$HADOOP_VERSION.tar.gz

ENV JAVA_HOME /usr/lib/jvm/java-1.8.0-openjdk-1.8.0.161-0.b14.el7_4.x86_64
ENV PATH $PATH:/usr/lib/hadoop/bin
ENV PATH $PATH:/usr/lib/hadoop/sbin
ENV PATH $PATH:$JAVA_HOME/bin
ENV HADOOP_PREFIX /usr/lib/hadoop

RUN yum update -y && \
    # Install base tools
    yum install -y \
    tar \
    which \
    sudo \
    wget \
    curl \
    # Install java jdk
    java-1.8.0-openjdk-devel \
    # required dependencies for executor to communicate with EOS via xrootd
    xrootd-client \
    xrootd-client-libs \
    xrootd-client-devel && \
    # Install hadoop
    curl -s $HADOOP_URL | tar -xzvf - -C /usr/lib/ && \
    cd /usr/lib && ln -s ./hadoop-$HADOOP_VERSION hadoop && \
    # Build tools for xrootd-connector, e.g. gcc-c++-4.8.5-16.el7_4.1.x86_64
    yum group install -y "Development Tools" && \
    # cleanup
    yum clean all && rm -rf /var/cache/yum/*

WORKDIR /data

# build the connector on docker run
CMD make clean && \
    make all && \
    cp /data/EOSfs.jar /usr/lib/hadoop-2.7.4/share/hadoop/common/lib/EOSfs.jar && \
    cp /data/libjXrdCl.so /usr/lib/hadoop/lib/native/libjXrdCl.so && \
    echo '' && \
    echo '* EOSfs.jar: /data/EOSfs.jar /usr/lib/hadoop-2.7.4/share/hadoop/common/lib/EOSfs.jar *' && \
    echo '* libjXrdCl.so: /data/libjXrdCl.so /usr/lib/hadoop/lib/native/libjXrdCl.so *' && \
    echo '' && \
    echo '* To test: *' && \
    echo '* export EOS_debug=1 *' && \
    echo '* kinit <your-username> *' && \
    echo '* hdfs dfs -ls root://eosuser.cern.ch/ *' && \
    echo '* hdfs dfs -get root://eospublic.cern.ch/eos/opendata/cms/MonteCarlo2012/Summer12_DR53X/DYJetsToLL_M-50_TuneZ2Star_8TeV-madgraph-tarball/AODSIM/PU_RD1_START53_V7N-v1/20000/DCF94DC3-42CE-E211-867A-001E67398011.root /tmp/ *' && \
    bash


LABEL \
  org.label-schema.version="0.1" \
  org.label-schema.name="Hadoop-XRootD-Connector Dockerfile" \
  org.label-schema.vendor="CERN" \
  org.label-schema.schema-version="1.0"
