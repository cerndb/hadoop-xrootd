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

CLASSES = ch/cern/eos/XrootDBasedClFile.java ch/cern/eos/Krb5TokenIdentifier.java ch/cern/eos/XrootDBasedFileSystem.java ch/cern/eos/XrootDBasedKerberizedFileSystem.java ch/cern/eos/XrootDBasedInputStream.java ch/cern/eos/XrootDBasedOutputStream.java ch/cern/eos/XrootDBasedKrb5.java ch/cern/eos/Krb5TokenRenewer.java ch/cern/eos/DebugLogger.java

all: libjXrdCl.so EOSfs.jar

clean:
	-rm ch_cern_eos_*.o ch_cern_eos_*.h ch/cern/eos/*.class libjXrdCl.so

classes: $(CLASSES:.java=.class)

#	jar -cfe $@ coucou $(CLASSES:.java=.class) META-INF
EOSfs.jar: classes
	jar -cfe $@ coucou $(CLASSES:.java=.class) META-INF
	

#ch/cern/eos/XrootDBasedFile.class: ch/cern/eos/XrootDBasedClFile.java
#	javac ch/cern/eos/XrootDBasedClFile.java

ch_cern_eos_XrootDBasedClFile.h: ch/cern/eos/XrootDBasedClFile.class
	javah $(JHFLAGS) ch.cern.eos.XrootDBasedClFile

ch_cern_eos_XrootDBasedKerberizedFileSystem.h: ch/cern/eos/XrootDBasedKerberizedFileSystem.class
	javah $(JHFLAGS) ch.cern.eos.XrootDBasedKerberizedFileSystem

ch_cern_eos_XrootDBasedFile.o: ch_cern_eos_XrootDBasedClFile.cpp ch_cern_eos_XrootDBasedClFile.h

ch_cern_eos_XrootDBasedKerberizedFileSystem.o: ch_cern_eos_XrootDBasedKerberizedFileSystem.cpp ch_cern_eos_XrootDBasedKerberizedFileSystem.h

libjXrdCl.so: ch_cern_eos_XrootDBasedFile.o ch_cern_eos_XrootDBasedKerberizedFileSystem.o
	g++ -shared -o $@ $^  -lXrdCl -lXrdUtils -ldl
