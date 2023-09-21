# _TraceUpscaler_

_TraceUpscaler_ is an offline tool to upscale user traffic trace (by increasing load) from open-loop latency sensitive applications.

## Requirements

The source code requires to be compiled with `Java 17`, (we used `Java Corretto 17.0.3`).
The code also uses the following libraries:
```
Apache Commons Lang 3.12.0
Apache Commons Math 3.6.1
Google Gson 2.7
```

## How to run

Please open directory `TraceUpscaler` as a gradle project in your choice of IDE (we used `IntelliJ IDEA`), and run the `Test.java` file in the `org.example` package from the IDE, by changing the values of the following variables:
> __sourceTracePath__: In the main method, change the value to the absolute path of the source trace file.

> __destTracePath__:  In the main method, change the value to the destination path of the upscaled trace file.

> __scalingFactor__:  In the main method, change the value to the desired upscaling factor. 

 



For ease of running _TraceUpscaler_, we have added a directory `test` which contains a jar file, `TraceUpscaler.jar` which was created from the  `TraceUpscalerRunner.java` file, with the required parameters:

> __param-1__: filepath of the trace to be upscaled (txt file)

> __param-2__: filepath of the destination of the upscaled trace (txt file)

>__param-3__: a float, which denotes the upscaling factor We also added a sample trace file, `source_trace.txt` for testing purposes. The jar file can be run with the following command format:

```
java -jar TraceUpscaler.jar param-1 param-2 param-3 
```

This should run with `Java 17+`. Please follow the following commands to run it:

```
cd test


# to store the upscaled trace in
# a file called 'upscaled_trace.
#txt' from the file 'source_trace.txt',
# where the upscaling factor is 2

java -jar TraceUpscaler.jar source_trace.txt upscaled_trace.txt 2

```

## Trace Format

The trace format used in this code is the following:

```
InitialStartTime;;;ActualStartTime;;;UserNumber;;;EndTime;;;Request-1;;Request-2;;..;;Request-N
```

Here is a short detail on each:

- InitialStartTime: The exact time in nanosecond when the request is supposed to arrive for servicing.
- ActualStartTime: The exact time in nanosecond when the request arrives for servicing. The difference between this and the InitialStartTime is the queueing delay.
- UserNumber: An integer which works as identifier for different users.
- EndTime: The exact time in nanosecond when the request has been served and out of the server.
- Request-X: Contains information about the request. This can change depending on the type of application we are working with. The different components of requests are separated by ';'.

