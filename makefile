

all: compile
compile:
	mvn package -DskipTests

clean:
	mvn clean
	rm -f *.jar

test:
	set -e ;\
	for file in src/tests/* ; do $${file} && echo '** Success **' || exit ; done ;
