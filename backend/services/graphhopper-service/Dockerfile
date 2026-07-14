FROM eclipse-temurin:21-jre

WORKDIR /app

COPY graphhopper-web-11.0.jar /app/graphhopper-web.jar
COPY config.yml /app/config.yml
COPY bologna.osm.pbf /app/bologna.osm.pbf

EXPOSE 8989

CMD ["java", "-Xms1g", "-Xmx2g", "-jar", "/app/graphhopper-web.jar", "server", "/app/config.yml"]