# groovy-graphdb

Source code for the following [blog post](https://groovy.apache.org/blog/groovy-graph-databases).

## Marathon Runner Case Study

The athlete example contains Groovy code for manipulating some historic marathon world records:

![Athletes](docs/images/athletes.graphml.png)

## Backstroke Swimmer Case Study
The swimming example contains Groovy code for manipulating some selected backstroke swimming olympic records:

![Swimmers](docs/images/BackstrokeRecords.png)

## Instructions

All scripts can be run as Gradle tasks. To see available tasks use:

```
$ ./gradlew tasks --group=Application
```

Some scripts require database servers to be already running.
Source files give hints to start such services using docker.

Examine also the respective [GitHub actions](.github/workflows) to see more details of running services with docker
as well as the script output from running all scripts.
