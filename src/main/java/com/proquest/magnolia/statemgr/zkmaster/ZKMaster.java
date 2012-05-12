package com.proquest.magnolia.statemgr.zkmaster;

import com.proquest.magnolia.statemgr.common.ZKClientBase;
import com.proquest.magnolia.statemgr.common.ZKConstants;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
      PropertyConfigurator.configure("./zkmaster-log4j.properties");
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

    List<String> znodes = new ArrayList<String>();

    // Create the master the master node and initialize
    if (startMode.equals(MASTER_STATE_STOP)) {
      setData(NODE_MASTER, MASTER_STATE_STOP);
    }

    // Create all nodes based on configuration file.
    for (Map.Entry<Object,Object> entry : nodeCfg.entrySet()) {
      String node = (String) entry.getKey();
      String desc = (String) entry.getValue();

      setData(node + NODE_DESCRIPTION, desc);

      // If mode start, initialize all node data
      if (startMode.equals(MASTER_STATE_START)) {
        setData(node + NODE_STATE, STATE_IDLE);
        setData(node + NODE_STATE_INFO, "");
        setData(node + NODE_TIME_START, "");
        setData(node + NODE_TIME_END, "");

      // If mode continue, only setup the node state to idle and only if it previously didn't succeed
      } else if (startMode.equals(MASTER_STATE_CONTINUE)) {
        String data = getData(node + NODE_STATE);
        if (!STATE_SUCCESS.equals(data)) {
          setData(node + NODE_STATE, STATE_IDLE);
        }
      }

      // Setup a watch on the nodes
      zookeeper.exists(node+NODE_STATE, this);
      zookeeper.exists(node+NODE_STATE_INFO, this);
    }

    // Create the master the master node and initialize
    if (!startMode.equals(MASTER_STATE_STOP)) {
      setData(NODE_MASTER, MASTER_STATE_START);

      // Wait for the user to type quit
      System.out.println("Type quit to stop processing.");
      byte[] b = new byte[80];
      while (System.in.read(b) > 0) {
        String d = new String(b);
        if (d.startsWith("quit")) {
          setData(NODE_MASTER, MASTER_STATE_STOP);
          break;
        }
      }
    }
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
      if (path != null && (path.endsWith(NODE_STATE) || path.endsWith(NODE_STATE_INFO))) {
        
        String rootPath = "";
        if (path.endsWith(NODE_STATE)) {
          rootPath = path.substring(0, path.length() - NODE_STATE.length());
        } else if (path.endsWith(NODE_STATE_INFO)) {
          rootPath = path.substring(0, path.length() - NODE_STATE_INFO.length());
        }

        String state = getData(rootPath + NODE_STATE);
        if (state.length() > 0) {
          String desc = getData(rootPath + NODE_DESCRIPTION);
          String stateInfo = getData(rootPath + NODE_STATE_INFO);

          String logStr = "";
          if (stateInfo.length() > 0) {
            logStr = String.format("[%s] %s (%s : %s)", rootPath, desc, state, stateInfo);
          } else {
            logStr = String.format("[%s] %s (%s)", rootPath, desc, state);
          }

          if (state.equals(STATE_ERROR)) {
            logger.error(logStr);
          } else {
            logger.info(logStr);
          }
        }
      }
    }
  }
}
