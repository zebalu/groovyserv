FROM debian:bookworm

ENV JAVA_VERSION 8.0.452-tem
ENV GROOVY_VERSION 2.4.14
ENV GOLANG_VERSION 1.9.4

# Prepare environment
ENV JAVA_HOME /opt/java
ENV PATH $PATH:/usr/local/go/bin:$JAVA_HOME/bin

# SDKMAN: Java / Groovy
RUN apt-get update && apt-get install -y curl unzip zip wget
RUN curl -s "https://get.sdkman.io" | bash 
RUN /bin/bash -lc "sdk install java $JAVA_VERSION" 
RUN /bin/bash -lc "sdk install groovy $GROOVY_VERSION"
RUN /bin/bash -lc "wget -O go.tar.gz https://go.dev/dl/go${GOLANG_VERSION}.linux-amd64.tar.gz"
RUN /bin/bash -lc "tar -C /usr/local -xzf go.tar.gz"

ENV JAVA_HOME /root/.sdkman/candidates/java/current
ENV GROOVY_HOME /root/.sdkman/candidates/groovy/current
ENV GO_HOME /usr/local/go
ENV PATH $PATH:$GROOVY_HOME/bin:$JAVA_HOME/bin:$GO_HOME/bin

WORKDIR /usr/src/app
CMD ["./gradlew"]
