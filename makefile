MAVEN_PROFILE=standalone

all: clean package test

clean:
	mvn clean
	rm -f *.jar

package:
	mvn package -DskipTests $(MAVEN_FLAGS)

test:
	set -e ;\
    export HADOOP_CLASSPATH="${PWD}/*:$(hadoop classpath)";\
    echo ${HADOOP_CLASSPATH};\
	for file in src/tests/* ; do $${file} && echo '** Success **' || exit ; done ;
