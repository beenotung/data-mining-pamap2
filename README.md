# Data mining on Physical Activity
Data source :
http://archive.ics.uci.edu/ml/datasets/PAMAP2+Physical+Activity+Monitoring

## data mining techies
1. Association Rule Mining
2. (useful?) feature selection (generic algorithm)

### Association Rule Mining
1. prepare data
   1. import the raw data from file into database
   2. pre-process raw data
      - calc relative motion (whole body)
         - average of three motion sensor
      - calc absolute direction
         - using the magnetometer to calibrate
      - calc relative temperature
         - against the three IMU sensor
      - this part can be done in parallel
         - dispatcher
           1. get list of timestamps
           2. send to workers
         - worker
           1. receive timestamp (UID) from dispatcher
           2. fetch raw from database
           3. calc extra data
           3. save extra data to database
2. learn from data
   1. find association rule
      1. find single attr asso rules
      2. combination attrs
      - this part can be done in parallel
3. test the learning
   - apply the association rule to predicate
   - calculate the correctness
   - this part can be done in parallel
4. analysis & report writing

# Akka & Docker

For a detailed description read [this blog entry](http://mukis.de/pages/akka-cluster-with-docker-containers/).

```bash
sbt docker:publishLocal
docker run --name seed-1 akka-docker:2.3.4 --seed
docker run --name seed-2 akka-docker:2.3.4 --seed <ip-of-your-seed-1>:2551
docker run --name node-1 akka-docker:2.3.4 <ip-of-your-seed-1>:2551 <ip-of-your-seed-2>:2551
docker run --name node-2 akka-docker:2.3.4 <ip-of-your-seed-1>:2551 <ip-of-your-seed-2>:2551
```

# SBT - none docker

Of course you can run your cluster within sbt for test purposes.

```
sbt runSeed
sbt runNode
```

# Setup Activator

1. [Download Typesafe Activator](http://typesafe.com/platform/getstarted) (or copy it over from a USB)
2. Extract the zip and run the `activator` or `activator.bat` script from a non-interactive shell
3. Your browser should open to the Activator UI: [http://localhost:8888](http://localhost:8888)

# Links and References

* [Akka Docker Cluster Example Blog](http://blog.michaelhamrah.com/2014/03/running-an-akka-cluster-with-docker-containers/)
* [Akka Docker Cluster Example Github](https://github.com/mhamrah/akka-docker-cluster-example)
* [Docker Networking](https://docs.docker.com/articles/networking/)
* [Docker Cheat Sheet](https://github.com/wsargent/docker-cheat-sheet)
* [Docker Env Variables](http://mike-clarke.com/2013/11/docker-links-and-runtime-env-vars/)
* [Docker Ambassador Pattern Linking](http://docs.docker.com/articles/ambassador_pattern_linking/)
