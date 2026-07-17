FROM jolielang/jolie
EXPOSE 8080
EXPOSE 8000
ADD https://repo.maven.apache.org/maven2/org/postgresql/postgresql/42.7.2/postgresql-42.7.2.jar /usr/lib/jolie/lib/jdbc-postgresql.jar
COPY banking-service.ol banking-service.ol
COPY banking-interface.iol banking-interface.iol
COPY BankingService.wsdl BankingService.wsdl 
CMD jolie banking-service.ol
