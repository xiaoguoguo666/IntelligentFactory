package machines.real.agv.behaviours.cycle;

import commons.tools.LoggerUtil;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import machines.real.agv.actions.InExportAction;
import machines.real.agv.actions.MoveAction;
import machines.real.agv.algorithm.AgvMapUtils;
import machines.real.agv.algorithm.AgvRoutePlan;
import machines.real.agv.behaviours.sequencial.CallWarehouseConveyor;
import machines.real.agv.behaviours.sequencial.CallWarehouseMoveItem;
import machines.real.agv.behaviours.sequencial.ImExportItemBehaviour;
import machines.real.agv.behaviours.simple.ActionCaller;
import machines.real.agv.behaviours.simple.InteractBuffer;
import machines.real.commons.actions.MachineAction;
import machines.real.commons.request.AgvRequest;
import machines.real.commons.request.BufferRequest;
import machines.real.commons.request.WarehouseConveyorRequest;
import machines.real.commons.request.WarehouseItemMoveRequest;

/**
 * 多Agv系统运输货物.
 *
 * @author <a href="mailto:junfeng_pan96@qq.com">junfeng</a>
 * @version 1.0.0.0
 * @since 1.8
 */
public class TransportItemBehaviour2 extends CyclicBehaviour {

  private Queue<AgvRequest> queue = new LinkedList<>();
  private Map<AID, Long> checkInMap;
  private AgvRoutePlan plan;
  private ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();

  public void setPlan(AgvRoutePlan plan) {
    this.plan = plan;
  }

  public void setCheckInMap(Map<AID, Long> checkInMap) {
    this.checkInMap = checkInMap;
  }

  @Override
  public void action() {
    receiveRequest();

    if (queue.size() == 0) {
      block();
      return;
    }
    if (checkInMap.size() == 0) {
      LoggerUtil.agent.warn("No available agv instance!");
      block(3000);
      return;
    }
    // 应该获取最近的agv, 调用它去完成任务
    AgvRequest request = queue.poll();
    if (request == null) {
      return;
    }
    // 取货点
    int fromBuffer = request.getFromBuffer();
    int fromLoc = AgvMapUtils.getBufferMap(fromBuffer, plan);
    AID choose = null;
    int distance = Integer.MAX_VALUE;
    for (AID agv : checkInMap.keySet()) {
      // 1. 获取agv当前位置
      int loc = AgvMapUtils.getLocationMap().get(agv);
      int d = plan.getDistance(loc, fromLoc);
      if (distance > d) {
        choose = agv;
        distance = d;
      }
    }
    // 2. 获取最近位置的agv
    if (choose == null) {
      return;
    }
    LoggerUtil.agent.debug("Choose agv: " + choose.getLocalName());
    // 3. 计算路径取货与清除阻塞
    int pos = AgvMapUtils.getLocationMap().get(choose);
    String getGoodPath = plan.getRoute(pos, fromLoc);
    if (!"".equals(getGoodPath)) {
      // 需移动
      int conflict = AgvMapUtils
          .conflictNode(
              Arrays.stream(getGoodPath.split(",")).mapToInt(Integer::parseInt).toArray());
      solveConflict(conflict);
      // 4. 到达取货点
      MachineAction moveToStart = new MoveAction(plan.getRoute(
          AgvMapUtils.getLocationMap().get(choose), fromLoc));
      waitCallerDone(choose, moveToStart);
    }
    // 5. 入料
    if (fromBuffer < 0) {
      // 如果对象是仓库，需要修改操作
      Behaviour callWhMoveItem = new CallWarehouseMoveItem(
          new WarehouseItemMoveRequest(request.getWpInfo().getWarehousePosition()));
      waitBehaviourDone(callWhMoveItem);
      // AGV入料
      waitCallerDone(choose, new InExportAction(true));
      // 仓库出料
      Behaviour whExport = new CallWarehouseConveyor(new WarehouseConveyorRequest(false));
      waitBehaviourDone(whExport);
    } else {
      // 与buffer交互
      InteractBuffer inFromBuffer = new InteractBuffer(
          new BufferRequest(fromBuffer, true));
      ActionCaller inCaller = new ActionCaller(choose, new InExportAction(true));
      Behaviour inBehaviour = new ImExportItemBehaviour(true, inCaller, inFromBuffer);
      waitBehaviourDone(inBehaviour);
    }

    // 6. 计算路径送货与清除阻塞
    int toBuffer = request.getToBuffer();
    int toLoc = AgvMapUtils.getBufferMap(toBuffer, plan);
    String sendGoodPath = plan.getRoute(fromLoc, toLoc);
    if (!"".equals(sendGoodPath)) {
      // 需移动
      int conflict = AgvMapUtils
          .conflictNode(
              Arrays.stream(sendGoodPath.split(",")).mapToInt(Integer::parseInt).toArray());
      solveConflict(conflict);
      // 7. 到达送货点
      MachineAction moveToEnd = new MoveAction(
          plan.getRoute(AgvMapUtils.getLocationMap().get(choose), toLoc));
      waitCallerDone(choose, moveToEnd);
    }
    // 8. 送料
    if (toBuffer < 0) {
      // 仓库入料
      Behaviour whImport = new CallWarehouseConveyor(new WarehouseConveyorRequest(true));
      waitBehaviourDone(whImport);
      // AGV出料
      waitCallerDone(choose, new InExportAction(false));
      // 如果对象是仓库，需要修改
      Behaviour callForWh = new CallWarehouseMoveItem(
          new WarehouseItemMoveRequest(request.getWpInfo().getWarehousePosition()));
      waitBehaviourDone(callForWh);
    } else {
      // 与buffer交互
      InteractBuffer exToBuffer = new InteractBuffer(new BufferRequest(toBuffer, false));
      ActionCaller outCaller = new ActionCaller(choose, new InExportAction(false));
      Behaviour outBehaviour = tbf.wrap(new ImExportItemBehaviour(false, outCaller, exToBuffer));
      waitBehaviourDone(outBehaviour);
    }
  }

  private void solveConflict(int conflict) {
    if (conflict == -1) {
      return;
    }
    AID conflictAid = AgvMapUtils.getConflictAid(conflict);
    if (conflictAid != null) {
      LoggerUtil.agent.info(
          "Route plan conflict! Pos: " + conflict + ", agent: " + conflictAid.getLocalName());
      int newLoc = AgvMapUtils.getFreeEdgeNode(plan.getEdgeNodes(), 4);
      MoveAction moveConflict = new MoveAction(plan.getRoute(conflict, newLoc));
      waitCallerDone(conflictAid, moveConflict);
    }
  }

  private void receiveRequest() {
    MessageTemplate mt = MessageTemplate.and(
        MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
        MessageTemplate.MatchPerformative(ACLMessage.REQUEST)
    );
    ACLMessage msg = myAgent.receive(mt);
    if (msg == null) {
      return;
    }
    AgvRequest request;
    try {
      request = (AgvRequest) msg.getContentObject();
    } catch (UnreadableException e) {
      e.printStackTrace();
      return;
    }
    queue.offer(request);
  }

  private void waitCallerDone(AID receiver, MachineAction action) {
    // 等待动作完成
    waitBehaviourDone(new ActionCaller(receiver, action));
  }

  private void waitBehaviourDone(Behaviour b) {
    // 额外线程运行
    b = tbf.wrap(b);
    myAgent.addBehaviour(b);
    while (!b.done()) {
      block(1000);
    }
  }

}
