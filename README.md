Configuration explanations
=============
The configuration file is under YCSB/workloads/workloada, also you can add workload* as you needed.

1. fieldcount=1      --------  Do not modify it.
2. hosts             --------  Replace it with your drive's ip and port. The format is ip1:port1;ip2:port2;ip3:port3;....
3. connectionpernode --------  Replace it with your connection number for one client.
4. fieldlength       --------  Replace it with value's size, now the default is 1MB=1048576B.
5. recordcount       --------  Number of generation key. Replace it with how many key samples you wanted to generate. 
6. operationcount    --------  Number of operation key selected from recordcount. Replace it with how many keys you wanted to operate to your drive. 
7. workload          --------  Do not modify it.
8. readallfields     --------  Do not modify it.
9. readproportion    --------  Replace it with the proportion of read operation you wanted to test.
10. updateproportion --------  Replace it with the proportion of update operation you wanted to test.
11. scanproportion   --------  Replace it with the proportion of scan operation you wanted to test. Scan operation not implement until now.
12. insertproportion --------  Replace it with the proportion of put operation you wanted to test.

Run script to test performance
============
1. $ cd YCSB/
2. $ mvn clean package
3. $ ./bin/ycsb-kinetic.sh -P "workload path" -threads "your threads number".

   For instance:

   $ ./bin/ycsb-kinetic.sh -P workloads/workloada -threads 10

