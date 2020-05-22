package machines.real.lathe.behaviours.sequential;

import jade.core.behaviours.Behaviour;
import jade.core.behaviours.SequentialBehaviour;
import machines.real.commons.ProcessItem;
import machines.real.commons.RealMachineAgent;
import machines.real.commons.behaviours.sequantial.SequentialCallForArm;
import machines.real.commons.behaviours.simple.ActionExecutor;
import machines.real.commons.behaviours.simple.ProcessFinishedBehaviour;
import machines.real.commons.behaviours.simple.ProcessItemBehaviour2;
import machines.real.commons.buffer.Buffer;
import machines.real.commons.hal.MiddleHal;
import machines.real.commons.request.ArmRequest;
import machines.real.lathe.actions.GrabReleaseAction;

/**
 * .
 *
 * @author <a href="mailto:junfeng_pan96@qq.com">junfeng</a>
 * @version 1.0.0.0
 * @since 1.8
 */

public class LatheProcessItem implements ProcessItem {

  private RealMachineAgent realAgent;
  private MiddleHal hal;
  private ArmRequest request;
  private Buffer buffer;
  private String armPwd;

  @Override
  public Behaviour getBehaviour(RealMachineAgent realAgent, Buffer buffer, String armPwd) {
    this.realAgent = realAgent;
    this.hal = realAgent.getHal();
    this.request = new ArmRequest(
        String.valueOf(buffer.getIndex()),
        this.realAgent.getLocalName(),
        buffer.getWpInfo().getGoodsId());
    this.buffer = buffer;
    this.armPwd = armPwd;
    return this.getBehaviour();
  }

  private Behaviour getBehaviour() {
    SequentialBehaviour s = new SequentialBehaviour();
    s.addSubBehaviour(loadItemBehaviour());
    s.addSubBehaviour(reverseLoadItemBehaviour());
    s.addSubBehaviour(unloadItemBehaviour());
    return s;
  }

  private SequentialBehaviour loadItemBehaviour() {
    SequentialBehaviour s = new SequentialBehaviour();

    // 装夹 工步1 送料到车床
    ArmRequest request1 = ArmRequest.nextStep(this.request);
    s.addSubBehaviour(new SequentialCallForArm(request1, armPwd,
        "Load item from buffer: " + buffer.getIndex()));
    // 车床夹紧
    s.addSubBehaviour(new ActionExecutor(new GrabReleaseAction(true), hal));

    // 装夹工步2 机械手松开离开
    ArmRequest request2 = ArmRequest.nextStep(request1);
    s.addSubBehaviour(new SequentialCallForArm(request2, armPwd, "Release item"));

    // 通知机械手动作组完成
    notifyArmEndStep(s, request2);

    // 机床加工
    s.addSubBehaviour(new ProcessItemBehaviour2(hal, buffer));
    return s;
  }

  private SequentialBehaviour reverseLoadItemBehaviour() {
    SequentialBehaviour s = new SequentialBehaviour();
    // 修改request
    ArmRequest reverseRequest = ArmRequest.reverseRequest(request);

    // 装夹 工步1 机械手抓住工件
    ArmRequest request1 = ArmRequest.nextStep(reverseRequest);
    s.addSubBehaviour(new SequentialCallForArm(request1, armPwd, "Grab item."));
    // 车床松开
    s.addSubBehaviour(new ActionExecutor(new GrabReleaseAction(false), hal));

    // 装夹工步2 机械手掉转工件,机床夹紧
    ArmRequest request2 = ArmRequest.nextStep(request1);
    s.addSubBehaviour(new SequentialCallForArm(request2, armPwd, "Reverse item."));
    // 机床夹紧
    s.addSubBehaviour(new ActionExecutor(new GrabReleaseAction(true), hal));

    // 装夹工步3 机械手离开 机床加工
    ArmRequest request3 = ArmRequest.nextStep(request2);
    s.addSubBehaviour(new SequentialCallForArm(request3, armPwd, "Release item."));

    // 通知机械手动作组完成
    notifyArmEndStep(s, request3);

    // 机床加工
    s.addSubBehaviour(new ProcessItemBehaviour2(hal, buffer));
    return s;
  }

  private SequentialBehaviour unloadItemBehaviour() {
    SequentialBehaviour s = new SequentialBehaviour();
    // 修改request
    ArmRequest unloadRequest = ArmRequest.unloadRequest(this.request);

    // 装夹 工步1 机械手抓住工件
    ArmRequest request1 = ArmRequest.nextStep(unloadRequest);
    s.addSubBehaviour(new SequentialCallForArm(request1, armPwd, "Grab item."));
    // 车床松开
    s.addSubBehaviour(new ActionExecutor(new GrabReleaseAction(false), hal));

    // 装夹工步2 机械手带着工件离开
    ArmRequest request2 = ArmRequest.nextStep(request1);
    s.addSubBehaviour(new SequentialCallForArm(request2, armPwd,
        "Unload item to buffer: " + buffer.getIndex()));
    s.addSubBehaviour(new ProcessFinishedBehaviour(realAgent, buffer));

    // 通知机械手动作组完成
    notifyArmEndStep(s, request2);

    return s;
  }

  private void notifyArmEndStep(SequentialBehaviour s, ArmRequest request) {
    // 通知机械手动作组完成
    ArmRequest endRequest = ArmRequest.endStep(request);
    s.addSubBehaviour(new SequentialCallForArm(endRequest, armPwd, "End Step"));
  }

}
