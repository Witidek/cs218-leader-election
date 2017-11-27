package leader_election;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.KeeperException;

public class ZooKeeperTest {
	
	// ZooKeeper base election path
	private static final String BASE_ELECTION_PATH = "/ELECTION";
	
	// ZooKeeper connection object
	private static ZooKeeper zk;
	
	// List of references to worker threads
	private static List<ZooKeeperWorker> workers = new ArrayList<>();
	
	// Print list of commands
	private static void helpCommand() {
		System.out.println("    stat");
		System.out.println("    create [number of nodes to create 1-10]");
		System.out.println("    delete [leader | guid]");
		System.out.println("    sort [number array]");
		System.out.println("    exit");
	}
	
	// Print number of znodes, leader path, and all znode paths
	private static void statCommand() throws KeeperException, InterruptedException {
		if (zk.exists(BASE_ELECTION_PATH, false) == null) {
			System.out.println("/ELECTION path does not exist, there are no znodes");
			return;
		}
		List<String> znodes = zk.getChildren(BASE_ELECTION_PATH, false);
		System.out.println("Number of znodes in ZooKeeper /ELECTION path: " + znodes.size());
		ZooKeeperWorker leader = ZooKeeperWorker.getLeader();
		if (leader != null) {
			System.out.println("The leader is " + leader.getZNodeElectionPath());
		}
		for (String s : znodes) {
			System.out.println(BASE_ELECTION_PATH + "/" + s);
		}
	}
	
	// Create between 1-10 new Workers and znodes
	private static void createCommand(String[] input) {
		int num = 0;
		if (input.length < 2) {
			System.out.println("Usage: create [number of nodes to create 1-50]");
			return;
		}
		try {
			num = Integer.parseInt(input[1]);
		}catch (NumberFormatException e) {
			System.out.println("Usage: create [number of nodes to create 1-50]");
			return;
		}
		if (num < 1 || num > 50) {
			System.out.println("Usage: create [number of nodes to create 1-50]");
			return;
		}
		
		for (int i = 0; i < num; i++) {
			ZooKeeperWorker worker = new ZooKeeperWorker();
			workers.add(worker);
			new Thread(worker).start();
		}
	}
	
	// Delete leader or a specific znode by guid
	private static void deleteCommand(String[] input) throws InterruptedException, KeeperException {
		if (input.length < 2) {
			System.out.println("Usage: delete [leader | guid]");
			return;
		}
		
		if (input[1].equals("leader")) {
			System.out.println("Attempting to terminate the leader");
			ZooKeeperWorker leader = ZooKeeperWorker.getLeader();
			if (leader != null) {
				leader.suicide();
			}
		}else {
			// Construct full znode path with padded zeroes
			StringBuilder build = new StringBuilder();
			build.append(BASE_ELECTION_PATH);
			build.append("/guid-n_");
			for (int i = 0; i < 10 - input[1].length(); i++) {
				build.append("0");
			}
			build.append(input[1]);
			String path = build.toString();
			
			// Iterate through workers to find deletion target
			for (int i = 0; i < workers.size(); i++) {
				ZooKeeperWorker w = workers.get(i);
				if (w.getZNodeElectionPath().equals(path)) {
					w.suicide();
					workers.remove(i);
					return;
				}
			}
			System.out.println(path + " was not found for deletion");
		}
	}
	
	// Send list of workers and input to leader to deal with
	private static void sortCommand(String[] input) {
		// Check number of workers
		int numberOfWorkers = workers.size();
		if (numberOfWorkers < 2) {
			System.out.println("Error: There needs to be at least 2 workers in ZooKeeper to help sort!");
			return;
		}
		
		// Parse input array
		int[] inputArray = new int[input.length - 1];
		for (int i = 1; i < input.length; i++) {
			inputArray[i-1] = Integer.parseInt(input[i]);
		}
		
		// Send list of workers and input array to leader, have leader split the work amongst workers
		ZooKeeperWorker leader = ZooKeeperWorker.getLeader();
		int[] sortedArray = leader.leaderSort(workers, inputArray);
		
		// Print returned result
		System.out.println("Finished sorting, printing sorted array");
		System.out.println(Arrays.toString(sortedArray));
	}
	
	// Wait a little and then exit
	private static void exitCommand() throws InterruptedException {
		System.out.println("Terminating program...");
		Thread.sleep(1000);
		System.exit(0);
					
	}
	
	public static void main(String[] args) throws KeeperException,InterruptedException {
		try {
			// Connect to ZooKeeper server to get ZooKeeper object
			zk = new ZooKeeper("localhost:2181", 3000, null);
			System.out.println("Connected to ZooKeeper server");
			
			// Take user input
			System.out.println("Use command 'help' for a list of commands.");
			System.out.print("ZooKeeperTest$ ");
			Scanner sc = new Scanner(System.in);
			while (sc.hasNextLine()) {
				
				String[] input = sc.nextLine().split(" ");
				
				if (input.length < 1) {
					continue;
				}
				// Commands
				switch (input[0]) {
				case "help":
					helpCommand();
					break;
				case "create":
					createCommand(input);
					break;
				case "delete":
					deleteCommand(input);
					break;
				case "sort":
					sortCommand(input);
					break;
				case "stat":
					statCommand();
					break;
				case "quit":
				case "exit":
					sc.close();
					exitCommand();
					break;
				default:
					System.out.println("Please enter a valid command.");
					break;
				}
				
				// Delay to make ordering a little nicer
				Thread.sleep(500);
				System.out.print("ZooKeeperTest$ ");
			}	
			// Terminate program if scanner reached end of input
			sc.close();
			System.out.println();
			exitCommand();
		}catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}
