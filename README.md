# Hadoop-XRootD-Connector

Connector between Hadoop and XRootD protocols (EOS compatible) 

[![Build Status](https://gitlab.cern.ch/db/hadoop-xrootd/badges/master/build.svg)](https://gitlab.cern.ch/db/hadoop-xrootd)

### Available spark/hadoop flags

For Hadoop config

```
    <?xml version="1.0" encoding="UTF-8"?>
    <?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
   
    <configuration>
        <property>
            <description>set the size of the request to XRootD storage issued for data read(allows prefetching more data in case of large reads). Defaults to 128KB.</description>
            <name>fs.xrootd.read.buffer</name>
            <value>128000</value>
        </property>
        <property>
            <description>set the size of the request to XRootD storage issued for data write (allows buffering writes into larger request). Defaults to 1MB.</description>
            <name>fs.xrootd.write.buffer</name>
            <value>1048576</value>
        </property>
    </configuration>
```

For Spark config 

```
spark.hadoop.fs.xrootd.read.buffer=128000
spark.hadoop.fs.xrootd.write.buffer=1048576
```

**WARNING** - diana-hep/root4j <=0.1.6 package resets configs spark.hadoop on executors!! 
Must be specified in HADOOP_CONF_DIR in core-site.xml - ref https://github.com/diana-hep/root4j/issues/3

### Build XRootD-Connector with MVN

Prerequisites:

```
- xrootd-client, xrootd-client-libs, xrootd-client-devel (tested with 4.8.4>)
- maven
```

Use "make package" to package
```
make package
```

NOTES:

Since hadoop-xrootd-connector relies on `.so` dependency for C++ `xrootd-client`, currently 
it requires to build connector with correct platform `linux` and `gcc` version to avoid error below:

```
java: symbol lookup error: /tmp/libhadoop-xrootd-1.0.0-SNAPSHOT9131106165051975528.so: undefined symbol: _ZN5XrdCl3URLC1ERKSs
```

Build with correct versions e.g. on `lxplus-cloud`:

```
source /cvmfs/sft.cern.ch/lcg/views/LCG_93/x86_64-slc6-gcc62-opt/setup.sh
mvn clean package -X -Dxrootd.lib64.path=${XROOTD_LIB64_SO_FILES_PATH} -Dxrootd.include.path=${XROOTD_INCLUDE_HH_FILES_PATH}
```

CI generates jars for using the connector with CVMFS sourced software. The jars including the dependencies are published at `s3://binaries/hadoop-xrootd`.

### Testing

[Docker image](docker/Dockerfile) is used as base for [Gitlab CI](.gitlab-ci.yml) pipeline.
To test manualy, build the docker image and run in interactive mode

```
# Build docker
$ docker build \
-t hadoop-xrootd-connector ./docker
 
# Run tests by mounting root directory
$ docker run --rm -it -v $(pwd):/build hadoop-xrootd-connector bash
```

Compile and package

```
$ make package
```

Test with HDFS cli

``` 
# Optionaly enable java debug
export HADOOP_ROOT_LOGGER=hadoop.root.logger=DEBUG,console
  
# Optionaly enable debug mode for XROOTD Client C++ library
$ export Xrd_debug=1
 
# Set hadoop classpath
$ export HADOOP_CLASSPATH="$(pwd)/*:$(hadoop classpath)"
  
# Try to check if some publicly available file exists
$ hdfs dfs -ls root://eospublic.cern.ch/eos/opendata/cms/MonteCarlo2012/Summer12_DR53X/DYJetsToLL_M-50_TuneZ2Star_8TeV-madgraph-tarball/AODSIM/PU_RD1_START53_V7N-v1/file-indexes/CMS_MonteCarlo2012_Summer12_DR53X_DYJetsToLL_M-50_TuneZ2Star_8TeV-madgraph-tarball_AODSIM_PU_RD1_START53_V7N-v1_20002_file_index.txt
```

Test by packaging the project, setting classpath and executing tests

```
# Execute tests
$ kinit <username>@CERN.CH
$ make tests
```

Test with Spark (add log4j conf for debug)

```
# Set spark hadoop classpath
$ export SPARK_DIST_CLASSPATH="$(pwd)/*:$(hadoop classpath)"
 
# Try to read file with spark
$ pyspark \
--master local[*] \
--conf "spark.driver.extraJavaOptions=-Dlog4j.configuration=file:/root/log4j.properties" \
--conf "spark.executor.extraJavaOptions=-Dlog4j.configuration=file:/root/log4j.properties"
 
# Run some commands in shell
input = sc.binaryFiles('root://eospublic.cern.ch/eos/opendata/cms/MonteCarlo2012/Summer12_DR53X/DYJetsToLL_M-50_TuneZ2Star_8TeV-madgraph-tarball/AODSIM/PU_RD1_START53_V7N-v1/file-indexes/CMS_MonteCarlo2012_Summer12_DR53X_DYJetsToLL_M-50_TuneZ2Star_8TeV-madgraph-tarball_AODSIM_PU_RD1_START53_V7N-v1_20002_file_index.txt')
input.map(lambda x: x[0]).collect()
```