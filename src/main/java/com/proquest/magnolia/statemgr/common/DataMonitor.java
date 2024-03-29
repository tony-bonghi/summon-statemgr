package com.proquest.magnolia.statemgr.common;

import com.proquest.magnolia.statemgr.zkclient.ZKClient;
import org.apache.log4j.Logger;
import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Monitors the data and existence of a zk node.
 */
public class DataMonitor implements Watcher, StatCallback, ZKConstants {

  private Logger logger = Logger.getLogger(DataMonitor.class.getName());
  
  private ZKClient zk;
  private List<String> znodes;
  private DataMonitorListener listener;
  private Map<String,String> prevPathDataMap = new HashMap<String, String>();
  boolean isDead;

  /**
   * Constructor.
   * @param zk        ZKClient reference
   * @param znodes    Array of z-nodes to watch
   * @param listener  The DataMonitorListener used to callback with notifications
   */
  public DataMonitor(ZKClient zk, List<String> znodes, DataMonitorListener listener) {
    this.zk = zk;
    this.znodes = znodes;
    this.listener = listener;
    // Get things started by checking if the node exists. We are going
    // to be completely event driven
    for (String znode : znodes) {
      zk.getZookeeper().exists(znode, true, this, null);
    }
  }

  /**
   * Other classes use the DataMonitor by implementing this method
   */
  public interface DataMonitorListener {
    /**
     * The existence status of the node has changed.
     */
    void exists(String path, String data);

    /**
     * The ZooKeeper session is no longer valid.
     *
     * @param rc  The ZooKeeper reason code
     */
    void closing(int rc);
  }

  public boolean isDead() {
    return isDead;
  }

  /**
   * Process events handed out by the ZK service.
   * @param event The event
   */
  public void process(WatchedEvent event) {
    String path = event.getPath();
    if (event.getType() == Event.EventType.None) {
      // We are are being told that the state of the
      // connection has changed
      switch (event.getState()) {
        case SyncConnected:
          // In this particular example we don't need to do anything
          // here - watches are automatically re-registered with
          // server and any watches triggered while the client was
          // disconnected will be delivered (in order of course)
          break;
        case Expired:
          // It's all over
          logger.warn("The session has expired.");
          isDead = true;
          listener.closing(KeeperException.Code.SessionExpired);
          break;
      }
    } else {
      // Something has changed on the node, let's find out
      for (String znode : znodes) {
        if (znode.equals(path)) {
          zk.getZookeeper().exists(znode, true, this, null);
        }
      }
    }
  }

  /**
   * Handler to process the node and data updates. If applicable, the listener
   * interface will be called to do the actual work.
   * @param rc
   * @param path
   * @param ctx
   * @param stat
   */
  public void processResult(int rc, String path, Object ctx, Stat stat) {
    boolean exists = false;
    switch (rc) {
      case Code.Ok:
        logger.debug(String.format("Node found {node=[%s]}", path));
        exists = true;
        break;
      case Code.NoNode:
        logger.warn(String.format("Node not found {node=[%s]}", path));
        exists = false;
        break;
      case Code.SessionExpired:
      case Code.NoAuth:
        logger.warn("Session expired");
        isDead = true;
        listener.closing(rc);
        return;
      default:
        logger.info("Retry errors");
        for (String znode : znodes) {
          zk.getZookeeper().exists(znode, true, this, null);
        }
        return;
    }

    if (exists) {
      String d = zk.getData(path);
      String prevData = prevPathDataMap.get(path);
      if ((d == null && d != prevData) || (d != null && !d.equals(prevData))) {
        logger.info(String.format("Calling listener for node data change {node=[%s], data=[%s]}", path, d));
        listener.exists(path, d);
        prevPathDataMap.put(path, d);
      }
    }
  }
}