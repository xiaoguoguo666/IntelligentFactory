package machines.real.commons.hal;

import commons.WorkpieceInfo;

/**
 * 机床Hal 包含 加工和时间预估 两个功能
 *
 * @author <a href="mailto:junfeng_pan96@qq.com">junfeng</a>
 * @version 1.0.0.0
 * @since 1.8
 */

public class MachineHal extends BaseHal {
    private static final String CMD_PROCESS = "process";
    private static final String CMD_EVALUATE = "evaluate";

    public MachineHal() {
        super();
    }

    public MachineHal(int port) {
        super(port);
    }

    public boolean process(WorkpieceInfo wpInfo) {
        return executeCmd(CMD_PROCESS, wpInfo);
    }

    public int evaluate(WorkpieceInfo wpInfo) {
        if(executeCmd(CMD_EVALUATE, wpInfo)){
            return (int) Float.parseFloat((String)getExtraInfo());
        } else {
            return 0;
        }
    }
}