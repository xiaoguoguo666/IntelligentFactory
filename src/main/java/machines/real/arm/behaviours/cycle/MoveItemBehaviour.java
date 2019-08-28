package machines.real.arm.behaviours.cycle;

import commons.tools.LoggerUtil;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import machines.real.arm.ArmHal;
import machines.real.commons.request.ArmrobotRequest;

/**
 * executor move item request.
 *
 * @author <a href="mailto:junfeng_pan96@qq.com">junfeng</a>
 * @version 1.0.0.0
 * @since 1.8
 */

public class MoveItemBehaviour extends CyclicBehaviour {
    private ArmHal hal;

    public MoveItemBehaviour() {
        super();
    }

    public void setHal(ArmHal hal) {
        this.hal = hal;
    }

    @Override
    public void action() {
        MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
        ACLMessage msg = myAgent.receive(mt);
        if (msg != null) {
            ArmrobotRequest request = null;
            try {
                request = (ArmrobotRequest) msg.getContentObject();
            } catch (UnreadableException e) {
                LoggerUtil.hal.error("Get Request Error.");
                e.printStackTrace();
            }
            if (request != null) {
                String from = request.getFrom();
                String to = request.getTo();
                String goodsid = request.getGoodsid();
                Integer step = request.getStep();
                if (hal.moveItem(from, to, goodsid, step)) {
                    LoggerUtil.hal.info(String.format("Move item from %s to %s, goodsid: %s, step: %d.",
                            from, to, goodsid, step));
                    // 搬运完成通知
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    myAgent.send(reply);
                } else {
                    LoggerUtil.hal.error(String.format("Failed! Move item from %s to %s, goodsid: %s, step: %d.",
                            from, to, goodsid, step));
                }
            } else {
                LoggerUtil.hal.error("Request NEP Error.");
            }
        } else {
            block(1000);
        }


    }
}
