/*
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

package org.apache.tajo.master.event;

import com.google.protobuf.RpcCallback;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.tajo.ExecutionBlockId;
import org.apache.tajo.ipc.TajoWorkerProtocol.QueryUnitRequestProto;
import org.apache.tajo.master.querymaster.QueryUnitAttempt;

public class QueryUnitAttemptScheduleEvent extends TaskSchedulerEvent {
  private final QueryUnitAttemptScheduleContext context;
  private final QueryUnitAttempt queryUnitAttempt;

  public QueryUnitAttemptScheduleEvent(EventType eventType, ExecutionBlockId executionBlockId,
                                       QueryUnitAttemptScheduleContext context, QueryUnitAttempt queryUnitAttempt) {
    super(eventType, executionBlockId);
    this.context = context;
    this.queryUnitAttempt = queryUnitAttempt;
  }

  public QueryUnitAttempt getQueryUnitAttempt() {
    return queryUnitAttempt;
  }

  public QueryUnitAttemptScheduleContext getContext() {
    return context;
  }

  public static class QueryUnitAttemptScheduleContext {
    private ContainerId containerId;
    private String host;
    private RpcCallback<QueryUnitRequestProto> callback;

    public QueryUnitAttemptScheduleContext() {

    }

    public QueryUnitAttemptScheduleContext(ContainerId containerId,
                                           String host,
                                           RpcCallback<QueryUnitRequestProto> callback) {
      this.containerId = containerId;
      this.host = host;
      this.callback = callback;
    }

    public ContainerId getContainerId() {
      return containerId;
    }

    public void setContainerId(ContainerId containerId) {
      this.containerId = containerId;
    }

    public String getHost() {
      return host;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public RpcCallback<QueryUnitRequestProto> getCallback() {
      return callback;
    }

    public void setCallback(RpcCallback<QueryUnitRequestProto> callback) {
      this.callback = callback;
    }
  }
}
