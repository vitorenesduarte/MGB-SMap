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
	sbt clean

dockerBuild:
	 cd Dockerfiles; docker build -t mgb-smap:latest .

testSMapClient:
	sbt "test:testOnly *SMapClientSpec"

testSMapServiceClient:
	sbt "test:testOnly *SMapServiceClientSpec"

runServiceServer:
	sbt "runMain org.telecomsudparis.smap.SMapServiceServer"

runServiceStandard:
	sbt "runMain org.telecomsudparis.smap.SMapServiceServer -sp 8980 -lr true -vb true -zkh 127.0.0.1 -zkp 2181 -ts undefined"
