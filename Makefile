SMAP_JAR=target/mgb-smap-0.1-SNAPSHOT.jar
SMAP_POM=target/mgb-smap-0.1-SNAPSHOT.pom 

all:
	sbt compile
	sbt pack
	sbt makePom
	mvn install:install-file -Dfile=$(SMAP_JAR) -DpomFile=$(SMAP_POM)

compile:
	sbt compile

poms:
	sbt makePom

jars:
	sbt pack

install:
	mvn install:install-file -Dfile=$(SMAP_JAR) -DpomFile=$(SMAP_POM)

clean:
	sbt clean

dockerBuild:
	 docker build --cpuset-cpus "1,2" -t tfr011/mgb-smap:latest -f Dockerfiles/Dockerfile .

testSMapClient:
	sbt "test:testOnly *SMapClientSpec"

testSMapServiceClient:
	sbt "test:testOnly *SMapServiceClientSpec"

runServiceServer:
	sbt "runMain org.telecomsudparis.smap.SMapServiceServer"

runServiceStandard:
	sbt "runMain org.telecomsudparis.smap.SMapServiceServer -sp 8980 -lr true -vb true -zkh 127.0.0.1 -zkp 2181 -ts undefined -rt 300"
