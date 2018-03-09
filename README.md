# CERN IT  Hadoop-XRootD-Connector

Connector between Hadoop and XRootD protocols (EOS compatible) 

### Build and run PySpark Shell with XRootD-Connector

Prerequisites:  

- Docker Installation https://docs.docker.com/install/

- Docker as non-root user https://docs.docker.com/install/linux/linux-postinstall/

This will build the environment image and run bash shell. While in bash, run pyspark with any required packages or any other command

```
./run-demo.sh
```

while in bash shell inside the docker, you can run spark shell with connector preinstalled
```
/usr/lib/spark/bin/pyspark --packages org.diana-hep:spark-root_2.11:0.1.15

>>> input = "root://eospublic.cern.ch//eos/opendata/cms/MonteCarlo2012/Summer12_DR53X/DYJetsToLL_M-50_TuneZ2Star_8TeV-madgraph-tarball/AODSIM/PU_RD1_START53_V7N-v1/20000/DCF94DC3-42CE-E211-867A-001E67398011.root"
>>> df = sqlContext.read.format("org.dianahep.sparkroot").option("tree", "Events").load(input)
>>> df.count()
```

NOTE: First run is the longest, but next attempts will be instant

### Build and test XRootD-Connector in hadalytic

Prerequisites:

```
xrootd-client, xrootd-client-libs, xrootd-client-devel
```

Use "make all" command to compile
```
make all
```

Use "make test" command to run integration tests

```
cp EOSfs.jar /usr/lib/hadoop/share/hadoop/common/lib/EOSfs.jar
cp libjXrdCl.so /usr/lib/hadoop/lib/native/libjXrdCl.so
make test
```

### Build and test XRootD-Connector in docker on localhost

This will build the environment image and run integration tests. 

```
./run-docker-tests.sh
```

NOTE: First run is the longest, but next attempts will be instant

### Documentation: Recommended

NOTE: User inside docker is different then on parent host, thus one might need to
change ownership with `chown

NOTE: If you don't have connectivity inside the docker e.g. `ping www.google.com` 
please ensure that you edit `/etc/docker/daemon.json` with 
```
{ 
    "dns": ["<your-cern-dns>", "8.8.8.8"] 
}
```

### Documentation: XRootD-Connector prebuild environment

**Use XRootD-Connector Docker with your gcc, java version, and hadoop versions**

Build the image

```
docker build -t hadoop-xrootd-connector .
```

You can go to docker inside with bash
```
docker run --rm -it hadoop-xrootd-connector bash
```

You can then test your connector with e.g.

```
make clean
make all
cp /data/EOSfs.jar /usr/lib/hadoop-2.7.4/share/hadoop/common/lib/EOSfs.jar
cp /data/libjXrdCl.so /usr/lib/hadoop/lib/native/libjXrdCl.so
make clean
export EOS_debug=1
kinit <your-username>
hdfs dfs -ls root://eosuser.cern.ch/
hdfs dfs -get root://eospublic.cern.ch/eos/opendata/cms/MonteCarlo2012/Summer12_DR53X/DYJetsToLL_M-50_TuneZ2Star_8TeV-madgraph-tarball/AODSIM/PU_RD1_START53_V7N-v1/20000/DCF94DC3-42CE-E211-867A-001E67398011.root /tmp/
```
