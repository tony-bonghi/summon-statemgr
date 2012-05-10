package com.proquest.magnolia.statemgr.zkclient;

import com.proquest.magnolia.statemgr.common.ZKConstants;
import org.apache.log4j.Logger;
import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;

/**
 * Monitors the data and existence of a zk node.
 */
public class DataMonitor implements Watcher, StatCallback, ZKConstants {

  private Logger logger = Logger.getLogger(DataMonitor.class.getName());
  
  private ZKClient zk;
  private ZKProcess zkProc;
  private DataMonitorListener listener;
  private String prevData = "";
  boolean isDead;

  /**
   * Constructor.
   * @param zk        ZKClient reference
   * @param zkProc    The ZK Process object containing the nodes to watch
   * @param listener  The DataMonitorListener used to callback with notifications
   */
  public DataMonitor(ZKClient zk, ZKProcess zkProc, DataMonitorListener listener) {
    this.zk = zk;
    this.zkProc = zkProc;
    this.listener = listener;
    // Get things started by checking if the node exists. We are going
    // to be completely event driven
    zk.getZookeeper().exists(zkProc.getDependencyStateNode(), true, this, null);
    zk.getZookeeper().exists(zkProc.getSubNode(NODE_STATE), true, this, null);
    zk.getZookeeper().exists(NODE_MASTER, true, this, null);
  }

  /**
   * Other classes use the DataMonitor by implementing this method
   */
  public interface DataMonitorListener {
    /**
     * The existence status of the node has changed.
     */
    void exists(String path, byte data[]);

    /**
     * The ZooKeeper session is no longer valid.
     *
     * @param rc  The ZooKeeper reason code
     */
    void closing(int rc);
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
      if (zkProc.getSubNode(NODE_STATE).equals(path)) {
        zk.getZookeeper().exists(zkProc.getSubNode(NODE_STATE), true, this, null);
      } else if (NODE_MASTER.equals(path)) {
        zk.getZookeeper().exists(NODE_MASTER, true, this, null);
      } else if (zkProc.getDependencyStateNode().equals(path)) {
        zk.getZookeeper().exists(zkProc.getDependencyStateNode(), true, this, null);
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
        zk.getZookeeper().exists(zkProc.getSubNode(NODE_STATE), true, this, null);
        return;
    }

    if (exists) {
      String d = zk.getData(path);
      if ((d == null && d != prevData) || (d != null && !d.equals(prevData))) {
        logger.info(String.format("Calling listener for node data change {node=[%s], data=[%s]}", path, d));
        listener.exists(path, d.getBytes());
        prevData = d;
      }
    }
  }
}