CS218 Project 2
Avinash Nanjappan
Dishen Zhao

Leader Election using ZooKeeper

ZooKeeperWorker.java
 - Thread runnable class that creates a ZooKeeper electon znode and acts as a
   worker to perform MergeSort on a given array. Each Worker has a Watcher to
   keep watch the previous znode for its deletion. Detecting its deletion 
   will make the Worker check for new leader (re-election) and set a new
   Watcher.

ZooKeeperWatcher.java
 - Class that extends ZooKeeper API Watcher in order to watch a znode for its
   deletion and inform the Worker listening.

ZooKeeperTest.java
 - Main test example, starts 3 threads with ZooKeeperWorker objects, takes in
   user input for int array, splits array into 3 sub-arrays to send to the 3
   worker threads, each worker sorts returns their given array, and then the
   sorted arrays are merged and printed to stdout. The leader is then deleted
   and after 2 more seconds the program terminates entirely.

Requires ZooKeeper server to be running locally.
