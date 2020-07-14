package machines.real.commons.request;

import commons.order.WorkpieceStatus;
import java.io.Serializable;

/**
 * 仓库传送带运动请求.
 *
 * @author <a href="mailto:junfeng_pan96@qq.com">junfeng</a>
 * @version 1.0.0.0
 * @since 1.8
 */
public class WarehouseConveyorRequest implements Serializable {

  public static final String LANGUAGE = "CONVEYOR";
  private final boolean importMode;
  private final String orderId;
  private final String workpieceId;

  /**
   * 仓库传送带控制请求.
   *
   * @param importMode 进出货模式
   * @param wpInfo     工件信息
   */
  public WarehouseConveyorRequest(boolean importMode, WorkpieceStatus wpInfo) {
    this.importMode = importMode;
    this.orderId = wpInfo.getOrderId();
    this.workpieceId = wpInfo.getWorkpieceId();
  }

  public boolean isImportMode() {
    return importMode;
  }

  public String getOrderId() {
    return orderId;
  }

  public String getWorkpieceId() {
    return workpieceId;
  }
}
