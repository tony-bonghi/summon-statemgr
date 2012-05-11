package com.proquest.magnolia.statemgr.zkclient;

import com.proquest.magnolia.statemgr.common.ZKConstants;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a process that can be launched by the ZKClient.
 */
public class ZKProcess {

  private String processPath = "";
  private String args = "";
  private ProcessType processType = ProcessType.Java;
  private Set<String> dependencyNodes = new HashSet<String>();
  private Set<String> dependencyStateNodes = new HashSet<String>();
  private String node = "";

  enum ProcessType {
    Java, Shell, EXE, Gradle
  }

  public String getProcessPath() {
    return processPath;
  }

  public void setProcessPath(String processPath) {
    this.processPath = processPath;
  }

  public String getArgs() {
    return args;
  }

  public void setArgs(String args) {
    this.args = args;
  }

  public ProcessType getProcessType() {
    return processType;
  }

  public void setProcessType(ProcessType processType) {
    this.processType = processType;
  }

  public Set<String> getDependencyNodes() {
    return dependencyNodes;
  }

  public Set<String> getDependencyStateNodes() {
    return dependencyStateNodes;
  }

  /**
   * @return  Returns the current node associated with this process
   *          with the "/State" appended.
   */
  private String getDependencyStateNode(String node) {
    StringBuilder sb = new StringBuilder(node);
    sb.append(ZKConstants.NODE_STATE);
    return sb.toString();
  }

  public void setDependencyNodes(Set<String> dependencyNodes) {
    this.dependencyNodes = dependencyNodes;
    for (String dn : dependencyNodes) {
      this.dependencyStateNodes.add(getDependencyStateNode(dn));
    }
  }
  
  public void addDependencyNode(String dependencyNode) {
    this.dependencyNodes.add(dependencyNode);
    this.dependencyStateNodes.add(getDependencyStateNode(dependencyNode));
  }
  
  public String getNode() {
    return node;
  }

  /**
   * @return  Returns the current node associated with this process
   *          with the specified sub-node appended.
   * @param subNode
   */
  public String getSubNode(String subNode) {
    StringBuilder sb = new StringBuilder(node);
    sb.append(subNode);
    return sb.toString();
  }

  public void setNode(String node) {
    this.node = node;
  }
}
