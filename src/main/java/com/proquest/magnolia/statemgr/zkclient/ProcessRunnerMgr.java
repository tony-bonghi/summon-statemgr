package com.proquest.magnolia.statemgr.zkclient;

import com.proquest.magnolia.statemgr.common.DataMonitor;
import com.proquest.magnolia.statemgr.common.ZKConstants;
import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.io.IOException;
import java.util.*;

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
  private MasterProcessState masterState = MasterProcessState.Unknown;
  private ProcessState thisProcessState = ProcessState.Unknown;
  private Map<String,ProcessState> dependencyStates = Collections.synchronizedMap(new HashMap<String, ProcessState>());

  enum ProcessState { Unknown, Idle, InProgress, Success, Error }
  enum MasterProcessState { Unknown, Started, Stopped }

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

    List<String> znodes = new ArrayList<String>(zkProc.getDependencyStateNodes());
    znodes.add(NODE_MASTER);
    znodes.add(zkProc.getSubNode(NODE_STATE));

    this.processRunner = new ProcessRunner(zk, zkProc);
    this.dm = new DataMonitor(zk, znodes, this);
    this.zk.addWatch(this);
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
        
        initProcessStates();

        while (!dm.isDead()) {
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
   * @param data
   */
  public void exists(String path, String data) {

    if (data != null) {

      // If the Master node, update the master state
      if (NODE_MASTER.equals(path)) {
        masterState = getMasterProcessState(data);

      // Update the "dependency" node state
      } else if (zkProc.getDependencyStateNodes().contains(path)) {
        logger.info(String.format("Updating dependency state {path=[%s], node=[%s]}", path, data));
        dependencyStates.put(path, getProcessState(data));

      // Update this processes node state
      } else if (zkProc.getSubNode(NODE_STATE).equals(path)) {
        thisProcessState = getProcessState(data);
      }
      
      // Start or stop the process based on the master, dependencies, and current node states
      if (masterState == MasterProcessState.Started) {
        if (allDependenciesSucceeded() && (thisProcessState == ProcessState.Idle)) {
          processRunner.startProcess();
        }
      } else {
        processRunner.stopProcess("Stopping process...");
      }
    }
  }

  private boolean allDependenciesSucceeded() {
    synchronized (dependencyStates) {
      boolean allSucceeded = true;
      for (ProcessState ps : dependencyStates.values()) {
        if (ps != ProcessState.Success) {
          allSucceeded = false;
          break;
        }
      }
      return allSucceeded;
    }
  }

  /**
   * Do a one-time initialization of the master, dependency, and current process states.
   */
  private void initProcessStates() {

    if (masterState == MasterProcessState.Unknown) {
      masterState = getMasterProcessState(zk.getData(NODE_MASTER));
    }

    synchronized (dependencyStates) {
      if (dependencyStates.isEmpty()) {
        Set<String> depNodes = zkProc.getDependencyStateNodes();
        for (String depNode : depNodes) {
          // If a process has the Master as a dependency, need to set it's state based on the master state
          if (depNode.equals(NODE_MASTER)) {
            ProcessState ps = (masterState == MasterProcessState.Started) ? ProcessState.Success : ProcessState.Idle;
            dependencyStates.put(NODE_MASTER, ps);
          } else {
            dependencyStates.put(depNode, getProcessState(zk.getData(depNode)));
          }
        }
      }
    }

    if (thisProcessState == ProcessState.Unknown) {
      thisProcessState = getProcessState(zk.getData(zkProc.getSubNode(NODE_STATE)));
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
    } else {
      procState = ProcessState.Unknown;
    }
    return procState;
  }

  /**
   * Converts the specified state string to a <code>MasterProcessState</code> enum.
   * @param state The process state as a string (see ZKConstants)
   * @return The MasterProcessState enum
   */
  private MasterProcessState getMasterProcessState(String state) {
    MasterProcessState procState = MasterProcessState.Stopped;
    if (MASTER_STATE_START.equals(state)) {
      procState = MasterProcessState.Started;
    } else if (STATE_INPROGRESS.equals(state)) {
      procState = MasterProcessState.Stopped;
    } else {
      procState = MasterProcessState.Unknown;
   }
    return procState;
  }
}