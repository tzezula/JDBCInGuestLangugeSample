FROM maven:3-jdk-11-slim
LABEL maintainer="tomas.zezula@oracle.com"

RUN set -x \
    && apt-get -y update \
    && apt-get -y install zlib1g-dev wget \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /opt

RUN set -x \
    && wget -q --output-document graalvm-20.1.0-dev.tar.gz https://github.com/graalvm/graalvm-ce-dev-builds/releases/download/20.1.0-dev-20200419_0207/graalvm-ce-java11-linux-amd64-20.1.0-dev.tar.gz \
    && mkdir /opt/graalvm-20.1.0-dev \
    && tar -xz --strip 1 -f graalvm-20.1.0-dev.tar.gz -C /opt/graalvm-20.1.0-dev \
    && rm graalvm-20.1.0-dev.tar.gz

ENV JAVA_HOME=/opt/graalvm-20.1.0-dev

WORKDIR /build
ADD src src
ADD lib lib
ADD pom.xml .
RUN mvn package

CMD ["mvn", "exec:java", "-Dexec.mainClass=org.graalvm.contextclassloadersample.Main"]
