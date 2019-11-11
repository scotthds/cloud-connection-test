##########
# NOTE: When building this image, there is an assumption that you are in the
# top level directory of the repository.
#
# To build:
# $ docker build -f envoy-management-service/Dockerfile -t envoy-management-service .
#
# To run:
# $ docker run -e CONTACT_POINTS="<IP addresses or hostnames>"\
#              -e LOCAL_DATA_CENTER="dc1" \
#              -e PLAINTEXT_AUTH_USERNAME="<DSE username>" \
#              -e PLAINTEXT_AUTH_PASSWORD="<DSE password>" \
#              -e CLIENT_DOMAIN="<Domain name for the Envoy endpoint>"
#              -e CLIENT_TLS_PATH="<Location of the tls.yaml>" # Contains locations for client certificate hashes and their CA cert
#              -e VAULT_URI="<URI for vault>"
#              -e VAULT_IAM_ROLE_NAME="<IAM role name to use for Vault auth>"
#              envoy-management-service
##########

# Build the service
FROM maven:3-jdk-8-slim

WORKDIR /app

COPY src/ src/
COPY pom.xml ./
COPY pet_comment.txt ./

RUN mvn dependency:resolve-plugins
RUN mvn clean
RUN mvn install
RUN mvn dependency:copy-dependencies

ENV DSE_USER=jd_200gb
ENV DSE_PASS=jd_200gb
ENV DSE_KEYSPACE=jd_200gb
ENV DSE_CREDS_BUNDLE=/secure-connect-jd-200gb.zip
ENV DB_WRITER=true

CMD ["/bin/bash"]
