FROM openjdk:8
LABEL maintainer="sharadsauravbit@gmail.com"
ADD target/spring-boot-JDBC.jar spring-boot-JDBC.jar
RUN apt-get update && apt-get install -y apt-utils
RUN wget -qO - https://artifacts.elastic.co/GPG-KEY-elasticsearch | apt-key add -
RUN apt-get clean && apt-get update
RUN apt-get install apt-transport-https
RUN echo "deb https://artifacts.elastic.co/packages/6.x/apt stable main" | tee -a /etc/apt/sources.list.d/elastic-6.x.list
RUN apt-get update && apt-get install logstash
ENV PATH="/usr/share/logstash/bin/:${PATH}"
RUN curl -sL https://deb.nodesource.com/setup_10.x | bash
RUN apt update
RUN apt install nodejs
RUN apt-get install -y npm
RUN npm install elasticdump -g
EXPOSE 8080
CMD ["java", "-jar", "spring-boot-JDBC.jar"]