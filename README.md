# Spring logging

## 1. Spring Boot Logging


## 2. Setting up logback

logback 설정 방식은 logback-spring.xml 파일, logback.properties 사용하는 방법이 있다.
spring 에서는 logback-spring.xml 파일 설정 방식을 권장한다.

- 프로퍼티 설정 -> 환경 변수, 스프링 변수
- 이벤트란
- 로거 설정
- 어펜더 설정
- 인코더 설정

### Appender

- [ ] 어펜더 구조 정리
- [ ] 커스텀한 어펜더
	- [ ] [Create Custom Appender](https://logback.qos.ch/manual/appenders.html#WriteYourOwnAppender)
- [ ] 사용해볼법한 어펜더
	- [X] [File Appender](https://logback.qos.ch/manual/appenders.html#FileAppender)
	- [X] [SMTP Appender](https://logback.qos.ch/manual/appenders.html#SMTPAppender)
	- [ ] [DB Appender](https://logback.qos.ch/manual/appenders.html#DBAppender)
- [ ] 원리가 궁금한 어펜더
	- [ ] [Sifting Appender](https://logback.qos.ch/manual/appenders.html#SiftingAppender)
	- [ ] [File Appender - prudenr](https://logback.qos.ch/manual/appenders.html#prudent)
- [ ] 성능 확인하고 싶은 어펜더
	- [ ] [Async Appender](https://logback.qos.ch/manual/appenders.html#SiftingAppender)
- [ ] 커스텀한 어펜더 - s3 어펜더 만들어보자.
	- [ ] [Create Custom Appender](https://logback.qos.ch/manual/appenders.html#WriteYourOwnAppender)

### SMTP Appender


### SMTP Appender - Marker based triggering

ERROR 레벨 중 일부 이벤트 만 메일을 받을 수 있도록 마커 설정이 가능하다.

```kotlin
@RestControllerAdvice
class HelloExceptionHandler {
    private val log = LoggerFactory.getLogger(HelloExceptionHandler::class.java)
    @ExceptionHandler(RuntimeException::class)
    fun handleException(e: RuntimeException): String {
        val notifyAdmin = MarkerFactory.getMarker("NOTIFY_ADMIN")
        log.warn(notifyAdmin, "An exception occurred {}", e.message)
        return "Error: ${e.message}"
    }
}
```

logback-spring.xml 파일에는 OnMarkerEvaluator 에 마커를 등록한다.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="EMAIL" class="ch.qos.logback.classic.net.SMTPAppender">
        <evaluator class="ch.qos.logback.classic.boolex.OnMarkerEvaluator">
            <marker>NOTIFY_ADMIN</marker>
        </evaluator>
        <smtpHost>smtp-relay.gmail.com</smtpHost>
        <from>geonc123@estsoft.com</from>
        <to>geonc123@estsoft.com</to>
        <subject>TESTING: %logger{20} - %m</subject>
        <layout class="ch.qos.logback.classic.html.HTMLLayout"/>
    </appender>

    <root level="INFO">
        <appender-ref ref="EMAIL" />
    </root>
</configuration>

```


발생한 예외 수집한 모습이다.

<img width="1300" alt="스크린샷 2024-12-23 오후 1 27 43" src="https://github.com/user-attachments/assets/cc511049-6e64-471f-a550-c28eca14a005" />

느꼈던 점은 mail 보내기전 위치가 존재하고 그 이후 로그를 전송한다는 점이다. 
누군가 로그 전송한 인덱스를 저장하고 있고 이후 로그를 차례대로 출력한다.

<img width="1300" alt="스크린샷 2024-12-23 오후 1 19 46" src="https://github.com/user-attachments/assets/9ea189a9-248f-4664-a1be-5cb044119ffc" />

JaninoEventEvaluator 기반 트리거도 가능하다. expression 을 등록하면 전송한다.

```
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="EMAIL" class="ch.qos.logback.classic.net.SMTPAppender">
        <evaluator class="ch.qos.logback.classic.boolex.OnMarkerEvaluator">
            <marker>NOTIFY_ADMIN</marker>
            <expression>markerList.contains("NOTIFY_ADMIN")||markerList.contains("TRANSACTION_ADMIN")</expression>
        </evaluator>
        <smtpHost>smtp-relay.gmail.com</smtpHost>
        <from>geonc123@estsoft.com</from>
        <to>geonc123@estsoft.com</to>
<!--        <to>ANOTHER_EMAIL_DESTINATION</to> &lt;!&ndash; additional destinations are possible &ndash;&gt;-->
        <subject>TESTING: %logger{20} - %m</subject>
        <layout class="ch.qos.logback.classic.html.HTMLLayout"/>
    </appender>

    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="EMAIL" />
        <appender-ref ref="STDOUT" />
    </root>
</configuration>

```

JaninoEventEvaluator 사용하려면 build.gradle.kts 에 다음 라이브러리를 추가해야 한다.

> [spring boot coordinates](https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html) 에서 [org.codehaus.janino:commons-compiler:3.1.12](https://janino-compiler.github.io/janino/) 라이브러리가 포함돼야 하는데 포함되지 않았나보다.
> 어떤 이유에선지 라이브러리를 직접 설정해야 한다.

```
dependencies {
	implementation("org.codehaus.janino:janino:3.1.12")
}
```

아래와 같은 경우 예시다.

```kotlin
@RestControllerAdvice
class HelloExceptionHandler {
    private val log = LoggerFactory.getLogger(HelloExceptionHandler::class.java)
    @ExceptionHandler(RuntimeException::class)
    fun handleException(e: RuntimeException): String {
        val notifyAdmin: Marker = MarkerFactory.getMarker("NOTIFY_ADMIN")
        val transactionAdmin: Marker = MarkerFactory.getMarker("TRANSACTION_ADMIN")
        log.warn(notifyAdmin, "An exception occurred {}", e.message)
        log.warn(transactionAdmin, "An exception occurred {}", e.message)
        return "Error: ${e.message}"
    }
}
```

### FileAppender

OutputStreamAppender 하위 클래스로 로깅 이벤트를 파일에 추가한다. 아래 옵션으로 어떻게 저장할지 결정된다.

| Property Name	 | Type	    | Description                                                                               |
|----------------|----------|-------------------------------------------------------------------------------------------|
| append	        | boolean	 | 기존 파일 끝에 추가할지 결정한다. true 면 파일 끝에 추가하고 그렇지 않다면 삭제한다. 기본 값은 true 다.                         |
| encoder	       | Encoder	 | 로킹 이벤트가 기록되는 방식을 정한다.                                                                     |
| file	          | String	  | 작성할 파일 이름을 정한다. 파일 부모 디렉토리가 없는 경우 자동으로 생성한다.                                              |
| bufferSize	    | FileSize | immediateFlush 옵션이 false 로 설정된 경우 출력 버퍼 크기 설정한다. 기본 값은 8KB이다. 아무리 부담스러운 작업이어도 256KB 충분하다. |
| prudent        | boolean  | 한 파일을 여러 FileAppender 가 사용하는 경우 해당 파일에 신중하게 작성할지 결정한다.                                    |

> FileAppender 는 기본 출력 스트림으로 바로 flush 되므로 로깅 이벤트가 손실되지 않는다. 그러나 로깅 이벤트 처리량을 늘리기 위해 immediateFlush 설정 변경으로 버퍼를 활용 할 수 있다.

No buffer

![image](https://github.com/user-attachments/assets/a5603678-7626-440a-b514-664d9585f2e2)

With buffer

![image](https://github.com/user-attachments/assets/c0bdc2fc-5858-4407-93b5-ae3fa7c58e07)

> prudent 모드는 배타적 잠금을 활용해 직렬화하며 대략 로그 작성 비용이 3배 증가한다. NFS(네트워크 파일 시스템)에서는 더 많은 비용이 발생한다. 잠금 편향이 발생하고 기아 현상이 발생한다.
> prudent 모드는 네트워크 속도와 OS 구현 세부 정보에 성능이 좌우하는데 [FileLockSimulator](https://gist.github.com/ceki/2794241) 로 시뮬레이션 가능하다. 

> 팁 : 배치 애플리케이션을 개발하거나 단기 애플리케이션인 경우 시작마다 새 로그 파일을 만드는게 좋다. 파일에 실행한 날짜를 추가하면 된다. (참고 자료 : [Uniquely named files](https://logback.qos.ch/manual/appenders.html#uniquelyNamed))

```xml
<configuration>
  <!-- Insert the current time formatted as "yyyyMMdd'T'HHmmss" under
       the key "bySecond" into the logger context. This value will be
       available to all subsequent configuration elements. -->
  <timestamp key="bySecond" datePattern="yyyyMMdd'T'HHmmss"/>

  <appender name="FILE" class="ch.qos.logback.core.FileAppender">
    <!-- use the previously created timestamp to create a uniquely
         named log file -->
    <file>log-${bySecond}.log</file>
    <!--... -->
  </appender>
<!--... -->
 </configuration>
```

### RollingFileAppender

RollingFileAppender 는 특정 조건이 충족되면 파일을 생성해 로깅 이벤트를 적재한다.
롤오버하는 방식은 두 가지 중 하나로 설정한다.

- RollingPolicy : 롤링이 발생할 상황을 지시한다.
- TriggeringPolicy : 롤링이 발생할 시점을 지시한다.

기본적인 FileAppender 설정 방식을 따르며 추가적인 설정은 다음과 같다.

| Property Name	 | Type	          | Description             |
|----------------|----------------|-------------------------|
| rollingPolicy	 | RollingPolicy	 | 롤오버가 발생할 상황을 지시하는 방법이다. |

정책은 다음과 같다.

- RollingPolicy
  - TimeBasedRollingPolicy : 날짜별로 파일을 보관한다.
  - SizeAndTimeBasedRollingPolicy : 날짜별로 파일을 보관함과 동시에 파일 크기를 제한한다. 크기를 넘으면 해당 날짜에서 넘버링해 파일을 관리한다.
  - FixedWindowRollingPolicy : 더이상 사용하지 않는다.
- TriggeringPolicy
  - SizeBasedTriggeringPolicy : 파일 크기가 지정한 크기를 넘으면 롤링한다.

자주 사용되는 SizeAndTimeBasedRollingPolicy, SizeAndTimeBasedRollingPolicy 정책만 확인해보겠다.

#### TimeBasedRollingPolicy

| Property Name	       | Type	    | Description                                                                      |
|----------------------|----------|----------------------------------------------------------------------------------|
| fileNamePattern	     | String	  | 롤오버될 파일 이름을 정의하며 날짜 패턴이 생력되면 기본 패턴(yyyy-MM-dd)이 추가된다. 롤오버 기간은 패턴에서 유추한다.         |
| maxHistory	          | int	     | 보관할 로그 파일 수를 지정한다. 최대 보관 수를 넘으면 오래된 로그부터 비동기적으로 삭제한다. 0으로 설정하면 비활성화되며 기본 값은 0이다. |
| totalSizeCap	        | int	     | 보관한 로그 크기를 지정한다. 총 크기를 초과하면 오래된 로그부터 비동기적으로 삭제한다. 0으로 설정하면 비활성화되며 기본 값은 0이다.     |
| cleanHistoryOnStart	 | boolean	 | true로 설졍하면 appender 시작하면서 아카이브된 로그를 삭제한다.                                        |

> 팁 : TimeBasedRollingPolicy 에서 로그가 간혹 보이지 않는 경우는 아직 롤오버가 진행되지 않은 경우다. 롤오버 트리거는 서비스가 로깅 이벤트를 적재할 때 발생한다.

> 유추할 때 다중으로 쓰면 기본 지정자를 찾을 수 없기에 `%d{yyyy/MM, aux}` 방식처럼 보조 라고 지정해야 한다. 시간대를 지정하고 싶다면 `%d {yyyy-MM-dd-HH, UTC }` 처럼 지정하면 된다.

> /wombat/foo.%d.gz 형식으로 명명하면 파일을 압축(Automatic file compression)해서 관리할 수 있다.

#### SizeAndTimeBasedRollingPolicy

기본적인 TimeBasedRollingPolicy 설정 방식을 따르며 추가적인 설정은 다음과 같다.

| Property Name	   | Type	     | Description                                                                       |
|------------------|-----------|-----------------------------------------------------------------------------------|
| fileNamePattern	 | String	   | TimeBasedRollingPolicy 정책과 유사하지만 형식에 `%i` 가 꼭 포함되어야 한다. 크기가 넘어가면 해당 위치에 숫자가 증가한다. |
| maxFileSize	     | FileSize	 | 설정된 크기가 넘어가면 0부터 시작하여 증가하는 인덱스로 보관된다. `%i` 형식에 해당 인덱스가 설정된다.                      |

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



