## Description
This repository reproduces potential in OrientDB when using live queries.

## Environment
The bug is reproducible when working with OrientDB remotely and only when running
live queries simultaneously from different threads.

It could not be reproduced in embedded mode.
The test uses `docker-compose` in order to run `OrientDB` container

## Description

### Steps to reproduce

1. Ensure `docker` and `docker-compose` are installed
2. Run `./gradlew test`

This will run `testMultithreadedLiveQuery`

### Test case:
| Thread    | Actions                                                               |
|-----------|-----------------------------------------------------------------------|
| `Main`    | Create DB                                                             |
| `Main`    | Create class `ClassA`                                                 |
| `Main`    | Create class `ClassB`                                                 |
| `Thread1` | live query `select from ClassA`                                       |
| `Thread2` | live query `select from ClassB`                                       |
| `Main`    | insert `company: {name: "A1"}`                                        |
| `Main`    | `assert:` received 1 notification of `ClassA` by listener of `ClassA` |

### Listener
The listener checks that the `create` notification of queried class corresponds 
the `@class` property in notification. Otherwise, error is printed to stderr

### An expected outcome:
Listener of `ClassA` should receive 1 create notification for `ClassA`

### An actual outcome:
Sometimes (~50% of the runs), listener of `ClassB` receives create notification of `ClassA`

## Output example:

```
Subscribing for ClassA from thread: pool-1-thread-1
Subscribing for ClassB from thread: pool-1-thread-2
<ClassB> listener received notification: {"name": "A1", "@rid": "#33:0", "@class": "ClassA", "@version": 1}
Received incorrect class notification! Expected class: <ClassB> actual: <ClassA>
```
