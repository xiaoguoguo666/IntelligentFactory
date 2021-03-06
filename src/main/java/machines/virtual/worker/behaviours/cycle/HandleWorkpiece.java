package machines.virtual.worker.behaviours.cycle;

import commons.order.WorkpieceStatus;
import commons.tools.DfServiceType;
import commons.tools.DfUtils;
import commons.tools.LoggerUtil;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import machines.virtual.worker.behaviours.simple.MyContractNetInitiator;

import java.util.List;
import java.util.Random;

/**
 * 处理工件招投标.
 *
 * @author <a href="mailto:junfeng_pan96@qq.com">junfeng</a>
 * @version 1.0.0.0
 * @since 1.8
 */


public class HandleWorkpiece extends CyclicBehaviour {
    private Integer detectRatio;

    public HandleWorkpiece() {
        super();
    }

    public void setDetectRatio(Integer detectRatio) {
        this.detectRatio = detectRatio;
    }

    @Override
    public void action() {
        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST));
        ACLMessage msg = myAgent.receive(mt);
        if (msg == null) {
            block();
            return;
        }
        WorkpieceStatus wpInfo = null;
        try {
            wpInfo = (WorkpieceStatus) msg.getContentObject();
            LoggerUtil.agent.info("Receive Request From: " + msg.getSender().getName());
        } catch (UnreadableException e) {
            e.printStackTrace();
        }
        if (wpInfo == null) {
            block();
            return;
        }
        // 重置加工工步
        wpInfo.resetProcessStep();

        List<String> processPlan = wpInfo.getProcessPlan();
        int processIndex = wpInfo.nextProcessIndex();
        if (processIndex >= processPlan.size()) {
            // 所有工艺完成，回成品库
            LoggerUtil.agent.info(String.format("OrderId: %s, wpId: %s done.",
                    wpInfo.getOrderId(), wpInfo.getWorkpieceId()));
            processOn(wpInfo, DfServiceType.PRODUCT);
            return;
        }

        String process = processPlan.get(processIndex);
        if (process.equals(DfServiceType.WAREHOUSE)) {
            addVisionProcess(wpInfo);
        }
        processOn(wpInfo, process);
    }

    private void processOn(WorkpieceStatus wpInfo, String serviceType) {
        try {
            ACLMessage msg = DfUtils.createCfpMsg(wpInfo);
            if (serviceType.equals(DfServiceType.PRODUCT)) {
                DfUtils.searchDf(myAgent, msg, DfServiceType.WAREHOUSE);
                msg.setLanguage("PRODUCT");
            } else if (serviceType.equals(DfServiceType.WAREHOUSE)) {
                DfUtils.searchDf(myAgent, msg, serviceType);
                msg.setLanguage("RAW");
            } else {
                DfUtils.searchDf(myAgent, msg, serviceType);
            }

            Behaviour b = new MyContractNetInitiator(myAgent, msg, serviceType);
            myAgent.addBehaviour(b);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addVisionProcess(WorkpieceStatus wpInfo) {
        int random = new Random().nextInt(100);
        if (random < detectRatio) {
            LoggerUtil.agent.info(String.format("Item orderId:%s, wpId:%s add vision detect",
                    wpInfo.getOrderId(), wpInfo.getWorkpieceId()));
            wpInfo.getProcessPlan().add(DfServiceType.VISION);
        }
    }
}
