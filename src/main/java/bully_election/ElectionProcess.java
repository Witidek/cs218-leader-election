package bully_election;

import java.util.concurrent.locks.ReentrantLock;

public class ElectionProcess {
	// Locks
	public static ReentrantLock pingLock = new ReentrantLock();
	public static ReentrantLock electionLock = new ReentrantLock();
	
	// Flags to allow election and ping
	private static boolean currentElection = false;
	private static boolean allowPing = true;
	
	// Initial node that discovered leader failed
	private static NodeInstance electionDetector;

	public static NodeInstance getElectionDetector() {
		return electionDetector;
	}

	public static void setElectionDetector(NodeInstance electionDetector) {
		ElectionProcess.electionDetector = electionDetector;
	}

	public static boolean isAllowPing() {
		return allowPing;
	}

	public static void setAllowPing(boolean flag) {
		ElectionProcess.allowPing = flag;
	}

	public static boolean isCurrentElection() {
		return currentElection;
	}

	public static void setCurrentElection(boolean flag) {
		ElectionProcess.currentElection = flag;
	}

	public static void initialElection(ThreadProcess[] nodes) {
		NodeInstance temp = new NodeInstance(-1, -1);
		for (int i = 0; i < nodes.length; i++) {
			if (nodes[i].getProcess().getNodePriority() > temp.getNodePriority()) {
				temp = nodes[i].getProcess();
			}
		}

		temp.setIsLeader(true);
		System.out.println("NEW LEADER: Node[" + temp.getNodeID() + "]");
	}
}
