FROM openjdk:8-slim

MAINTAINER TFR <tfr011@gmail.com>

WORKDIR /app
RUN apt-get update && apt-get install -y \
    git \
    maven \
    curl \
    build-essential

ENV SBT_VERSION 1.0.4
RUN \
  curl -L -o sbt-$SBT_VERSION.deb http://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb && \
  dpkg -i sbt-$SBT_VERSION.deb && \
  rm sbt-$SBT_VERSION.deb && \
  apt-get update && \
  apt-get install sbt && \
  sbt sbtVersion

ADD https://api.github.com/repos/vitorenesduarte/VCD-java-client/git/refs/heads/zk vcd-java-client-version.json
RUN git clone -b zk https://github.com/vitorenesduarte/VCD-java-client
RUN mvn -f VCD-java-client clean install -DskipTests

ADD https://api.github.com/repos/tuanir/MGB-SMap/git/refs/heads/master mgb-smap-version.json
RUN git clone https://github.com/tuanir/MGB-SMap.git
RUN make -C MGB-SMap compile jars

WORKDIR /app/MGB-SMap

ENV SERVERPORT 8980
ENV LOCALREADS true
ENV VERBOSE true
ENV ZHOST 127.0.0.1
ENV ZPORT 2181
ENV TIMESTAMP undefined

CMD ["sh", "-c", "/app/MGB-SMap/target/pack/bin/MGB-SMap -sp ${SERVERPORT} \
                -lr ${LOCALREADS} \
                -vb ${VERBOSE} \
                -zkh ${ZHOST} \
                -zkp ${ZPORT} \
                -ts ${TIMESTAMP}"]
