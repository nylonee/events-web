FROM hseeberger/scala-sbt:eclipse-temurin-11.0.14.1_1.6.2_2.13.8 as build

WORKDIR /app

# Copy the project files necessary for sbt update first and then run update. These layers will be cached and
# sbt update will not be run unless these files change.
COPY / /
RUN sbt update

# Copy the rest of the project and compile it. This layer will be rebuilt whenever
# a source file changes.
COPY . .
RUN sbt compile stage

FROM openjdk:11-jre-slim

WORKDIR /app

# Copy the necessary files from the build image.
COPY --from=build /app/target/universal/stage /app

# Expose port 9000
EXPOSE 9000

# Run the application
#CMD ["tail", "-f", "/dev/null"]
CMD ["/app/bin/events-web", "-Dplay.http.secret.key=some-secret-key"]