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

### Build XRootD-Connector with docker

**Build XRootD-Connector with your gcc, java version, and hadoop versions**

Go to this repository directory (where e.g. `BUILD_PATH=$(pwd)`)

```
BUILD_PATH=/path/to/hadoop-xrootd-connector
echo $BUILD_PATH
```

If you have not yet, build the base image containing all required dependencies

```
docker build -t gitlab-registry.cern.ch/awg/hadoop-xrootd-connector $BUILD_PATH
```

Build the hadoop-xrootd-connector in parent directory

```
docker run --rm -v $BUILD_PATH:/data gitlab-registry.cern.ch/awg/hadoop-xrootd-connector
```

NOTE: User inside docker is different then on parent host, thus one might need to
change ownership with `chown`

NOTE: you can also execute custom commands e.g `make clean`

```
docker run --rm -v $BUILD_PATH:/data gitlab-registry.cern.ch/awg/hadoop-xrootd-connector sh -c "make clean"
```