MAVEN_PROFILE=standalone

all: package test

package:
	mvn package -DskipTests $(MAVEN_FLAGS)

clean:
	mvn clean
	rm -f *.jar

test:
	set -e ;\
    export HADOOP_CLASSPATH="${PWD}/*:$(hadoop classpath)";\
    echo ${HADOOP_CLASSPATH};\
	for file in src/tests/* ; do $${file} && echo '** Success **' || exit ; done ;
