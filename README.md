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

### Testing
#### Test with Hadoop

```
# Add to Hadoop Classpath (Spark Driver or Executor extra classpath - spark.driver.extraClassPath)
$ VERSION=$(mvn help:evaluate -Dexpression=project.version $@ 2>/dev/null\
| grep -v "INFO"\
| grep -v "WARNING"\
| tail -n 1)
$ export HADOOP_CLASSPATH="$(pwd)/hadoop-xrootd-${VERSION}-jar-with-dependencies.jar:$(hadoop classpath)"
 
# Try to read a file
$ hdfs dfs -ls root://eospublic.cern.ch//eos/opendata/cms/MonteCarlo2012/Summer12_DR53X/DYJetsToLL_M-50_TuneZ2Star_8TeV-madgraph-tarball/AODSIM/PU_RD1_START53_V7N-v1/20000/DCF94DC3-42CE-E211-867A-001E67398011.root
```

#### Test with integration tests

This will build the Docker image and run integration tests. 

```
# Build docker
$ docker build \
--build-arg BUILD_DATE=$(date -u +"%Y-%m-%dT%H:%M:%SZ") \
-t hadoop-xrootd-connector $(pwd)
 
# Run tests
$ docker run --rm -it hadoop-xrootd-connector
```

