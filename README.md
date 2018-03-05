# CERN IT  Hadoop-XRootD-Connector

Connector between Hadoop and XRootD protocols (EOS compatible) 

### Run XRootD-Connector integration tests

This will build the environment image and run integration tests. 
First run is the longest, but next attempts will be instant

```
./run-integration-tests.sh
```


### Build XRootD-Connector in hadalytic

Prerequisites:

```
xrootd-client, xrootd-client-libs, xrootd-client-devel
```

Use "make all" command to compile
```
make all
```

### Use XRootD-Connector prebuild environment

**Use XRootD-Connector Docker with your gcc, java version, and hadoop versions**

Go to this repository directory (where e.g. `BUILD_PATH=$(pwd)`) and run the docker


If you are already on Linux, and in hadoop-xrootd-connector folder use:

```
BUILD_PATH=/path/to/hadoop-xrootd-connector
```

or on Linux

```
BUILD_PATH=$(pwd) 
```

The below command will bring you to the bash shell, in which you can execute `make all`, `make clean` or
`hdfs dfs -ls root:// ` as you were on preconfigured cluster

```
docker run --rm -it -v $BUILD_PATH:/data gitlab-registry.cern.ch/awg/hadoop-xrootd-connector bash
```

You can then test your connector with

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

### Recommended

NOTE: If you have not yet, build the base image containing all required dependencies

```
docker build -t gitlab-registry.cern.ch/awg/hadoop-xrootd-connector $BUILD_PATH
```

NOTE: User inside docker is different then on parent host, thus one might need to
change ownership with `chown

NOTE: If you don't have connectivity inside the docker e.g. `ping www.google.com` 
please ensure that you edit `/etc/docker/daemon.json` with 
```
{ 
    "dns": ["<your-cern-dns>", "8.8.8.8"] 
}
```