package leader_election;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

public class ZooKeeperWorker implements Watcher, Runnable, ZooKeeperWatcher.ZooKeeperListener {
	// ZooKeeper base election path
	private static final String BASE_ELECTION_PATH = "/ELECTION";
	
	// This thread's election znode path
	private String zNodeElectionPath;
	
	// Leader's election znode path
	private String leaderElectionPath;
	
	// Flag that shows if this worker is leader or regular worker
	private boolean isLeader;
	
	// Create instance for ZooKeeper class
	private ZooKeeper zk;
	
	// Watches previous znode
	ZooKeeperWatcher watcher;
	
	// Suicide flag
	boolean shutdown;
		
	// MergeSort array
	private int[] data;

	// Constructor
	public ZooKeeperWorker() {
		//
	}
	
	// Returns true if this worker is leader
	public boolean isLeader() {
		return this.isLeader;
	}
	// Getter for MergeSort array
	public int[] getData() {
		return data;
	}
	
	// Setter for MergeSort array
	public void setData(int[] data) {
		this.data = data;
	}
	
	// MergeSort data array and return sorted array
	public int[] mergeSort() {
		if (data.length > 1) {
			sort(0, data.length-1);
		}
		System.out.println(zNodeElectionPath + " finished sorting:");
		System.out.println(Arrays.toString(data));
		return data;
	}
	
	// MergeSort divide & conquer recursive method
	private void sort(int l, int r) {
		if (l < r) {
			int m = (l + r) / 2;
			sort(l, m);
			sort(m+1, r);
			merge(l, m, r);
		}
	}
	
	// MergeSort merge method
	private void merge(int l, int m, int r) {
		// Create and fill sub-arrays for comparison
		int leftSize = m - l + 1;
		int rightSize = r - m;
		int left[] = new int[leftSize];
		int right[] = new int[rightSize];
		
		for (int i = 0; i < leftSize; i++) {
			left[i] = data[l + i];
		}
		for (int i = 0; i < rightSize; i++) {
			right[i] = data[m + 1 + i];
		}
		
		// Merge in order into original array
		int leftIndex = 0;
		int rightIndex = 0;
		int mergeIndex = l;
		
		// Compare and fill with smaller of two elements
		while (leftIndex < leftSize && rightIndex < rightSize) {
			if (left[leftIndex] <= right[rightIndex]) {
				data[mergeIndex] = left[leftIndex];
				leftIndex++;
			}else {
				data[mergeIndex] = right[rightIndex];
				rightIndex++;
			}
			mergeIndex++;
		}
		
		// Fill remaining elements from left
		while (leftIndex < leftSize) {
			data[mergeIndex] = left[leftIndex];
			leftIndex++;
			mergeIndex++;
		}
		
		// Fill remaining elements from right
		while (rightIndex < rightSize) {
			data[mergeIndex] = right[rightIndex];
			rightIndex++;
			mergeIndex++;
		}
	}
	
	// Create election znode for this thread
	private void createWorkerNode() throws KeeperException, InterruptedException {
		// Create base election znode path if does not exist
		if (zk.exists(BASE_ELECTION_PATH, false) == null) {
			try {
				zk.create(BASE_ELECTION_PATH, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}catch (KeeperException.NodeExistsException e) {
				System.out.println("ZooKeeperWorker::createWorkerNode(): Base /ELECTION znode creation failed: Node already exists.");
			}
		}
		zNodeElectionPath = zk.create(BASE_ELECTION_PATH + "/guid-n_", null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
		System.out.println("Created new ZooKeeper worker thread with path: " + zNodeElectionPath);
	}
	
	// Find and save path to leader znode, check if this worker is leader
	private void checkLeader() throws KeeperException, InterruptedException {
		// Get all znodes in election path
		List<String> znodes = zk.getChildren(BASE_ELECTION_PATH, false);
		
		// Empty election path error
		if (znodes.isEmpty()) {
			System.out.println("ZooKeeperWorker::checkLeader(): No znodes found in \"/ELECTION\" path!");
			isLeader = false;
			return;
		}
		
		// Leader is smallest guid znode
		leaderElectionPath = BASE_ELECTION_PATH + "/" + Collections.min(znodes);
		if (zNodeElectionPath.equals(leaderElectionPath)) {
			System.out.println(zNodeElectionPath + " is the new leader!");
			isLeader = true;
		}else {
			isLeader = false;
		}
	}
	
	// Set ZooKeeperWatcher to watch previous node
	private void setWatcher() throws KeeperException, InterruptedException {
		String zNodePathToWatch = null;
		
		// Set watcher to self if this is leader, else find the previous znode
		if (isLeader) {
			zNodePathToWatch = zNodeElectionPath;
		}else {
			List<String> znodes = zk.getChildren(BASE_ELECTION_PATH, false);
			for (String znode : znodes) {
				
				// Extend to full path for comparison
				String path = BASE_ELECTION_PATH + "/" + znode;
				
				// Set new best path if lesser than this znode and greater than old best path (or null)
				if (path.compareTo(zNodeElectionPath) < 0 && (zNodePathToWatch == null || path.compareTo(zNodePathToWatch) > 0)) {
					zNodePathToWatch = path;
				}
			}
			System.out.println(zNodeElectionPath + " is watching " + zNodePathToWatch);
		}
		// Create new ZooKeeperWatcher with ZooKeeper object, path of znode to watch, and this znode
		watcher = new ZooKeeperWatcher(zk, zNodePathToWatch, this);
	}
	
	// Thread main logic
	public void run() {
		try {
			// Connects to ZooKeeper and creates election znode
			zk = new ZooKeeper("localhost:2181", 3000, this);
			
			// Create election znode in ZooKeeper for this thread
			createWorkerNode();
			checkLeader();
			setWatcher();

			while (!shutdown) {
				// Stay alive and running until shutdown flag is set
			}
		}catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	// The znode being watched was deleted, this znode will check leader and set new node to watch
	@Override
	public void onNodeDeleted() {
		// Do not continue if the leader detecting its own deletion
		if (watcher.getZNodeWatchPath().equals(zNodeElectionPath)) {
			return;
		}
		System.out.println(zNodeElectionPath + " detected that " + watcher.getZNodeWatchPath() + " was deleted!");
		try {
			checkLeader();
			setWatcher();
		}catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	// Watcher callback
	@Override
	public void process(WatchedEvent event) {
		watcher.process(event);
	}
	
	// Stop thread by deleting this worker's znode and end the run() method by setting shutdown flag
	public void suicide() throws InterruptedException, KeeperException {
		System.out.println(zNodeElectionPath + " deleting znode and shutting down");
		zk.delete(zNodeElectionPath, -1);
		shutdown = true;
	}
}

