CP:=$(shell hadoop classpath)
JFLAGS = -g -XDignore.symbol.file -Xlint:unchecked  -Xlint:deprecation -cp ./src:$(CP)
JHFLAGS = -jni -force -classpath ./src:$(CP)
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

CLASSES = src/ch/cern/eos/XrootDBasedClFile.java src/ch/cern/eos/Krb5TokenIdentifier.java src/ch/cern/eos/XrootDBasedFileSystem.java src/ch/cern/eos/XrootDBasedKerberizedFileSystem.java src/ch/cern/eos/XrootDBasedInputStream.java src/ch/cern/eos/XrootDBasedOutputStream.java src/ch/cern/eos/XrootDBasedKrb5.java src/ch/cern/eos/Krb5TokenRenewer.java src/ch/cern/eos/DebugLogger.java

all: libjXrdCl.so EOSfs.jar

clean:
	-rm ch_cern_eos_*.o ch_cern_eos_*.h src/ch/cern/eos/*.class libjXrdCl.so EOSfs.jar

classes: $(CLASSES:.java=.class)

#	jar -cfe $@ coucou $(CLASSES:.java=.class) META-INF
EOSfs.jar: classes
	jar -cfe $@ coucou $(CLASSES:.java=.class) META-INF
	

#ch/cern/eos/XrootDBasedFile.class: src/ch/cern/eos/XrootDBasedClFile.java
#	javac src/ch/cern/eos/XrootDBasedClFile.java

ch_cern_eos_XrootDBasedClFile.h: src/ch/cern/eos/XrootDBasedClFile.class
	javah $(JHFLAGS) ch.cern.eos.XrootDBasedClFile

ch_cern_eos_XrootDBasedKerberizedFileSystem.h: src/ch/cern/eos/XrootDBasedKerberizedFileSystem.class
	javah $(JHFLAGS) ch.cern.eos.XrootDBasedKerberizedFileSystem

ch_cern_eos_XrootDBasedClFile.o: ch_cern_eos_XrootDBasedClFile.cpp ch_cern_eos_XrootDBasedClFile.h

ch_cern_eos_XrootDBasedKerberizedFileSystem.o: ch_cern_eos_XrootDBasedKerberizedFileSystem.cpp ch_cern_eos_XrootDBasedKerberizedFileSystem.h

libjXrdCl.so: ch_cern_eos_XrootDBasedClFile.o ch_cern_eos_XrootDBasedKerberizedFileSystem.o
	g++ -shared -o $@ $^  -lXrdCl -lXrdUtils -ldl
