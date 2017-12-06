CS218 Project 2
Avinash Nanjappan
Dishen Zhao

Leader Election using ZooKeeper
/src/main/java/leader_election/

Requires ZooKeeper server to be running locally.
Build with maven to include ZooKeeper libaries and run ZooKeeperTest.

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


Leader Election using Bully Algorithm
/src/main/java/bully_election/

No external libraries required, run ElectionTest.

ElectionTest.java
 - Main program to start the simulation. Accepts user input to create number
   of nodes and starts the thread for each node to run on their own.

ElectionProcess.java
 - Static class to help with election and status check pings. Selects the
   initial leader when the system is first started up. Holds the two flags
   to signal whether there is an election running currently, and when to
   allow pinging the leader. Also has the two locks to go along to ensure
   only one node acts at a time for each.

ThreadProcess.java
 - Thread runnable class that exists as a node in the system. Each node
   either acts as a leader or a regular node. There is only one leader at
   a time, and it accepts incoming connections from all other nodes. Regular
   nodes simulate some work by waiting and on completion pings the leader
   to check if it's alive. Re-election is done by messaging higher nodes
   to check for their status and the highest available becomes leader. 
   Recovery from failure is simulated and nodes will check if they can bully
   the leader. The leader and non-leader nodes all simulate a failure after
   random amount of work performed.
