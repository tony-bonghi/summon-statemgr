package com.proquest.magnolia.statemgr.zkclient;

import com.proquest.magnolia.statemgr.utils.ZKClientBase;
import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;

/**
 */
public class ZKClient extends ZKClientBase {

  private static final Logger logger = Logger.getLogger(ZKClient.class.getName());
  
  public static void main(String[] args) throws Exception {
    ZKClient zkClient = new ZKClient(args[0]);
    zkClient.execute(args[1]);
  }

  public ZKClient(String zkConnection) throws IOException {
    super(zkConnection);
  }

  public void execute(String processCfgPath) throws SAXException, ParserConfigurationException, IOException, InterruptedException, KeeperException {
    ProcessFileReader pfr = new ProcessFileReader();
    List<ZKProcess> processes = pfr.read(processCfgPath);

    for (ZKProcess proc : processes) {
      zookeeper.exists(proc.getNode(), this);
    }
  }

  @Override
  public void process(WatchedEvent event) {
  }

}
