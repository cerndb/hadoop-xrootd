CP:=$(shell hadoop classpath)
JFLAGS = -g -XDignore.symbol.file -Xlint:unchecked  -Xlint:deprecation -cp .:$(CP)
JHFLAGS = -jni -force -classpath .:$(CP)
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

CLASSES = ch/cern/eos/XRootDClFile.java ch/cern/eos/Krb5TokenIdentifier.java ch/cern/eos/XRootDFileSystem.java ch/cern/eos/XRootDKrb5FileSystem.java ch/cern/eos/XRootDInputStream.java ch/cern/eos/XRootDOutputStream.java ch/cern/eos/XRootDKrb5.java ch/cern/eos/Krb5TokenRenewer.java ch/cern/eos/DebugLogger.java ch/cern/eos/XRootDUtils.java ch/cern/eos/XRootDConstants.java ch/cern/eos/XRootDInstrumentation.java

all: libjXrdCl.so EOSfs.jar

clean:
	-rm ch_cern_eos_*.o ch_cern_eos_*.h ch/cern/eos/*.class libjXrdCl.so EOSfs.jar

test:
	{ \
	set -e ;\
	echo '* Running integration tests...: *' ;\
	for file in integration-tests/* ; do $${file} && echo '** Success **' || exit ; done ; \
	}

classes: $(CLASSES:.java=.class)

EOSfs.jar: classes
	jar -cfe $@ coucou $(CLASSES:.java=.class) META-INF

ch_cern_eos_XRootDClFile.h: ch/cern/eos/XRootDClFile.class
	javah $(JHFLAGS) ch.cern.eos.XRootDClFile

ch_cern_eos_XRootDKrb5FileSystem.h: ch/cern/eos/XRootDKrb5FileSystem.class
	javah $(JHFLAGS) ch.cern.eos.XRootDKrb5FileSystem

ch_cern_eos_XRootDClFile.o: ch_cern_eos_XRootDClFile.cpp ch_cern_eos_XRootDClFile.h

ch_cern_eos_XRootDKrb5FileSystem.o: ch_cern_eos_XRootDKrb5FileSystem.cpp ch_cern_eos_XRootDKrb5FileSystem.h

libjXrdCl.so: ch_cern_eos_XRootDClFile.o ch_cern_eos_XRootDKrb5FileSystem.o
	g++ -shared -o $@ $^  -lXrdCl -lXrdUtils -ldl
