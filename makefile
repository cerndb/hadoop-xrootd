CP:=$(shell hadoop classpath)
JFLAGS = -g -XDignore.symbol.file -Xlint:unchecked  -Xlint:deprecation -cp src/main/java/:$(CP)
JHFLAGS = -jni -force -classpath src/main/java/:$(CP)
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

INCLxrootd=/usr/include/xrootd
#INCLjava=/usr/java/jdk1.8.0_121/include
#INCLjava=/usr/lib/jvm/java-1.7.0-oracle.x86_64/include
ifeq ($(JAVA_HOME),)
INCLjava := /usr/lib/jvm/java-1.7.0-oracle.x86_64/include
else
INCLjava := $(JAVA_HOME)/include
endif

CXXFLAGS=-I$(INCLxrootd) -I$(INCLjava) -I$(INCLjava)/linux -fPIC

export CLASSPATH=$(shell hadoop classpath) 

CLASSES = src/main/java/ch/cern/eos/XRootDClFile.java src/main/java/ch/cern/eos/Krb5TokenIdentifier.java src/main/java/ch/cern/eos/XRootDFileSystem.java src/main/java/ch/cern/eos/XRootDKrb5FileSystem.java src/main/java/ch/cern/eos/XRootDInputStream.java src/main/java/ch/cern/eos/XRootDOutputStream.java src/main/java/ch/cern/eos/XRootDKrb5.java src/main/java/ch/cern/eos/Krb5TokenRenewer.java src/main/java/ch/cern/eos/DebugLogger.java src/main/java/ch/cern/eos/XRootDUtils.java src/main/java/ch/cern/eos/XRootDConstants.java src/main/java/ch/cern/eos/XRootDInstrumentation.java

all: libjXrdCl.so EOSfs.jar

clean:
	-rm ch_cern_eos_*.o src/main/cpp/ch_cern_eos_*.h src/main/java/ch/cern/eos/*.class libjXrdCl.so EOSfs.jar

.PHONY: test
test:
	{ \
	set -e ;\
	echo '* Running integration tests...: *' ;\
	for file in integration-tests/* ; do $${file} && echo '** Success **' || exit ; done ; \
	}

classes: $(CLASSES:.java=.class)

EOSfs.jar: classes
	jar -cfe $@ coucou $(CLASSES:.java=.class) META-INF

src/main/cpp/ch_cern_eos_XRootDClFile.h: src/main/java/ch/cern/eos/XRootDClFile.class
	javah -d src/main/cpp/ $(JHFLAGS) ch.cern.eos.XRootDClFile

src/main/cpp/ch_cern_eos_XRootDKrb5FileSystem.h: src/main/java/ch/cern/eos/XRootDKrb5FileSystem.class
	javah -d src/main/cpp/ $(JHFLAGS) ch.cern.eos.XRootDKrb5FileSystem

ch_cern_eos_XRootDClFile.o: src/main/cpp/ch_cern_eos_XRootDClFile.cpp src/main/cpp/ch_cern_eos_XRootDClFile.h
	g++ $(CXXFLAGS) -g -c -o ch_cern_eos_XRootDClFile.o src/main/cpp/ch_cern_eos_XRootDClFile.cpp -lXrdCl -lXrdUtils -ldl

ch_cern_eos_XRootDKrb5FileSystem.o: src/main/cpp/ch_cern_eos_XRootDKrb5FileSystem.cpp src/main/cpp/ch_cern_eos_XRootDKrb5FileSystem.h
	g++ $(CXXFLAGS) -g -c -o ch_cern_eos_XRootDKrb5FileSystem.o src/main/cpp/ch_cern_eos_XRootDKrb5FileSystem.cpp -lXrdCl -lXrdUtils -ldl

libjXrdCl.so: ch_cern_eos_XRootDClFile.o ch_cern_eos_XRootDKrb5FileSystem.o
	g++ -shared -o $@ $^  -lXrdCl -lXrdUtils -ldl
