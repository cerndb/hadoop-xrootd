MAVEN_PROFILE=standalone

all: compile

compile:
	mvn package -DskipTests -P$(MAVEN_PROFILE)

clean:
	mvn clean
	rm -f *.jar

test:
	set -e ;\
	for file in src/tests/* ; do $${file} && echo '** Success **' || exit ; done ;
