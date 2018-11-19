MAVEN_PROFILE=standalone

all: compile test

compile:
	mvn package -DskipTests $(MAVEN_FLAGS)

clean:
	mvn clean
	rm -f *.jar

test:
	set -e ;\
    hadoop classpath ;\
	for file in src/tests/* ; do $${file} && echo '** Success **' || exit ; done ;
