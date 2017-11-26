package leader_election;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Scanner;

import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.KeeperException;

public class ZooKeeperTest {
	
	// Private helper class for merge k-sorted arrays
	private static class ArrayPointer implements Comparable<ArrayPointer> {
		private int[] array;
		int arrayIndex;
		
		public ArrayPointer(int[] array, int arrayIndex) {
			this.array = array;
			this.arrayIndex = arrayIndex;
		}
		
		@Override
		public int compareTo(ArrayPointer other) {
			int first = this.array[this.arrayIndex];
			int second = other.array[other.arrayIndex];
			if (first < second) {
				return -1;
			}else if (first > second) {
				return 1;
			}else {
				return 0;
			}
		}
	}
	// ZooKeeper base election path
	private static final String BASE_ELECTION_PATH = "/ELECTION";
	
	// List of references to worker threads
	private static List<ZooKeeperWorker> workers = new ArrayList<>();

	private static int[] mergeSortedArrays(int[][] sortedArrays) {
		PriorityQueue<ArrayPointer> heap = new PriorityQueue<ArrayPointer>();
		int totalSize = 0;
 
		// Add arrays to heap
		for (int i = 0; i < sortedArrays.length; i++) {
			if (sortedArrays[i].length > 0) {
				heap.add(new ArrayPointer(sortedArrays[i], 0));
				totalSize += sortedArrays[i].length;
			}
		}
		
		// Create final merged array and index
		int result[] = new int[totalSize];
		int mergeIndex = 0;
 
		// Add min element from heap while heap is not empty
		while (!heap.isEmpty()){
			ArrayPointer ap = heap.poll();
			result[mergeIndex++] = ap.array[ap.arrayIndex];
 
			if(ap.arrayIndex < ap.array.length - 1){
				heap.add(new ArrayPointer(ap.array, ap.arrayIndex + 1));
			}
		}
 
		return result;
	}
	
	public static void main(String[] args) throws KeeperException,InterruptedException {
		try {
			// Connect to ZooKeeper server to get ZooKeeper object
			ZooKeeper zk = new ZooKeeper("localhost:2181", 3000, null);
			System.out.println("Connected to ZooKeeper server");
			
			/* Below is hard coded test example of sorting 3 sub-arrays and merging
			 * User input is provided as integers delimited by a space, ends with next line
			 * TODO: Design MergeSort input, design test flow, add print/log lines
			 */
			
			// Create and start 3 threads as ZooKeeper workers
			for (int i = 0; i < 3; i++) {
				ZooKeeperWorker worker = new ZooKeeperWorker();
				workers.add(worker);
				new Thread(worker).start();
			}
			
			// Take user input of multiple int
			System.out.println("Enter integers to be sorted delimited by space, press enter when finished");
			Scanner sc = new Scanner(System.in);
			String[] input = sc.nextLine().split(" ");
			sc.close();
			int[] inputArray = new int[input.length];
			for (int i = 0; i < input.length; i++) {
				inputArray[i] = Integer.parseInt(input[i]);
			}
			
			// Split array into 3 parts
			int[][] unsortedArrays = new int[3][];
			for (int i = 0; i < 3; i++) {
				int start = (i == 0) ? 0 : (inputArray.length / 3 * i) + 1;
				int end = (i == 2) ? inputArray.length : (inputArray.length / 3 * (i+1)) + 1;
				unsortedArrays[i] = Arrays.copyOfRange(inputArray, start, end);
			}
			
			// Give sub-arrays to worker threads to sort (no leader election, all znodes are workers)
			// TODO: Maybe not thread safe, might need join() to wait on threads?
			System.out.println("Sending sub-arrays to be sorted by worker threads...");
			int[][] sortedArrays = new int[3][];
			for (int i = 0; i < 3; i++) {
				ZooKeeperWorker worker = workers.get(i);
				worker.setData(unsortedArrays[i]);
				sortedArrays[i] = worker.mergeSort();
			}
			
			// Merge k-sorted arrays and print sorted array
			System.out.println("Finished sorting, printing sorted array");
			int[] sortedArray = mergeSortedArrays(sortedArrays);
			System.out.println(Arrays.toString(sortedArray));
			
			// Shutdown the leader
			System.out.println("Attempting to terminate the leader");
			for (int i = 0; i < workers.size(); i++) {
				if (workers.get(i).isLeader()) {
					workers.get(i).suicide();
					workers.remove(i);
					i--;
					break;
				}
			}
			
			// Wait a little and then exit
			Thread.sleep(2000);
			System.out.println("Terminating program...");
			System.exit(0);
			
		}catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}