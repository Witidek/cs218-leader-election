package leader_election;

import java.util.Arrays;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

public class ZooKeeperWorker implements Runnable {
	// ZooKeeper base election path
	private static final String BASE_ELECTION_PATH = "/ELECTION";
	
	// This thread's election znode path
	private String zNodeElectionPath;
	
	// Create instance for ZooKeeperConnection class
	private ZooKeeperConnection conn;
	
	// Create instance for ZooKeeper class
	private ZooKeeper zk;
		
	// MergeSort array
	private int[] data;

	// Constructor
	public ZooKeeperWorker() {
		//
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
	
	// Create election znode for this thread
	private void createWorkerNode() throws KeeperException, InterruptedException {
		// Create base election znode path if does not exist
		if (zk.exists(BASE_ELECTION_PATH, false) == null) {
			try {
				zk.create(BASE_ELECTION_PATH, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}catch (KeeperException.NodeExistsException e) {
				System.out.println("Base /ELECTION znode creation failed: Node already exists.");
			}
		}
		zNodeElectionPath = zk.create(BASE_ELECTION_PATH + "/guid-n_", null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
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
	
	public void run() {
		try {
			/* Connects to ZooKeeper and creates election znode
			 * TODO: Leader election, watch previous znode, communicate with leader
			 */
			
			// Connect to ZooKeeper server to get ZooKeeper object
			conn = new ZooKeeperConnection();
			zk = conn.connect("localhost");
			
			// Create election znode in ZooKeeper for this thread
			createWorkerNode();
			System.out.println("Created new ZooKeeper worker thread with path: " + zNodeElectionPath);
		}catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}

