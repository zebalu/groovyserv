FROM golang:1.24.2-bookworm

ENV JAVA_VERSION 21.0.7-tem
ENV GROOVY_VERSION 4.0.26
ENV GOLANG_VERSION 1.24.2
ENV GRADLE_VERSION 8.13

# Prepare environment
ENV JAVA_HOME /opt/java
ENV PATH $PATH:$JAVA_HOME/bin

# SDKMAN: Java / Groovy
RUN apt-get update && apt-get install -y --no-install-recommends curl unzip zip && \
    curl -s "https://get.sdkman.io" | bash && \
    /bin/bash -lc "sdk install java $JAVA_VERSION" && \
    /bin/bash -lc "sdk install groovy $GROOVY_VERSION" && \
    /bin/bash -lc "sdk install gradle $GRADLE_VERSION"
ENV JAVA_HOME /root/.sdkman/candidates/java/current
ENV GROOVY_HOME /root/.sdkman/candidates/groovy/current
ENV GRADLE_HOME /root/.sdkman/candidates/gradle/current
ENV PATH $PATH:$GROOVY_HOME/bin:$JAVA_HOME/bin:$GRADLE_HOME/bin/

WORKDIR /usr/src/app
CMD ["gradle"]
