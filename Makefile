SMAP_JAR=target/mgb-smap-0.1-SNAPSHOT.jar
SMAP_POM=target/mgb-smap-0.1-SNAPSHOT.pom 

compile:
	sbt compile

poms:
	sbt makePom

jars:
	sbt package

install:
	mvn install:install-file -Dfile=$(SMAP_JAR) -DpomFile=$(SMAP_POM)

clean:
	mvn clean

testSMapClient:
	sbt "test:testOnly *SMapClientSpec"

testSMapServiceClient:
	sbt "test:testOnly *SMapServiceClientSpec"

runServiceServer:
	sbt "runMain org.telecomsudparis.smap.SMapServiceServer"
