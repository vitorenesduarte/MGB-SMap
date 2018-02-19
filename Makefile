SMAP_JAR=target/mgbsmap-0.1-SNAPSHOT.jar
SMAP_POM=target/mgb-smap-0.1-SNAPSHOT.pom 

poms:
	sbt makePom

jars:
	sbt package

install:
	mvn install:install-file -Dfile=$(SMAP_JAR) -DpomFile=$(SMAP_POM)

clean:
	mvn clean
