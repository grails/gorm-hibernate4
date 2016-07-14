#!/bin/bash
EXIT_STATUS=0

./gradlew --stop

./gradlew compileGroovy
./gradlew --stop
./gradlew compileTestGroovy
./gradlew --stop

./gradlew test --refresh-dependencies -no-daemon -x grails2-plugin:test -x gorm-hibernate4-spring-boot:test  || EXIT_STATUS=$?
if [[ $EXIT_STATUS -eq 0 ]]; then
    ./gradlew grails2-plugin:test -no-daemon -Pgrails2.plugin.enabled=true || EXIT_STATUS=$?
fi
if [[ $EXIT_STATUS -eq 0 ]]; then 
    ./gradlew gorm-hibernate4-spring-boot:test --refresh-dependencies -no-daemon || EXIT_STATUS=$?
fi

./gradlew --stop

if [[ $EXIT_STATUS -eq 0 ]]; then
    ./travis-publish.sh || EXIT_STATUS=$?
fi

exit $EXIT_STATUS



