CS218 Project 2
Avinash Nanjappan
Dishen Zhao

Leader Election using ZooKeeper

ZooKeeper 3.4.10

ZooKeeperConnection.java
 - Helper class to establish a connection to ZooKeeper server

ZooKeeperWorker.java
 - Thread runnable class that creates a ZooKeeper electon znode and acts as a
   worker to perform MergeSort on a given array

ZooKeeperTest.java
 - Main test example, starts 3 threads with ZooKeeperWorker objects, takes in
   user input for int array, splits array into 3 sub-arrays to send to the 3
   worker threads, each worker sorts returns their given array, and then the
   sorted arrays are merged and printed to stdout.

Requires ZooKeeper server to be running locally.
