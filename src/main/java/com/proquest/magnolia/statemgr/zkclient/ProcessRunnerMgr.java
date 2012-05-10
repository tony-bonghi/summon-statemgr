package com.proquest.magnolia.statemgr.zkclient;

import com.proquest.magnolia.statemgr.common.ZKConstants;
import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.io.IOException;

/**
 * Manages the launching of processes in conjunction with the DataMonitor class. This class
 * simply waits for notifications from DataMonitor for node state changes and launches or stops
 * the executable process as needed.
 */
public class ProcessRunnerMgr implements Watcher, Runnable, DataMonitor.DataMonitorListener, ZKConstants {

  private static final Logger logger = Logger.getLogger(ProcessRunnerMgr.class.getName());

  private ProcessRunner processRunner;
  private DataMonitor dm;
  private ZKClient zk;
  private ZKProcess zkProc;
  private String dependencyNodeState = "";
  
  enum ProcessState { Idle, InProgress, Success, Error }

  /**
   * Constructor.
   * @param zk      ZKClient reference.
   * @param zkProc  Process to execute
   * @throws KeeperException
   * @throws IOException
   */
  public ProcessRunnerMgr(ZKClient zk, ZKProcess zkProc) throws KeeperException, IOException {
    this.zk = zk;
    this.zkProc = zkProc;
    this.dependencyNodeState = zkProc.getDependencyNode()+"/State";
    this.dm = new DataMonitor(zk, dependencyNodeState, this);
    this.zk.addWatch(this);
    this.processRunner = new ProcessRunner(zk, zkProc);
  }

  /**
   * Forwards the event to the DataMonitor object.
   */
  public void process(WatchedEvent event) {
    dm.process(event);
  }

  public void run() {
    logger.info("ProcessRunningMgr is starting...");
    try {
      synchronized (this) {
        
        // First test to see if the dependency node state is SUCCESS
//        String d = zk.getData(dependencyNodeState);
//        exists(dependencyNodeState, d.getBytes());
        
        while (!dm.isDead) {
          wait();
        }
      }
    } catch (InterruptedException e) {
    }
    logger.info("ProcessRunningMgr is dead");
  }

  public void closing(int rc) {
    synchronized (this) {
      notifyAll();
    }
  }

  /**
   * 
   * @param path
   * @param bytes
   */
  public void exists(String path, byte[] bytes) {

    String data = (bytes != null) ? new String(bytes) : null;

    if (data == null) {
      processRunner.stopProcess("Killing child process");

    } else {

      if (MASTER_NODE.equals(dependencyNodeState)) {
        if (data.equals(MASTER_STATE_START) || data.equals(MASTER_STATE_CONTINUE)) {
           processRunner.startProcess();
        }

      } else if (MASTER_NODE.equals(path)) {
        if (MASTER_STATE_STOP.equals(data)) {
          processRunner.stopProcess("Stopping child process due to master process node being set to STOP");
        }

      } else {
        switch (getProcessState(data)) {
          case Success:
            processRunner.startProcess();
            break;
          default:
            processRunner.stopProcess("Stopping child process");
            break;
        }
      }
    }
  }

  /**
   * Converts the specified state string to a <code>ProcessState</code> enum.
   * @param state The process state as a string (see ZKConstants)
   * @return The ProcessState enum
   */
  private ProcessState getProcessState(String state) {
    ProcessState procState = ProcessState.Idle;
    if (STATE_IDLE.equals(state)) {
      procState = ProcessState.Idle;
    } else if (STATE_INPROGRESS.equals(state)) {
      procState = ProcessState.InProgress;
    } else if (STATE_SUCCESS.equals(state)) {
      procState = ProcessState.Success;
    } else if (STATE_ERROR.equals(state)) {
      procState = ProcessState.Error;
    }
    return procState;
  }
}