# Spring logging

## 1. Spring Boot Logging

## 2. Setting up logback

logback 설정 방식은 logback-spring.xml 파일, logback.properties 사용하는 방법이 있다.
spring 에서는 logback-spring.xml 파일 설정 방식을 권장한다.

- 프로퍼티 설정 -> 환경 변수, 스프링 변수
- 로거 설정
- 어펜더 설정
- 인코더 설정


## 3. Setting up log structure with logstash

- 3.4.0 버전 이상은 logging.structured.{appender type}.* 로 설정 가능하다. (참고 자료 : [spring boot logging features.logging.structured](https://docs.spring.io/spring-boot/reference/features/logging.html#features.logging.structured))
- 3.4.0 버전 이하는 appender 포함한 라이브러리를 추가하고 logback 설정을 변경한다. (참고 자료 : [logstash-logback-encoder](https://github.com/logfellow/logstash-logback-encoder))
- logstash structure 특징
  - 변수가 전부 `.` 구분자를 사용한다. transaction_id -> transaction.id 처럼 표현한다.
  - `@vsersion` 필드가 추가되는데 버전에 따라 etl 작업이 수행 가능해보인다. 굉장히 유용해 보인다.


### 3.4.0 버전 이상

application.yml 파일에서 정의 가능하며 [기본 구성](https://github.com/spring-projects/spring-boot/blob/v3.4.1/spring-boot-project/spring-boot/src/main/resources/org/springframework/boot/logging/logback/structured-file-appender.xml)은 다음과 같다.

```xml
<?xml version="1.0" encoding="UTF-8"?>

<!--
File appender with structured logging logback configuration provided for import,
equivalent to the programmatic initialization performed by Boot
-->

<included>
	<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
		<filter class="ch.qos.logback.classic.filter.ThresholdFilter">
			<level>${FILE_LOG_THRESHOLD}</level>
		</filter>
		<encoder class="org.springframework.boot.logging.logback.StructuredLogEncoder">
			<format>${FILE_LOG_STRUCTURED_FORMAT}</format>
			<charset>${FILE_LOG_CHARSET}</charset>
		</encoder>
		<file>${LOG_FILE}</file>
		<rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
			<fileNamePattern>${LOGBACK_ROLLINGPOLICY_FILE_NAME_PATTERN:-${LOG_FILE}.%d{yyyy-MM-dd}.%i.gz}</fileNamePattern>
			<cleanHistoryOnStart>${LOGBACK_ROLLINGPOLICY_CLEAN_HISTORY_ON_START:-false}</cleanHistoryOnStart>
			<maxFileSize>${LOGBACK_ROLLINGPOLICY_MAX_FILE_SIZE:-10MB}</maxFileSize>
			<totalSizeCap>${LOGBACK_ROLLINGPOLICY_TOTAL_SIZE_CAP:-0}</totalSizeCap>
			<maxHistory>${LOGBACK_ROLLINGPOLICY_MAX_HISTORY:-7}</maxHistory>
		</rollingPolicy>
	</appender>
</included>
```

정의하는 형식은 다음과 같다.

```yaml
logging:
  file:
    path: ./logs
  structured:
    format:
      file: logstash
      console: logstash
    ecs:
      service:
        # 지정하지 않으면 spring.application.name 값으로 설정
        name: hello-spring-log
        # 지정하지 않으면 spring.application.version 값으로 설정
        version: 1.0.0
        environment: dev
        node-name: localhost
```

예상되는 로그 형식은 다음과 같다.

```json
{
  "@timestamp": "2024-12-21T15:07:47.804877+09:00",
  "@version": "1",
  "message": "Saying hello",
  "logger_name": "tis.hello_log_system.HelloController",
  "thread_name": "http-nio-8080-exec-1",
  "level": "INFO",
  "level_value": 20000,
  "transaction.id": "571a5b65-18cd-44a1-84cc-6a78758a18eb"
}
```

해당 방식은 세팅 시작은 쉽지만 커스텀은 데코레이터 패턴을 활용해야 한다.
append name 이 FILE 을 레퍼런스로 사용하면 된다.

```xml
<appender name="custom_appender" class="...">
    <appender-ref ref="FILE" />
</appender>
```

> `/org/springframework/boot/logging/logback/structured-file-appender.xml` 파일을 참고해야 한다.

### 3.4.0 버전 이하

logback 설정을 직접 변경해야 한다. 번거롭지만 커스터마이징이 쉽다.
build.gradle 파일에 logstash-logback-encoder 라이브러리를 추가한다.

```kotlin
dependencies {
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")
}
```

로컬에 파일 롤링 업데이트하려면 logback-spring.xml 파일을 다음과 같이 설정한다.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="ECS용_어펜더" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
          <!-- MDC에 저장된 값을 포함할지 여부다. MDC 값은 기본적으로 추가되므로 따로 설정할 필요없다. -->
          <!--        <includeMdcKeyName>transaction.id</includeMdcKeyName>-->
          <!-- MDC에 저장된 값을 제외할 여부다. -->
          <excludeMdcKeyName>transaction.id</excludeMdcKeyName>
        </encoder>

        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
          <fileNamePattern>${LOGBACK_ROLLINGPOLICY_FILE_NAME_PATTERN:-${LOG_FILE}.%d{yyyy-MM-dd}.%i.gz}</fileNamePattern>
          <cleanHistoryOnStart>${LOGBACK_ROLLINGPOLICY_CLEAN_HISTORY_ON_START:-false}</cleanHistoryOnStart>
          <maxFileSize>${LOGBACK_ROLLINGPOLICY_MAX_FILE_SIZE:-10MB}</maxFileSize>
          <totalSizeCap>${LOGBACK_ROLLINGPOLICY_TOTAL_SIZE_CAP:-0}</totalSizeCap>
          <maxHistory>${LOGBACK_ROLLINGPOLICY_MAX_HISTORY:-7}</maxHistory>
        </rollingPolicy>
    </appender>

    <logger name="ECS용_로거">
        <appender-ref ref="ECS용_어펜더"/>
    </logger>
</configuration>
```

세팅하면서 도움 된 정보를 나열했다.

유용한 필드 설정은 다음과 같다.

- [Key Value Pair Fields](https://github.com/logfellow/logstash-logback-encoder?tab=readme-ov-file#key-value-pair-fields) : 제외하고 싶은 필드 설정 (excludeMdcKeyName 사용하자.)
- [Caller Info Fields](https://github.com/logfellow/logstash-logback-encoder?tab=readme-ov-file#caller-info-fields) : 호출자 정보 추가 설정
- [Masking](https://github.com/logfellow/logstash-logback-encoder?tab=readme-ov-file#custom-fields) : 특정 필드 마스킹 처리 설정
- [Custom Fields](https://github.com/logfellow/logstash-logback-encoder?tab=readme-ov-file#custom-fields) : 사용자 정의 필드 추가 설정 (Markers 를 이용하면 json 필드 추가 가능하다. StructuredArguments 는 message 에 데이터가 포함된다.)

[유용한 예외 관련 로깅 설정](https://github.com/logfellow/logstash-logback-encoder?tab=readme-ov-file#customizing-stack-traces)은 다음과 같다.

- [Omit common frames](https://github.com/logfellow/logstash-logback-encoder?tab=readme-ov-file#omit-common-frames) : 불필요한 데이터 생략
- [Exclude frames per regex](https://github.com/logfellow/logstash-logback-encoder?tab=readme-ov-file#exclude-frames-per-regex) : 스택 트레이스 요약
- [Maximum depth per throwable](https://github.com/logfellow/logstash-logback-encoder?tab=readme-ov-file#maximum-depth-per-throwable) : 스택 트레이스 깊이 설정
- [Maximum trace size bytes](https://github.com/logfellow/logstash-logback-encoder?tab=readme-ov-file#maximum-trace-size-bytes) : 스택 트레이스 최대 크기 설정
- [Root cause first](https://github.com/logfellow/logstash-logback-encoder?tab=readme-ov-file#root-cause-first) : 루트 원인을 먼저 표시

만약 커스텀하고자 한다면 다음 순서로 세팅 방식을 고민한다.

1. 루트 원인을 먼저 표시할지 판단 ([Root cause first](https://github.com/logfellow/logstash-logback-encoder?tab=readme-ov-file#root-cause-first))
2. 스택 트레이스 형식 설정 ([Exclude frames per regex](https://github.com/logfellow/logstash-logback-encoder?tab=readme-ov-file#exclude-frames-per-regex), [Omit common frames](https://github.com/logfellow/logstash-logback-encoder?tab=readme-ov-file#omit-common-frames), [Maximum depth per throwable](https://github.com/logfellow/logstash-logback-encoder?tab=readme-ov-file#maximum-depth-per-throwable))
3. 스택 트레이스 최대 크기 설정 ([Maximum trace size bytes](https://github.com/logfellow/logstash-logback-encoder?tab=readme-ov-file#maximum-trace-size-bytes))



