FROM navikt/java:12

COPY spenn-server/target/libs/*.jar ./
COPY spenn-server/target/helse-spenn.jar ./app.jar

