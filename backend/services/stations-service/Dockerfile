FROM jolielang/jolie

EXPOSE 8080
EXPOSE 8000

COPY stations-service.ol stations-service.ol
COPY stations-interface.iol stations-interface.iol
COPY StationsService.wsdl StationsService.wsdl
COPY stations.json stations.json

CMD jolie stations-service.ol
