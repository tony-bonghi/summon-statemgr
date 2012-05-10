package com.proquest.magnolia.statemgr.zkmaster;

import com.proquest.magnolia.statemgr.common.ZKClientBase;
import com.proquest.magnolia.statemgr.common.ZKConstants;
import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * Client utility used to configure and initialize ZK state nodes. The input configuration file will be a simple
 * properties format consisting of the node and description.
 */
public class ZKMaster extends ZKClientBase implements ZKConstants {

  private static final Logger logger = Logger.getLogger(ZKMaster.class.getName());

  private Properties nodeCfg;
  private String startMode = "";

  /**
   * Main entry point.
   * @param args See the constructor...
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    if (args.length >= 3) {
      ZKMaster zkMaster = new ZKMaster(args[0], args[1], args[2]);
      zkMaster.execute();
    }
  }

  /**
   * Constructor.
   * @param zkConnectionStr  The Zookeeper host:port to connect
   * @param propsFile Properties file containing the state configuration
   * @param startMode Start mode (start, stop, continue)
   * @throws IOException
   */
  public ZKMaster(String zkConnectionStr, String propsFile, String startMode) throws IOException {
    super(zkConnectionStr);
    this.nodeCfg = new Properties();
    this.startMode = startMode;
    
    nodeCfg.load(new BufferedInputStream(new FileInputStream(propsFile)));
  }

  /**
   * Performs the execution for this class. The z-nodes are configured based on the
   * input properties file.
   * @throws IOException
   * @throws InterruptedException
   * @throws KeeperException
   */
  public void execute() throws IOException, InterruptedException, KeeperException {

    // Create all nodes based on configuration file.
    for (Map.Entry<Object,Object> entry : nodeCfg.entrySet()) {
      String node = (String) entry.getKey();
      String desc = (String) entry.getValue();
      setData(node, "");
      setData(node + "/Description", desc);
      if (startMode.equalsIgnoreCase(MASTER_STATE_START)) {
        setData(node + "/State", STATE_IDLE);
        setData(node + "/StateInfo", "");
        setData(node + "/Timestamp/Start", "");
        setData(node + "/Timestamp/End", "");
      }
    }

    // Create the master the master node and initialize
    setData(MASTER_NODE, startMode);
  }

  /**
   * Handles events by outputting updates to a log file.
   * @param event
   */
  @Override
  public void process(WatchedEvent event) {
    super.process(event);
    if (!startMode.equalsIgnoreCase(MASTER_STATE_STOP)) {
      
      String path = event.getPath();
      String desc = getData(path + "/Description");
      String state = getData(path + "/State");
      String stateInfo = getData(path + "StateInfo");

      if (state.equals(STATE_ERROR)) {
        logger.error(String.format("%s {%s : %s}", desc, state, stateInfo));
      } else {
        logger.info(String.format("%s {%s : %s}", desc, state, stateInfo));
      }
    }
  }
}
