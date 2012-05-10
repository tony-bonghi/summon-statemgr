package com.proquest.magnolia.statemgr.zkclient;

import com.proquest.magnolia.statemgr.common.ZKClientBase;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;
import java.util.List;

/**
 * ZK Client application used to manage and launch multiple applications based
 * on the configuration in the ZK service nodes.
 */
public class ZKClient extends ZKClientBase {

  private static final Logger logger = Logger.getLogger(ZKClient.class.getName());

  /**
   * Main entry point.
   * @param args
   * <code>
   *          args[0] - The ZK service connection parameters ("127.0.0.1:2181,127.0.0.1:2182")
   *          args[1] - The client configuration file
   * </code>
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    PropertyConfigurator.configure("./log4j.properties");
    ZKClient zkClient = new ZKClient(args[0]);
    zkClient.execute(args[1]);
  }

  /**
   * Constructor.
   * @param zkConnection  The ZK service connection parameters ("127.0.0.1:2181,127.0.0.1:2182")
   * @throws IOException
   */
  public ZKClient(String zkConnection) throws IOException {
    super(zkConnection);
  }

  /**
   * Reads in the list of processes to be run and launches
   * a separate manager thread for each so they are run as the
   * dependencies are completed.
   *
   * @param processCfgPath  The client configuration file
   * @throws Exception
   */
  public void execute(String processCfgPath) throws Exception {

    // Read in the list of processes to be run on this machine
    logger.info("Reading in configuration file...");
    ProcessFileReader pfr = new ProcessFileReader();
    List<ZKProcess> zkProcesses = pfr.read(processCfgPath);

    // Launch a thread for each process to be run
    logger.info("Launching processes read from file...");
    for (ZKProcess proc : zkProcesses) {
      ProcessRunnerMgr executor = new ProcessRunnerMgr(this, proc);
      Thread t = new Thread(executor);
      t.setDaemon(true);
      t.setName(proc.getNode());
      t.start();
    }
    
    // Wait so we stay alive
    while (true) {
      Thread.sleep(10000);
    }
  }
}
