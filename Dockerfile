# Build extension
#FROM maven:3.8.5-openjdk-17 as buildext
#
#COPY . /build
#WORKDIR /build
#RUN mvn clean install

# Add Extension to Keyclock server
FROM keycloak/keycloak:25.0.6

#COPY --from=buildext /build/target/suvera-keycloak-scim2-outbound-provisioning-jar-with-dependencies.jar /opt/keycloak/providers/
COPY target/suvera-keycloak-scim2-outbound-provisioning-jar-with-dependencies.jar /opt/keycloak/providers/

RUN /opt/keycloak/bin/kc.sh build

EXPOSE 8080/tcp
EXPOSE 8443/tcp
EXPOSE 9000/tcp

ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start-dev"]
