package machines.real.agv.actions;

import machines.real.agv.MultiAgvHal;

/**
 * 获取agv当前位置点.
 *
 * @author <a href="mailto:junfeng_pan96@qq.com">junfeng</a>
 * @version 1.0.0.0
 * @since 1.8
 */
public class GetPositionAction extends AbstractMachineAction {

  @Override
  public String getCmd() {
    return MultiAgvHal.CMD_GET_POSTION;
  }

  @Override
  public Object getExtra() {
    return "";
  }
}
