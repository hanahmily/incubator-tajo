/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tajo.client;

import com.google.protobuf.ServiceException;
import org.apache.commons.cli.*;
import org.apache.commons.lang.StringUtils;
import org.apache.tajo.TajoProtos.QueryState;
import org.apache.tajo.conf.TajoConf;
import org.apache.tajo.ipc.ClientProtos.BriefQueryInfo;
import org.apache.tajo.ipc.ClientProtos.WorkerResourceInfo;
import org.apache.tajo.util.TajoIdUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class TajoAdmin {
  private static final org.apache.commons.cli.Options options;
  private static DecimalFormat decimalF = new DecimalFormat("###.0");
  private enum WorkerStatus {
    LIVE,
    DEAD,
    DECOMMISSION
  }

  final static String line5   = "-----";
  final static String line7   = "-------";
  final static String line10  = "----------";
  final static String line12  = "------------";
  final static String line15  = "---------------";
  final static String line20  = "--------------------";
  final static String line25  = "-------------------------";
  final static String line30  = "------------------------------";
  final static String DATE_FORMAT  = "yyyy-MM-dd HH:mm:ss";

  static {
    options = new Options();
    options.addOption("h", "host", true, "Tajo server host");
    options.addOption("p", "port", true, "Tajo server port");
    options.addOption("list", null, false, "Show Tajo query list");
    options.addOption("cluster", null, false, "Show Cluster Info");
    options.addOption("desc", null, false, "Show Query Description");
    options.addOption("kill", null, true, "Kill a running query");
  }

  private static void printUsage() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp( "admin [options]", options );
  }

  private static String getQueryState(QueryState state) {
    String stateStr = "FAILED";

    if (TajoClient.isQueryRunnning(state)) {
      stateStr = "RUNNING";
    } else if (state == QueryState.QUERY_SUCCEEDED) {
      stateStr = "SUCCEED";
    }

    return stateStr;
  }

  public static void main(String [] args) throws ParseException, IOException, ServiceException, SQLException {
    TajoConf conf = new TajoConf();

    CommandLineParser parser = new PosixParser();
    CommandLine cmd = parser.parse(options, args);

    String param = "";
    int cmdType = 0;

    String hostName = null;
    Integer port = null;
    if (cmd.hasOption("h")) {
      hostName = cmd.getOptionValue("h");
    }
    if (cmd.hasOption("p")) {
      port = Integer.parseInt(cmd.getOptionValue("p"));
    }

    String queryId = null;

    if (cmd.hasOption("list")) {
      cmdType = 1;
    } else if (cmd.hasOption("desc")) {
      cmdType = 2;
    } else if (cmd.hasOption("cluster")) {
      cmdType = 3;
    } else if (cmd.hasOption("kill")) {
      cmdType = 4;
      queryId = cmd.getOptionValue("kill");
    }

    // if there is no "-h" option,
    if(hostName == null) {
      if (conf.getVar(TajoConf.ConfVars.TAJO_MASTER_CLIENT_RPC_ADDRESS) != null) {
        // it checks if the client service address is given in configuration and distributed mode.
        // if so, it sets entryAddr.
        hostName = conf.getVar(TajoConf.ConfVars.TAJO_MASTER_CLIENT_RPC_ADDRESS).split(":")[0];
      }
    }
    if (port == null) {
      if (conf.getVar(TajoConf.ConfVars.TAJO_MASTER_CLIENT_RPC_ADDRESS) != null) {
        // it checks if the client service address is given in configuration and distributed mode.
        // if so, it sets entryAddr.
        port = Integer.parseInt(conf.getVar(TajoConf.ConfVars.TAJO_MASTER_CLIENT_RPC_ADDRESS).split(":")[1]);
      }
    }

    if (cmdType == 0) {
        printUsage();
        System.exit(0);
    }

    TajoClient client = null;
    if ((hostName == null) ^ (port == null)) {
      System.err.println("ERROR: cannot find valid Tajo server address");
      System.exit(-1);
    } else if (hostName != null && port != null) {
      conf.setVar(TajoConf.ConfVars.TAJO_MASTER_CLIENT_RPC_ADDRESS, hostName+":"+port);
      client = new TajoClient(conf);
    } else if (hostName == null && port == null) {
      client = new TajoClient(conf);
    }

    Writer writer = new PrintWriter(System.out);
    switch (cmdType) {
      case 1:
        processList(writer, client);
        break;
      case 2:
        processDesc(writer, client);
        break;
      case 3:
        processCluster(writer, client);
        break;
      case 4:
        processKill(writer, client, queryId);
        break;
      default:
        printUsage();
        break;
    }

    writer.flush();
    writer.close();

    System.exit(0);
  }

  public static void processDesc(Writer writer, TajoClient client) throws ParseException, IOException,
      ServiceException, SQLException {
    List<BriefQueryInfo> queryList = client.getRunningQueryList();
    SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
    int id = 1;
    for (BriefQueryInfo queryInfo : queryList) {
        String queryId = String.format("q_%s_%04d",
                                       queryInfo.getQueryId().getId(),
                                       queryInfo.getQueryId().getSeq());

        writer.write("Id: " + id);
        writer.write("\n");
        id++;
        writer.write("Query Id: " + queryId);
        writer.write("\n");
        writer.write("Started Time: " + df.format(queryInfo.getStartTime()));
        writer.write("\n");
        String state = getQueryState(queryInfo.getState());
        writer.write("Query State: " + state);
        writer.write("\n");
        long end = queryInfo.getFinishTime();
        long start = queryInfo.getStartTime();
        String executionTime = decimalF.format((end-start) / 1000) + " sec";
        if (state.equals("RUNNING") == false) {
          writer.write("Finished Time: " + df.format(queryInfo.getFinishTime()));
          writer.write("\n");
        }
        writer.write("Execution Time: " + executionTime);
        writer.write("\n");
        writer.write("Query Progress: " + queryInfo.getProgress());
        writer.write("\n");
        writer.write("Query Statement:");
        writer.write("\n");
        writer.write(queryInfo.getQuery());
        writer.write("\n");
        writer.write("\n");
    }
  }

  public static void processCluster(Writer writer, TajoClient client) throws ParseException, IOException,
      ServiceException, SQLException {
    List<WorkerResourceInfo> workerList = client.getClusterInfo();

    int runningQueryMasterTasks = 0;

    List<WorkerResourceInfo> liveWorkers = new ArrayList<WorkerResourceInfo>();
    List<WorkerResourceInfo> deadWorkers = new ArrayList<WorkerResourceInfo>();
    List<WorkerResourceInfo> decommissionWorkers = new ArrayList<WorkerResourceInfo>();

    List<WorkerResourceInfo> liveQueryMasters = new ArrayList<WorkerResourceInfo>();
    List<WorkerResourceInfo> deadQueryMasters = new ArrayList<WorkerResourceInfo>();

    for (WorkerResourceInfo eachWorker : workerList) {
      if(eachWorker.getQueryMasterMode() == true) {
        if(eachWorker.getWorkerStatus().equals(WorkerStatus.LIVE.toString())) {
          liveQueryMasters.add(eachWorker);
          runningQueryMasterTasks += eachWorker.getNumQueryMasterTasks();
        }
        if(eachWorker.getWorkerStatus().equals(WorkerStatus.DEAD.toString())) {
          deadQueryMasters.add(eachWorker);
        }
      }

      if(eachWorker.getTaskRunnerMode() == true) {
        if(eachWorker.getWorkerStatus().equals(WorkerStatus.LIVE.toString())) {
          liveWorkers.add(eachWorker);
        } else if(eachWorker.getWorkerStatus().equals(WorkerStatus.DEAD.toString())) {
          deadWorkers.add(eachWorker);
        } else if(eachWorker.getWorkerStatus().equals(WorkerStatus.DECOMMISSION.toString())) {
          decommissionWorkers.add(eachWorker);
        }
      }
    }

    String fmtInfo = "%1$-5s %2$-5s %3$-5s%n";
    String infoLine = String.format(fmtInfo, "Live", "Dead", "Tasks");

    writer.write("Query Master\n");
    writer.write("============\n\n");
    writer.write(infoLine);
    String line = String.format(fmtInfo, line5, line5, line5);
    writer.write(line);

    line = String.format(fmtInfo, liveQueryMasters.size(),
                         deadQueryMasters.size(), runningQueryMasterTasks);
    writer.write(line);
    writer.write("\n");

    writer.write("Live QueryMasters\n");
    writer.write("=================\n\n");

    if (liveQueryMasters.isEmpty()) {
      writer.write("No Live QueryMasters\n");
    } else {
      String fmtQueryMasterLine = "%1$-25s %2$-5s %3$-5s %4$-10s %5$-10s%n";
      line = String.format(fmtQueryMasterLine, "QueryMaster", "Port", "Query",
                           "Heap", "Status");
      writer.write(line);
      line = String.format(fmtQueryMasterLine, line25, line5,
                           line5, line10, line10);
      writer.write(line);
      for (WorkerResourceInfo queryMaster : liveQueryMasters) {
        String queryMasterHost = String.format("%s:%d",
                                  queryMaster.getAllocatedHost(),
                                  queryMaster.getQueryMasterPort());
        String heap = String.format("%d MB", queryMaster.getMaxHeap()/1024/1024);
        line = String.format(fmtQueryMasterLine, queryMasterHost,
                             queryMaster.getClientPort(),
                             queryMaster.getNumQueryMasterTasks(),
                             heap, queryMaster.getWorkerStatus());
        writer.write(line);
      }

      writer.write("\n\n");
    }

    if (!deadQueryMasters.isEmpty()) {
      writer.write("Dead QueryMasters\n");
      writer.write("=================\n\n");

      String fmtQueryMasterLine = "%1$-25s %2$-5s %3$-10s%n";
      line = String.format(fmtQueryMasterLine, "QueryMaster", "Port", "Status");
      writer.write(line);
      line = String.format(fmtQueryMasterLine, line25, line5, line10);
      writer.write(line);

      for (WorkerResourceInfo queryMaster : deadQueryMasters) {
        String queryMasterHost = String.format("%s:%d",
                                  queryMaster.getAllocatedHost(),
                                  queryMaster.getQueryMasterPort());
        line = String.format(fmtQueryMasterLine, queryMasterHost,
                             queryMaster.getClientPort(),
                             queryMaster.getWorkerStatus());
        writer.write(line);
      }

      writer.write("\n\n");
    }

    writer.write("Worker\n");
    writer.write("======\n\n");

    String fmtWorkerInfo = "%1$-5s %2$-5s%n";
    String workerInfoLine = String.format(fmtWorkerInfo, "Live", "Dead");
    writer.write(workerInfoLine);
    line = String.format(fmtWorkerInfo, line5, line5);
    writer.write(line);

    line = String.format(fmtWorkerInfo, liveWorkers.size(), deadWorkers.size());
    writer.write(line);
    writer.write("\n");

    writer.write("Live Workers\n");
    writer.write("============\n\n");
    if(liveWorkers.isEmpty()) {
      writer.write("No Live Workers\n\n");
    } else {
      writeWorkerInfo(writer, liveWorkers);
    }

    writer.write("Dead Workers\n");
    writer.write("============\n\n");
    if(deadWorkers.isEmpty()) {
      writer.write("No Dead Workers\n\n");
    } else {
      writeWorkerInfo(writer, deadWorkers);
    }
  }

  private static void writeWorkerInfo(Writer writer, List<WorkerResourceInfo> workers) throws ParseException,
      IOException, ServiceException, SQLException {
    String fmtWorkerLine = "%1$-25s %2$-5s %3$-5s %4$-10s %5$-10s %6$-12s %7$-10s%n";
    String line = String.format(fmtWorkerLine,
        "Worker", "Port", "Tasks",
        "Mem", "Disk",
        "Heap", "Status");
    writer.write(line);
    line = String.format(fmtWorkerLine,
        line25, line5, line5,
        line10, line10,
        line12, line10);
    writer.write(line);

    for (WorkerResourceInfo worker : workers) {
      String workerHost = String.format("%s:%d",
          worker.getAllocatedHost(),
          worker.getPeerRpcPort());
      String mem = String.format("%d/%d", worker.getUsedMemoryMB(),
          worker.getMemoryMB());
      String disk = String.format("%.2f/%.2f", worker.getUsedDiskSlots(),
          worker.getDiskSlots());
      String heap = String.format("%d/%d MB", worker.getFreeHeap()/1024/1024,
          worker.getMaxHeap()/1024/1024);

      line = String.format(fmtWorkerLine, workerHost,
          worker.getPullServerPort(),
          worker.getNumRunningTasks(),
          mem, disk, heap, worker.getWorkerStatus());
      writer.write(line);
    }
    writer.write("\n\n");
  }

  public static void processList(Writer writer, TajoClient client) throws ParseException, IOException,
      ServiceException, SQLException {
    List<BriefQueryInfo> queryList = client.getRunningQueryList();
    SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
    String fmt = "%1$-20s %2$-7s %3$-20s %4$-30s%n";
    String line = String.format(fmt, "QueryId", "State", 
                  "StartTime", "Query");
    writer.write(line);
    line = String.format(fmt, line20, line7, line20, line30);
    writer.write(line);

    for (BriefQueryInfo queryInfo : queryList) {
        String queryId = String.format("q_%s_%04d",
                                       queryInfo.getQueryId().getId(),
                                       queryInfo.getQueryId().getSeq());
        String state = getQueryState(queryInfo.getState());
        String startTime = df.format(queryInfo.getStartTime());

        String sql = StringUtils.abbreviate(queryInfo.getQuery(), 30);
        line = String.format(fmt, queryId, state, startTime, sql);
        writer.write(line);
    }
  }

  public static void processKill(Writer writer, TajoClient client, String queryIdStr)
      throws IOException, ServiceException {
    boolean killedSuccessfully = client.killQuery(TajoIdUtils.parseQueryId(queryIdStr));
    if (killedSuccessfully) {
      writer.write(queryIdStr + " is killed successfully.\n");
    } else {
      writer.write("killing query is failed.");
    }
  }
}
