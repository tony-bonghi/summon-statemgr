package com.proquest.magnolia.statemgr.common;

import org.apache.log4j.Logger;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 */
public class ZKClientBase implements Watcher {

  private static final Logger logger = Logger.getLogger(ZKClientBase.class);

  protected final ZooKeeper zookeeper;
  private AtomicBoolean closed = new AtomicBoolean(false);
  private long retryDelay = 500L;
  private int retryCount = 10;
  private List<ACL> acl = ZooDefs.Ids.OPEN_ACL_UNSAFE;
  private Set<Watcher> watchers = new HashSet<Watcher>();

  public ZKClientBase(String zkConnection) throws IOException {
    this.zookeeper = new ZooKeeper(zkConnection, 30000, this);
  }

  /**
   * Closes this strategy and releases any ZooKeeper resources; but keeps the
   *  ZooKeeper instance open
   */
  public void close() {
    if (closed.compareAndSet(false, true)) {
      doClose();
    }
  }

  /**
   * return zookeeper client instance
   * @return zookeeper client instance
   */
  public ZooKeeper getZookeeper() {
    return zookeeper;
  }

  /**
   * get the retry delay in milliseconds
   * @return the retry delay
   */
  public long getRetryDelay() {
    return retryDelay;
  }

  /**
   * Sets the time waited between retry delays
   * @param retryDelay the retry delay
   */
  public void setRetryDelay(long retryDelay) {
    this.retryDelay = retryDelay;
  }

  /**
   * Allow derived classes to perform
   * some custom closing operations to release resources
   */
  protected void doClose() {
  }

  /**
   * Creates a node with the specified data. If the node already exists, only data is modified.
   * @param node  Node name
   * @param data  Data associated with the node
   */
  public void setData(String node, String data) throws InterruptedException, KeeperException {
    logger.debug(String.format("Setting node data {node=[%s], data=[%s]}", node, data));
    ensurePathExists(node);
    ensureExists(node, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
  }

  public String getData(String node) {
    String desc = "";
    try {
      logger.debug(String.format("Getting data from node {node=[%s]}", node));
      Stat stat = zookeeper.exists(node, this);
      if (stat != null) {
        byte[] b = zookeeper.getData(node, this, stat);
        if (b != null) {
          desc = new String(b);
        }
      }
    } catch (Exception e) {
      logger.error(String.format("Error getting data from node {node=[%s]}", node), e);
    }
    return desc;
  }

  /**
   * Ensures that the given path exists with no data, the current
   * ACL and no flags
   * @param path
   */
  public void ensurePathExists(String path) {
    StringBuilder currNode = new StringBuilder();
    String[] allNodes = path.split("/");
    for (String n :  allNodes) {
      if (n.length() > 0) {
        currNode.append("/").append(n);
        ensureExists(currNode.toString(), null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
      }
    }
  }

  /**                                                                                                         
   * Ensures that the given path exists with the given data, ACL and flags
   * @param path
   * @param acl
   * @param flags
   */
  public void ensureExists(final String path, final byte[] data, final List<ACL> acl, final CreateMode flags) {
    try {
      retryOperation(new ZooKeeperOperation() {
        public boolean execute() throws KeeperException, InterruptedException {
          Stat stat = zookeeper.exists(path, false);
          if (stat != null) {
            zookeeper.setData(path, data, -1);
          } else {
            zookeeper.create(path, data, acl, flags);
          }
          return true;
        }
      });
    } catch (KeeperException e) {
      logger.warn("Caught: " + e, e);
    } catch (InterruptedException e) {
      logger.warn("Caught: " + e, e);
    }
  }

  /**
   * Perform the given operation, retrying if the connection fails
   * @return object. it needs to be cast to the callee's expected
   * return type.
   */
  private Object retryOperation(ZooKeeperOperation operation) throws KeeperException, InterruptedException {
    KeeperException exception = null;
    for (int i = 0; i < retryCount; i++) {
      try {
        return operation.execute();
      } catch (KeeperException.SessionExpiredException e) {
        logger.warn("Session expired for: " + zookeeper + " so reconnecting due to: " + e, e);
        throw e;
      } catch (KeeperException.ConnectionLossException e) {
        if (exception == null) {
          exception = e;
        }
        logger.debug("Attempt " + i + " failed with connection loss so " +
            "attempting to reconnect: " + e, e);
        retryDelay(i);
      }
    }
    throw exception;
  }

  /**
   * Returns true if this protocol has been closed
   * @return true if this protocol is closed
   */
  protected boolean isClosed() {
    return closed.get();
  }

  /**
   * Performs a retry delay if this is not the first attempt
   * @param attemptCount the number of the attempts performed so far
   */
  private void retryDelay(int attemptCount) {
    if (attemptCount > 0) {
      try {
        Thread.sleep(attemptCount * retryDelay);
      } catch (InterruptedException e) {
        logger.debug("Failed to sleep: " + e, e);
      }
    }
  }

  public void addWatch(Watcher watcher) {
    synchronized (watchers) {
      watchers.add(watcher);
    }
  }
  
  @Override
  public void process(WatchedEvent event) {
    synchronized (watchers) {
      for (Watcher w : watchers) {
        w.process(event);
      }
    }
  }

  /**
   *
   */
  public interface ZooKeeperOperation {

    /**
     * Performs the operation - which may be involved multiple times if the connection
     * to ZooKeeper closes during this operation
     *
     * @return the result of the operation or null
     * @throws KeeperException
     * @throws InterruptedException
     */
    public boolean execute() throws KeeperException, InterruptedException;
  }
}
