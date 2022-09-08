# Flatbuffer converter for kafka-connect-jdbc

Demonstration of a custom converter used in kafka-connect-jdbc.

## Build

Build all stuff

```shell
./gradlew build
```

Pack converter with dependencies into a single fat jar. This jar is used by a JDBC connector class loader in
`confluentinc/cp-kafka-connect` image

```shell
./gradlew :jdbc-converter:shadowJar
```

Build directory is mounted as a plugin source in `docker-compose.yaml`

```yaml
    volumes:
      - $PWD/connectors:/connectors # connector-jdbc + postgres driver
      - $PWD/jdbc-converter/build/libs:/custom-connectors # custom converter build dir
```

### Run

This demo uses several images with:

* zookeeper
* kafka broker
* postgres db
* kafka-connect

To start them run

```shell
docker-composer up
```

#### Check setup

```shell
curl http://localhost:8083/connector-plugins
```

`io.confluent.connect.jdbc.JdbcSinkConnector` JDBC connector must be there

### Run applications

#### Generate events

Java app `EventGenerator.main()` publishes a batch of

```flatbuffers
table FBReportEvent {
    id: long;
    who: string;
    what: string;
    when: long;
    status: FBReportEventStatus;
}
```

events to `demo.reports` kafka topic

#### Kafka stream store change log

Run kafka stream application `ReportKStreamApplication`

```shell
./gradlew :kafka-apps:bootRun
```

with a single stream processor which listens for incoming events and handle them into `FBReport`.

Main classes: 

`FlatbuffersDAO` -  generic DAO for wrappers. Controls audit data, has direct access to a kafka store

`ReportDAO` - generated DAO for `ReportWrapper`

`ReportWrapper` - generated class for mutation control around flatbuffers table

```flatbuffers
table FBReport {
    id: long;
    who: string;
    what: string;
    when: long;
    audit: FBAudit;
}
```

`ReportProcessor` - main processor to handle CUD operations on `ReportWrapper` objects

#### Start custom converter

Custom connector listens for store internal changelog topic
`report-processor-store-changelog`, converts flatbuffers table to kafka structs
with `org.example.models.generated.ReportConverter` and sink data into `REPORTS_LOG` table.

Connector properties:

```yaml
"topics"                             :"report-processor-store-changelog",
"value.converter.fb.converter.class" :"org.example.models.generated.ReportConverter",
"table.name.format"                  :"REPORTS_LOG",
```

Start connector with flatbuffers converter

```shell
curl -X PUT http://localhost:8083/connectors/FBReport-sink-postgres/config \
     -H "Content-Type: application/json" -d '{
    "connector.class"                    : "io.confluent.connect.jdbc.JdbcSinkConnector",
    "connection.url"                     : "jdbc:postgresql://postgres:5432/oper",
    "topics"                             : "report-processor-store-changelog",
    "key.converter"                      : "org.apache.kafka.connect.converters.LongConverter",
    "value.converter"                    : "org.example.FlatbufferSinkConverter",
    "value.converter.fb.converter.class" : "org.example.models.generated.ReportConverter",
    "transforms"                         : "SkipTombstone",
    "transforms.SkipTombstone.type"      : "org.example.SkipTombstone",
    "connection.user"                    : "operuser",
    "connection.password"                : "operpass",
    "table.name.format"                  : "REPORTS_LOG",
    "auto.create"                        : true,
    "auto.evolve"                        : true,
    "insert.mode"                        : "insert"
}'
```

Result table state

| id | who | what | when | audit\_\_trace\_id | audit\_\_version | audit\_\_created\_at | audit\_\_modified\_at | audit\_\_modified\_by | audit\_\_removed |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 0 | 0.8531306570647079 | 0.9545765261201834 | 753892086890999 | 0 | 1 | 1662647289369 | 1662647289369 |  | false |
| 1 | 0.29314451965634036 | 0.2239690665232763 | 753892661578041 | 0 | 1 | 1662647289380 | 1662647289380 |  | false |
| 1 | 0.29314451965634036 | 0.2239690665232763 | 753892661578041 | 0 | 2 | 1662647289380 | 1662647333486 |  | true |
| 2 | 0.7072722332995743 | 0.7641375036179949 | 753892662035791 | 0 | 1 | 1662647289381 | 1662647289381 |  | false |
| 2 | 0.6393934031119062 | 0.31887141163835 | 753892662182666 | 0 | 2 | 1662647289381 | 1662647289381 |  | false |
| 2 | 0.3805541061659019 | 0.9704368599156534 | 753892662301249 | 0 | 3 | 1662647289381 | 1662647289381 |  | false |
| 2 | 0.3805541061659019 | 0.9704368599156534 | 753892662301249 | 0 | 4 | 1662647289381 | 1662647339269 |  | true |

#### Check connector status

```shell
http://localhost:8083/connectors?expand=status
```

#### Remove connector

```shell
curl -X DELETE http://localhost:8083/connectors/sink-postgres
```
### Important classes

`:model`
* [FlatbuffersSerde.java](https://github.com/limitium/kafka-connect-jdbc-flatbuffers/blob/master/model/src/main/java/org/example/serde/FlatbuffersSerde.java) - generic `Serde`, works with any flatbuffers
  
`:kafka-apps`

`:jdbc-connect`
* [FlatbufferSinkConverter.java](https://github.com/limitium/kafka-connect-jdbc-flatbuffers/blob/master/jdbc-converter/src/main/java/org/example/FlatbufferSinkConverter.java) - Custom Flatbuffers converter uses child class of [ModelConverter](https://github.com/limitium/kafka-connect-jdbc-flatbuffers/blob/master/jdbc-converter/src/main/java/org/example/models/ModelConverter.java) for actual conversion

### Generated or semi-generated class

:kafka-apps module
* `org.example.dao.generated.ReportDAO` - is used to bind generic types in `FlatbuffersDAO` class
* `org.example.dao.generated.ReportWrapper` - controls object mutation and ensures the assembly of flatbuffers

:jdbc-converter module
* `org.example.models.generated.ReportConverter` - provides a schema for object storage and transfers data from flatbuffers to a struct

## TODO

* Add trace id to audit data
* Add modified by to audit data