package leader_election;

import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class ZooKeeperWatcher implements Watcher, StatCallback {
	// ZooKeeper object
	private ZooKeeper zk;

	// Path to znode being watched
	private String zNodeWatchPath;
	
	// Listener object watching this
	private ZooKeeperListener listener;
	
	// Listener interface, implemented in Worker
	public interface ZooKeeperListener {
		
		// Tell listener that the node being watched was deleted
		void onNodeDeleted();
	}
	
	// Return zNodeWatchPath
	public String getZNodeWatchPath() {
		return zNodeWatchPath;
	}
	
	// Constructor
	public ZooKeeperWatcher(ZooKeeper zk, String zNodeWatchPath, ZooKeeperListener listener) {
	    this.zk = zk;
	    this.zNodeWatchPath = zNodeWatchPath;
	    this.listener = listener;
	    
	    // Initial check to see if znode exists asynchronously
	    checkZNode();
	}
	
	private void checkZNode() {
		zk.exists(zNodeWatchPath, true, this, null);
	}
	
	@Override
	public void process(WatchedEvent event) {
		// Watcher reporting watched node was deleted, inform listener
		if (EventType.NodeDeleted.equals(event.getType())) {
			if (event.getPath().equalsIgnoreCase(zNodeWatchPath)) {
				listener.onNodeDeleted();
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void processResult(int rc, String path, Object ctx, Stat stat) {
	    switch (rc) {
	    case Code.Ok:
	        break;
	    case Code.NoNode:
	        listener.onNodeDeleted();
	        break;
	    case Code.SessionExpired:
	    case Code.NoAuth:
	        //onSessionClosed();
	        return;
	    default:
	        // Retry errors
	    		checkZNode();
	        return;
	    }	
	}
}
