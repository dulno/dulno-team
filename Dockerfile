FROM eclipse-temurin:26.0.2_10-jdk

COPY /build/libs/team-1.0.0-SNAPSHOT.jar team.jar
COPY /locale/ /locale/
COPY /geo/ /geo/

USER 1000

ENTRYPOINT ["java", "-jar", "team.jar"]