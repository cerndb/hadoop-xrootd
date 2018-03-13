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

CLASSES = src/main/java/ch/cern/eos/XrootDBasedClFile.java src/main/java/ch/cern/eos/Krb5TokenIdentifier.java src/main/java/ch/cern/eos/XrootDBasedFileSystem.java src/main/java/ch/cern/eos/XrootDBasedKerberizedFileSystem.java src/main/java/ch/cern/eos/XrootDBasedInputStream.java src/main/java/ch/cern/eos/XrootDBasedOutputStream.java src/main/java/ch/cern/eos/XrootDBasedKrb5.java src/main/java/ch/cern/eos/Krb5TokenRenewer.java src/main/java/ch/cern/eos/DebugLogger.java

all: libjXrdCl.so EOSfs.jar

clean:
	-rm ch_cern_eos_*.o src/main/cpp/ch_cern_eos_*.h src/main/java/ch/cern/eos/*.class libjXrdCl.so EOSfs.jar

test:
	{ \
	set -e ;\
	echo '* Running integration tests...: *' ;\
	for file in integration-tests/* ; do $${file} && echo '** Success **' || exit ; done ; \
	}

classes: $(CLASSES:.java=.class)

EOSfs.jar: classes
	jar -cfe $@ coucou $(CLASSES:.java=.class) META-INF

src/main/cpp/ch_cern_eos_XrootDBasedClFile.h: src/main/java/ch/cern/eos/XrootDBasedClFile.class
	javah -d src/main/cpp/ $(JHFLAGS) ch.cern.eos.XrootDBasedClFile

src/main/cpp/ch_cern_eos_XrootDBasedKerberizedFileSystem.h: src/main/java/ch/cern/eos/XrootDBasedKerberizedFileSystem.class
	javah -d src/main/cpp/ $(JHFLAGS) ch.cern.eos.XrootDBasedKerberizedFileSystem

ch_cern_eos_XrootDBasedClFile.o: src/main/cpp/ch_cern_eos_XrootDBasedClFile.cpp src/main/cpp/ch_cern_eos_XrootDBasedClFile.h src/main/cpp/ch_cern_eos_XrootDBasedClFile.h
	g++ $(CXXFLAGS) -c -o ch_cern_eos_XrootDBasedClFile.o src/main/cpp/ch_cern_eos_XrootDBasedClFile.cpp -lXrdCl -lXrdUtils -ldl

ch_cern_eos_XrootDBasedKerberizedFileSystem.o: src/main/cpp/ch_cern_eos_XrootDBasedKerberizedFileSystem.cpp src/main/cpp/ch_cern_eos_XrootDBasedKerberizedFileSystem.h
	g++ $(CXXFLAGS) -c -o ch_cern_eos_XrootDBasedKerberizedFileSystem.o src/main/cpp/ch_cern_eos_XrootDBasedKerberizedFileSystem.cpp -lXrdCl -lXrdUtils -ldl

libjXrdCl.so: ch_cern_eos_XrootDBasedClFile.o ch_cern_eos_XrootDBasedKerberizedFileSystem.o
	g++ -shared -o $@ $^  -lXrdCl -lXrdUtils -ldl
