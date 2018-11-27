# Hadoop-XRootD-Connector

Connector between Hadoop and XRootD protocols (EOS compatible) 

[![Build Status](https://gitlab.cern.ch/db/hadoop-xrootd/badges/master/build.svg)](https://gitlab.cern.ch/db/hadoop-xrootd)

### Available hadoop flags

```
    <?xml version="1.0" encoding="UTF-8"?>
    <?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
   
    <configuration>
        <property>
            <description>set the size of the request to XRootD storage issued for data (allows prefetching more data in case of large reads). Defaults to 128KB.</description>
            <name>fs.xrootd.readahead.range</name>
            <value>128000</value>
        </property>
    </configuration>
```

**WARNING** - diana-hep/root4j <=0.1.6 package resets configs spark.hadoop on executors!! 
Must be specified in HADOOP_CONF_DIR in core-site.xml - ref https://github.com/diana-hep/root4j/issues/3

### Build XRootD-Connector with MVN

Prerequisites:

```
- xrootd-client, xrootd-client-libs, xrootd-client-devel
- maven
```

Use "make all" to package
```
make all
```

NOTES:

Since hadoop-xrootd-connector relies on NAR packaging (`.so` dependency for C++ `xrootd-client`), currently 
it requires to build connector with correct platform `linux` and `gcc` version to avoid  error below:

```
java: symbol lookup error: /tmp/libhadoop-xrootd-1.0.0-SNAPSHOT9131106165051975528.so: undefined symbol: _ZN5XrdCl3URLC1ERKSs
```

Build with correct versions e.g. on `lxplus-cloud`:

```
source /cvmfs/sft.cern.ch/lcg/views/LCG_93/x86_64-slc6-gcc62-opt/setup.sh
make all
```

CI generates jars for using the connector with CVMFS sourced software. The jars including the dependencies are published at `s3://binaries/hadoop-xrootd`.

### Testing

[Docker image](Dockerfile) is used as base for [Gitlab CI](.gitlab-ci.yml) pipeline.
To test manualy, build the docker image and run in interactive mode

```
# Build docker
$ docker build \
-t hadoop-xrootd-connector $(pwd)
 
# Run tests by mounting root directory
$ docker run --rm -it -v $(pwd):/build hadoop-xrootd-connector bash
```

Test by packaging the project, setting classpath and executing tests 

```
# Package
$ make all
 
# Add to Hadoop Classpath (Spark Driver or Executor extra classpath - spark.driver.extraClassPath)
$ VERSION=$(mvn help:evaluate -Dexpression=project.version $@ 2>/dev/null\
| grep -v "INFO"\
| grep -v "WARNING"\
| tail -n 1)
$ export HADOOP_CLASSPATH="$(pwd)/hadoop-xrootd-${VERSION}-jar-with-dependencies.jar:$(hadoop classpath)"
  
# Try to check if some publicly available file exists
$ hdfs dfs -ls root://eospublic.cern.ch//eos/opendata/cms/MonteCarlo2012/Summer12_DR53X/DYJetsToLL_M-50_TuneZ2Star_8TeV-madgraph-tarball/AODSIM/PU_RD1_START53_V7N-v1/20000/DCF94DC3-42CE-E211-867A-001E67398011.root

# Optionaly, enable debug mode
$ export HADOOP_XROOTD_DEBUG=1

# Execute tests
$ kinit <username>@CERN.CH
$ make tests
```
