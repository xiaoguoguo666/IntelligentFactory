package machines.real.commons.behaviours.cycle;


import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import machines.real.commons.RealMachineAgent;
import machines.real.commons.buffer.Buffer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 挑选工件装载入机床
 *
 * @author <a href="mailto:junfeng_pan96@qq.com">junfeng</a>
 * @version 1.0.0.0
 * @since 1.8
 */

public class LoadItemBehaviour extends CyclicBehaviour {
    private RealMachineAgent machineAgent;
    private Class processClass;

    public LoadItemBehaviour(RealMachineAgent machineAgent, Class processClass) {
        this.machineAgent = machineAgent;
        this.processClass = processClass;
    }

    @Override
    public void action() {
        // 机床空闲
        if (!machineAgent.getMachineState().isBusy()) {
            // 获取等待加工的buffer
            Buffer buffer = machineAgent.getBufferManger().getWaitingBuffer();
            if (buffer != null) {
                // 请求机械手 装载入机床
                machineAgent.getMachineState().setBusy();
                String password = machineAgent.getArmPwd();
                // 利用反射进行加工处理
                try {
                    Constructor constructor = processClass.getConstructor(machineAgent.getClass(), Buffer.class, String.class);
                    constructor.setAccessible(true);
                    Object instance = constructor.newInstance(machineAgent, buffer, password);
                    Method getBehaviour = processClass.getMethod("getBehaviour");
                    Behaviour b = (Behaviour) getBehaviour.invoke(instance);
                    myAgent.addBehaviour(b);
                } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        block(1000);
    }
}