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
    wget \
    java-1.8.0-openjdk-devel-${JAVA_VERSION} \
    xrootd-client \
    xrootd-client-libs \
    xrootd-client-devel \
    yum clean all && rm -rf /var/cache/yum/*

RUN wget http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -O /etc/yum.repos.d/epel-apache-maven.repo && \
    sed -i s/\$releasever/6/g /etc/yum.repos.d/epel-apache-maven.repo && \
    yum install -y apache-maven

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

ARG CONNECTOR_RELEASE_NAME
ARG ARCHITECTURE_PROFILE

RUN mvn clean package && \
    mv target/${CONNECTOR_RELEASE_NAME}-${ARCHITECTURE_PROFILE}.nar /usr/lib/hadoop/share/hadoop/common/lib/ && \
    mv target/${CONNECTOR_RELEASE_NAME}.jar /usr/lib/hadoop/share/hadoop/common/lib/ && \
    curl http://central.maven.org/maven2/org/scijava/native-lib-loader/2.2.0/native-lib-loader-2.2.0.jar -o /usr/lib/hadoop/share/hadoop/common/lib/native-lib-loader-2.2.0.jar

ENV HADOOP_CLASSPATH="/usr/lib/hadoop/share/hadoop/common/lib/${CONNECTOR_RELEASE_NAME}-${ARCHITECTURE_PROFILE}.nar:$(hadoop classpath)"

CMD echo "Running integration tests" && make test

LABEL \
  org.label-schema.version="0.1" \
  org.label-schema.build-date=${BUILD_DATE} \
  org.label-schema.name="Hadoop-XRootD-Connector Dockerfile" \
  org.label-schema.vendor="CERN" \
  org.label-schema.schema-version="1.2"
