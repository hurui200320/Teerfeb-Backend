FROM adoptopenjdk/openjdk11
EXPOSE 8080
RUN mkdir /opt/app
COPY models /opt/app/models
COPY build/install/Teerfeb-Backend /opt/app
WORKDIR /opt/app
CMD ["bash", "./bin/Teerfeb-Backend"]

