CP:=$(shell hadoop classpath)
JFLAGS = -g -XDignore.symbol.file -Xlint:unchecked  -Xlint:deprecation -cp .:$(CP)
JHFLAGS = -jni -force -classpath .:$(CP)
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

INCLxrootd=/usr/include/xrootd
#INCLjava=/usr/java/jdk1.8.0_121/include
INCLjava=/usr/lib/jvm/java-1.7.0-oracle.x86_64/include

CXXFLAGS=-I$(INCLxrootd) -I$(INCLjava) -I$(INCLjava)/linux -fPIC

CLASSES = ch/cern/eos/XrdClFile.java ch/cern/eos/Krb5TokenIdentifier.java ch/cern/eos/EOSFileSystem.java ch/cern/eos/EOSInputStream.java ch/cern/eos/EOSOutputStream.java ch/cern/eos/EOSKrb5.java ch/cern/eos/Krb5TokenRenewer.java ch/cern/eos/EOSDebugLogger.java

all: libjXrdCl.so EOSfs.jar

clean:
	-rm ch_cern_eos_*.o ch_cern_eos_*.h ch/cern/eos/*.class libjXrdCl.so

classes: $(CLASSES:.java=.class)

#	jar -cfe $@ coucou $(CLASSES:.java=.class) META-INF
EOSfs.jar: classes
	jar -cfe $@ coucou $(CLASSES:.java=.class) META-INF
	

#ch/cern/eos/XrdClFile.class: ch/cern/eos/XrdClFile.java
#	javac ch/cern/eos/XrdClFile.java

ch_cern_eos_XrdClFile.h: ch/cern/eos/XrdClFile.class
	javah $(JHFLAGS) ch.cern.eos.XrdClFile

ch_cern_eos_EOSFileSystem.h: ch/cern/eos/EOSFileSystem.class
	javah $(JHFLAGS) ch.cern.eos.EOSFileSystem


ch_cern_eos_XrdClFile.o: ch_cern_eos_XrdClFile.cpp ch_cern_eos_XrdClFile.h

ch_cern_eos_EOSFileSystem.o: ch_cern_eos_EOSFileSystem.cpp ch_cern_eos_EOSFileSystem.h

libjXrdCl.so: ch_cern_eos_XrdClFile.o ch_cern_eos_EOSFileSystem.o
	g++ -shared -o $@ $^  -lXrdCl -lXrdUtils -ldl
