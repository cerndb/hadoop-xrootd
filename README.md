# CERN IT  Hadoop-XRootD-Connector

Connector between Hadoop and XRootD protocols (EOS compatible) 

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
This will bring you to the bash shell, in which you can execute `make all`, `make clean` or
`hdfs dfs -ls root:// ` as you were on preconfigured cluster

```
BUILD_PATH=/path/to/hadoop-xrootd-connector
echo $BUILD_PATH
docker run --rm -it -v $BUILD_PATH:/data gitlab-registry.cern.ch/awg/hadoop-xrootd-connector
```

If you are already on Linux, and in current folder use:

```
docker run --rm -it -v $(pwd):/data gitlab-registry.cern.ch/awg/hadoop-xrootd-connector
```

NOTE: If you have not yet, build the base image containing all required dependencies

```
docker build -t gitlab-registry.cern.ch/awg/hadoop-xrootd-connector $BUILD_PATH
```

NOTE: User inside docker is different then on parent host, thus one might need to
change ownership with `chown`