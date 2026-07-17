FROM jolielang/jolie

ADD https://repo.maven.apache.org/maven2/org/postgresql/postgresql/42.7.2/postgresql-42.7.2.jar /usr/lib/jolie/lib/jdbc-postgresql.jar

EXPOSE 8080
EXPOSE 8000

COPY stations-service.ol stations-service.ol
COPY stations-interface.iol stations-interface.iol
COPY StationsService.wsdl StationsService.wsdl
CMD jolie stations-service.ol
