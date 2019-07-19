package machines.real.commons.hal;

/**
 * 硬件层解析指令失败 异常.
 *
 * @author <a href="mailto:junfeng_pan96@qq.com">junfeng</a>
 * @version 1.0.0.0
 * @since 1.8
 */


public class CmdParseException extends HALException {

    public CmdParseException(String message) {
        super(message);
    }
}