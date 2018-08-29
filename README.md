# CERN IT  Hadoop-XRootD-Connector

Connector between Hadoop and XRootD protocols (EOS compatible) 

### Available hadoop flags

**WARNING** - diana-hep/root4j <=0.1.6 package resets configs spark.hadoop on executors!! 
Must be specified in HADOOP_CONF_DIR in core-site.xml - ref https://github.com/diana-hep/root4j/issues/3

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

### Build XRootD-Connector with MVN

Prerequisites:

```
xrootd-client, xrootd-client-libs, xrootd-client-devel
```

Use "mvn" command to package
```
mvn package
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
mvn package
```

### Testing
#### Test with HDFS

```bash
# Get native-lib-loader as mvn resolver will do
$ curl http://central.maven.org/maven2/org/scijava/native-lib-loader/2.2.0/native-lib-loader-2.2.0.jar -o target/native-lib-loader-2.2.0.jar
 
# Add to Hadoop Classpath (Spark Driver or Executor extra classpath - spark.driver.extraClassPath)
$ export HADOOP_CLASSPATH="target/${CONNECTOR_RELEASE_NAME}-${ARCHITECTURE_PROFILE}.nar:target/${CONNECTOR_RELEASE_NAME}.jar:target/native-lib-loader-2.2.0.jar:$(hadoop classpath)"
 
# Try to read a file
$ hdfs dfs -ls root://eospublic.cern.ch//eos/opendata/cms/MonteCarlo2012/Summer12_DR53X/DYJetsToLL_M-50_TuneZ2Star_8TeV-madgraph-tarball/AODSIM/PU_RD1_START53_V7N-v1/20000/DCF94DC3-42CE-E211-867A-001E67398011.root
```

#### Test with integration tests

This will build the Docker image and run integration tests. 

```
./run-tests.sh
```

