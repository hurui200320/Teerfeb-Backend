FROM adoptopenjdk/openjdk11
EXPOSE 7000
RUN mkdir /opt/app
COPY build/install/Teerfeb-Backend /opt/app
COPY models /opt/app/models
WORKDIR /opt/app
CMD ["bash", "./bin/Teerfeb-Backend"]

