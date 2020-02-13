FROM navikt/java:12

COPY spenn-server/target/dependency/*.jar ./
COPY spenn-server/target/helse-spenn.jar ./app.jar

