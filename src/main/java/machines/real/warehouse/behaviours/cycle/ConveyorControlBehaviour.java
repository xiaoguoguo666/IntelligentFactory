package machines.real.warehouse.behaviours.cycle;

import commons.tools.LoggerUtil;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import machines.real.commons.hal.MiddleHal;
import machines.real.commons.request.WarehouseConveyorRequest;
import machines.real.warehouse.actions.ImExportItemAction;

/**
 * 传送带控制.
 *
 * @author <a href="mailto:junfeng_pan96@qq.com">junfeng</a>
 * @version 1.0.0.0
 * @since 1.8
 */
public class ConveyorControlBehaviour extends CyclicBehaviour {

  private MiddleHal hal;
  private MessageTemplate mt = MessageTemplate.and(
      MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
      MessageTemplate.MatchLanguage(WarehouseConveyorRequest.LANGUAGE));

  public void setHal(MiddleHal hal) {
    this.hal = hal;
  }

  @Override
  public void action() {
    ACLMessage receive = myAgent.receive(mt);
    if (receive == null) {
      block();
      return;
    }
    WarehouseConveyorRequest request;
    try {
      request = (WarehouseConveyorRequest) receive.getContentObject();
    } catch (UnreadableException e) {
      e.printStackTrace();
      LoggerUtil.agent
          .warn("Deserialization Error in WarehouseConveyorRequest. " + e.getLocalizedMessage());
      return;
    }
    ImExportItemAction action = new ImExportItemAction(request.isImportMode());
    while (!hal.executeAction(action)) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    ACLMessage reply = receive.createReply();
    reply.setPerformative(ACLMessage.INFORM);
    myAgent.send(reply);
  }
}
