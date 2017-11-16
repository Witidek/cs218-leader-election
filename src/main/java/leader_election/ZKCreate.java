package leader_election;

import java.io.IOException;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;

public class ZKCreate {
   // Create static instance for zookeeper class.
   private static ZooKeeper zk;

   // Create static instance for ZooKeeperConnection class.
   private static ZooKeeperConnection conn;

   // Method to create znode in zookeeper ensemble
   public static void create(String path, byte[] data) throws KeeperException,InterruptedException {
	   zk.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
   }

	public static void main(String[] args) {
		// Example znode path
		String path = "/MyFirstZnode"; // Assign path to znode
	
		// String to byte array for data
		byte[] data = "My first zookeeper app".getBytes(); // Declare data
			
		try {
			// Connect to zookeeper server to get zookeeper object
			conn = new ZooKeeperConnection();
			zk = conn.connect("localhost");
			
			// Create the znode and close connection
			create(path, data);
			conn.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}