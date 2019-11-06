FROM clojure

WORKDIR /app
COPY project.clj /app/
RUN lein deps

COPY . /app

# Build an uberjar release artifact.
RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" app-standalone.jar

# Run the web service on container startup.
CMD ["java", "-jar", "app-standalone.jar"]