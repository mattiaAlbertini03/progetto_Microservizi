FROM jolielang/jolie
EXPOSE 8080
EXPOSE 8000

COPY server.ol server.ol
COPY interface.iol interface.iol
COPY MyInput.wsdl MyInput.wsdl
COPY stations.json stations.json

CMD jolie server.ol