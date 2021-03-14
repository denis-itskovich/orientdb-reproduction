## Description
This repository reproduces potential OrientDB bug, reproducible in multithreaded environment

### Environment
The bug is reproducible when working with OrientDB remotely. 
It could not be reproduced in embedded mode.

Also, the bug could not be reproduced in single threaded environment

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
