all: compile
compile:
	mvn package -DskipTests

clean:
	mvn clean

test:
	set -e ;\
	echo '* Running integration tests...: *' ;\
	for file in integration-tests/* ; do $${file} && echo '** Success **' || exit ; done ; \
	}
