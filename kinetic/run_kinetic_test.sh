#!/bin/sh

set -e

CONNECTION="-p host=169.254.219.193 -p port=8123 -p nio=true -p ssl=false"
OPTIONS="-threads 16"

# Load the database, using workload A’s parameter file (workloads/workloada) and the “-load” switch to the client.
./bin/ycsb load kinetic -P workloads/workloada $CONNECTION -p instantSecureErase=true

# Run workload A (using workloads/workloada and “-t”) for a variety of throughputs.
./bin/ycsb run kinetic -P workloads/workloada $CONNECTION $OPTIONS > workloada.dat

# Run workload B (using workloads/workloadb and “-t”) for a variety of throughputs.
./bin/ycsb run kinetic -P workloads/workloada $CONNECTION $OPTIONS > workloadb.dat

# Run workload C (using workloads/workloadc and “-t”) for a variety of throughputs. 
./bin/ycsb run kinetic -P workloads/workloadc $CONNECTION $OPTIONS > workloadc.dat

# Run workload F (using workloads/workloadf and “-t”) for a variety of throughputs.
./bin/ycsb run kinetic -P workloads/workloadf $CONNECTION $OPTIONS > workloadf.dat

# Run workload D (using workloads/workloadd and “-t”) for a variety of throughputs. This workload inserts records, increasing the size of the database.
./bin/ycsb run kinetic -P workloads/workloadd $CONNECTION $OPTIONS > workloadd.dat

# Reload the database, using workload E’s parameter file (workloads/workloade) and the "-load switch to the client.
./bin/ycsb load kinetic -P workloads/workloade $CONNECTION  -p instantSecureErase=true

# Run workload E (using workloads/workloade and “-t”) for a variety of throughputs. This workload inserts records, increasing the size of the database.
./bin/ycsb run kinetic -P workloads/workloade $CONNECTION $OPTIONS > workloade.dat

# Print the results
echo "WORKLOAD A - Update heavy"
grep "\[OVERALL\]" workloada.dat
echo ""

echo "WORKLOAD B - Ready mostly"
grep "\[OVERALL\]" workloadb.dat
echo ""

echo "WORKLOAD C - Read only"
grep "\[OVERALL\]" workloadc.dat
echo ""

echo "WORKLOAD D - Read latest"
grep "\[OVERALL\]" workloadd.dat
echo ""

echo "WORKLOAD E - Short ranges"
grep "\[OVERALL\]" workloade.dat
echo ""

echo "WORKLOAD F - Read-modfiy-write"
grep "\[OVERALL\]" workloadf.dat
echo ""







