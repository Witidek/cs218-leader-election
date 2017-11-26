CS218 Project 2
Avinash Nanjappan
Dishen Zhao

Leader Election using ZooKeeper

ZooKeeperWorker.java
 - Thread runnable class that creates a ZooKeeper electon znode and acts as a
   worker to perform MergeSort on a given array. Each Worker has a Watcher to
   keep watch the previous znode for its deletion. Detecting its deletion 
   will make the Worker check for new leader (re-election) and set a new
   Watcher. The leader is in charge of splitting input array and dividing
   sorting subarrays to other Workers and then merging the result.

ZooKeeperWatcher.java
 - Class that extends ZooKeeper API Watcher in order to watch a znode for its
   deletion and inform the Worker listening.

ZooKeeperTest.java
 - Main interactive test suite/simulator. Connects to ZooKeeper server and
   is able to create worker/znodes, delete worker/znodes, sort number arrays,
   and return ZooKeeper worker/znode stats. All workers/znodes are destroyed
   upon program termination.

Requires ZooKeeper server to be running locally.
