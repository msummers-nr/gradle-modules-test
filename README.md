# New Relic OpenTracing AWS Lambda Tracer

## Purpose

The New Relic Lambda Tracer is an OpenTracing [Tracer](https://opentracing.io/docs/overview/tracers/) implementation specifically designed to support AWS Lambda.

It captures and generates the following data:

- Span Events
- Transaction Events
- Error Events
- Traced Errors

It does not capture other New Relic data types like metrics or transaction traces.

## How to Use

1. Add the `newrelic-lambda-tracer` dependency to your project.
    - **Option A:** [Build the project from sources](#build-the-project) and [add the jar to your project as a Gradle dependency](#add-artifacts-to-gradle-project)
    - **Option B:** Add gradle or maven dependency to your project: [TODO N/A for Beta](https://mvnrepository.com)
2. Add the [AWS Lambda OpenTracing Java SDK](https://source.datanerd.us/java-agent/java-aws-lambda) as a dependency to your project and [implement the tracing request handler](https://source.datanerd.us/java-agent/java-aws-lambda#how-to-use). *Note:* In order for the `LambdaTracer` to function fully it must be used in conjunction with the `TracingRequestHandler` interface provided by the AWS Lambda OpenTracing Java SDK. If a different request handler is used the `LambdaTracer` will not be able to generate Error Events or Traced Errors.
3. Register a `LambdaTracer.INSTANCE` as the OpenTracing Global tracer as shown in the [example](#example-usage).
4. See Amazon's documentation on [creating a ZIP deployment package for a Java Lambda function](https://docs.aws.amazon.com/lambda/latest/dg/create-deployment-pkg-zip-java.html)
5. When creating your Lambda function in AWS Lambda console the handler for the given example would be entered as `com.handler.example.MyLambdaHandler::handleRequest` or just `com.handler.example.MyLambdaHandler`, the latter of which will use `handleRequest` as the handler method by default. *Note:* `handleRequest` is used as the handler entry point as it will call `doHandleRequest`.

## Build the Project

Run shadowJar task: `./gradlew newrelic-lambda-tracer:shadowJar`

Artifact: `newrelic-java-lambda/newrelic-lambda-tracer/build/libs/newrelic-lambda-tracer-all.jar`

## Add Artifacts to Gradle Project

Include the LambdaTracer (newrelic-lambda-tracer-all.jar) and AWS Lambda OpenTracing Java SDK (java-aws-lambda.jar) jars by adding them as dependencies in your `build.gradle` file:

```groovy
dependencies {
    compile files('/path/to/newrelic-lambda-tracer-all.jar')
    compile files('/path/to/java-aws-lambda.jar')
}
```

## Example Usage

```java
package com.handler.example;

import com.amazonaws.services.lambda.runtime.Context;
import io.opentracing.util.GlobalTracer;
import io.opentracing.contrib.aws.TracingRequestHandler;
import com.newrelic.opentracing.LambdaTracer;

import java.util.Map;

/**
 * Tracing request handler that creates a span on every invocation of a Lambda.
 *
 * @param Map<String, Object> The Lambda Function input
 * @param String The Lambda Function output
 */
public class MyLambdaHandler implements TracingRequestHandler<Map<String, Object>, String> {
    static {
        // Register the New Relic OpenTracing LambdaTracer as the Global Tracer
        GlobalTracer.register(LambdaTracer.INSTANCE);
    }

    /**
     * Method that handles the Lambda function request.
     *
     * @param input The Lambda Function input
     * @param context The Lambda execution environment context object
     * @return String The Lambda Function output
     */
    @Override
    public String doHandleRequest(Map<String, Object> input, Context context) {
        // TODO Your function logic here
        return "Lambda Function output";
    }
}
```

## Reporting Errors

The LambdaTracer follows OpenTracing [semantic conventions](https://github.com/opentracing/specification/blob/master/semantic_conventions.md#log-fields-table) when recording error events and traces to [Span Logs](https://opentracing.io/docs/overview/tags-logs-baggage/#logs). The minimum required attributes are `event`, `error.object`, and `message`.

| Log key        | Log type                |                        Note                      | Required |
| :------------: | :---------------------: | :----------------------------------------------: | :------: |
| `event`        | `String` `"error"`      | Indicates that an error event has occurred       |   Yes    |
| `error.object` | `Throwable`             | The `Throwable` object                           |   Yes    |
| `message`      | `Throwable` message     | The detail message string of the throwable       |   Yes    |
| `stack`        | `Throwable` stacktrace  | The the stack trace information of the throwable | Optional |
| `error.kind`   | `String` `"Exception"`  | Indicates that the error was an `Exception`      | Optional |

## Debug Logging

To enable debug logging add the `NEW_RELIC_DEBUG` key to the Lambda environment variable section with the value of `true`. Debug logging entries will be prefixed with `nr_debug` and will show full uncompressed payloads for Span events, Transaction events, and Error events as seen in the example.

##### Example Debug Log Entry

```json
nr_debug: [2,"DEBUG",
{
    "agent_version": "1.0.0",
    "protocol_version": 16,
    "agent_language": "java",
    "execution_environment": "AWS_Lambda_java8",
    "arn": "arn:aws:lambda:us-west-2:121212121212:function:S3ListBuckets",
    "metadata_version": 2
}
,
{
    "span_event_data": [
        null,
        {
            "events_seen": 1,
            "reservoir_size": 1
        },
        [
            [
                {
                    "duration": 8.619987,
                    "traceId": "46421eef76592796",
                    "name": "handleRequest",
                    "guid": "921ab7107eb894e5",
                    "type": "Span",
                    "category": "generic",
                    "priority": 1.162105,
                    "sampled": true,
                    "nr.entryPoint": true,
                    "transactionId": "86e9852546b259e4",
                    "timestamp": 1550171598419
                },
                {
                    "aws.lambda.eventSource.arn": "arn:aws:s3:::example-bucket",
                    "aws.lambda.arn": "arn:aws:lambda:us-west-2:121212121212:function:S3ListBuckets",
                    "aws.lambda.coldStart": true,
                    "aws.requestId": "e778ff53-e9aa-419a-9558-1EXAMPLEf81b"
                },
                {}
            ]
        ]
    ],
    "analytic_event_data": [
        null,
        {
            "events_seen": 1,
            "reservoir_size": 1
        },
        [
            [
                {
                    "duration": 8.619987,
                    "traceId": "46421eef76592796",
                    "name": "WebTransaction/Function/S3ListBuckets",
                    "guid": "86e9852546b259e4",
                    "type": "Transaction",
                    "priority": 1.162105,
                    "sampled": true,
                    "timestamp": 1550171598419
                },
                {
                    "aws.lambda.eventSource.arn": "arn:aws:s3:::example-bucket",
                    "aws.lambda.arn": "arn:aws:lambda:us-west-2:121212121212:function:S3ListBuckets",
                    "aws.lambda.coldStart": true,
                    "aws.requestId": "e778ff53-e9aa-419a-9558-1EXAMPLEf81b"
                },
                {}
            ]
        ]
    ]
}
]
``` 
