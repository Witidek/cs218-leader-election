package bully_election;

public class NodeInstance {

	// ID and priority
	private int nodeID;
	private int nodePriority;

	// Booleans
	private boolean isDown; 
	private boolean isLeader;

	public boolean isLeader() {
		return isLeader;
	}

	public void setIsLeader(boolean flag) {
		this.isLeader = flag;
	}

	public boolean isDown() {
		return isDown;
	}

	public void setIsDown(boolean flag) {
		this.isDown = flag;
	}

	public int getNodeID() {
		return nodeID;
	}

	public void setNodeID(int nodeID) {
		this.nodeID = nodeID;
	}

	public int getNodePriority() {
		return nodePriority;
	}

	public void setNodePriority(int nodePriority) {
		this.nodePriority = nodePriority;
	}

	// Constructor initializes the values for the above specified variables
	public NodeInstance(int nodeID, int nodePriority) {
		this.nodeID = nodeID;
		this.isDown = false;
		this.nodePriority = nodePriority;
		this.isLeader = false;
	}
}
