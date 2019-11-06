FROM navikt/java:11

COPY target/dependency/*.jar ./
COPY target/helse-spenn.jar ./app.jar

