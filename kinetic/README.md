Overview
========

`com.yahoo.ycsb.db.KineticClient` implements a DB provider for YCSB. Since Kinetic doesn't natively support dictionary
values it transparently (de)serializes given dictionaries using native Java serialization.

Compiling
=========

To compile and run tests run `mvn clean package` and then `./bin/ycsb` as with any other YCSB supported database. The
only thing to note is that since a public Kinetic client Maven repo isn't available yet the Kinetic client must
already be installed in the local maven repository. This can be done by running `mvn install` from the Kinetic
simulator directory

Running Benchmarks
==================

Running YCSB against Kiometer is no different than running for any other supported database. For convenience we provide
the `run_kinetic_test.sh` script. It can be modified to specify the host and will run the recommended sequence of
workloads. The raw results are piped to `workloadX.dat` and a summary is printed at the end

Interactive Shell
=================

To use YCSB's interactive shell for verifying the DB implementation run something like
`/bin/ycsb shell kinetic -p host=169.254.219.193`