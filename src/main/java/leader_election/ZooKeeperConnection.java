package leader_election;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;

public class ZooKeeperConnection {

	// Declare ZooKeeper instance to access ZooKeeper ensemble
	private ZooKeeper zoo;
	final CountDownLatch connectedSignal = new CountDownLatch(1);

	// Method to connect ZooKeeper ensemble, returns actual ZooKeeper object
	public ZooKeeper connect(String host) throws IOException,InterruptedException {
	
		zoo = new ZooKeeper(host,5000,new Watcher() {
			public void process(WatchedEvent we) {
				if (we.getState() == KeeperState.SyncConnected) {
					connectedSignal.countDown();
				}
			}
		});
		
		connectedSignal.await();
		return zoo;
	}

	// Method to disconnect from ZooKeeper server
	public void close() throws InterruptedException {
		zoo.close();
	}
}