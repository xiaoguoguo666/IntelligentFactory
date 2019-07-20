package machines.virtual.cloud;


import machines.virtual.cloud.behaviours.cycle.DetectUpdateMsg;
import machines.virtual.cloud.behaviours.cycle.GetOrder;
import machines.virtual.cloud.behaviours.cycle.HandleOrders;
import commons.tools.*;
import commons.AgentTemplate;
import commons.order.OrderInfo;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.domain.DFService;
import jade.domain.FIPAException;

import java.util.Map;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * cloud，负责与云端交互.
 * 1. 定时尝试获取新订单.
 * 2. 定时检测是否有新消息需要推送.
 * 3. 对新订单解析，并向仓库发起招标
 *
 * @author <a href="mailto:junfeng_pan96@qq.com">junfeng</a>
 * @version 1.0.0.1
 * @since 1.8
 */
public class CloudAgent extends AgentTemplate {
    /**
     * TICKER周期，单位ms
     */
    private static final int TICKER_TIME = 10000;
    /**
     * 新状态推送列表.
     */
    Queue<String> stateQueue = new LinkedBlockingDeque<>();
    /**
     * 新位置推送列表.
     */
    Queue<String> posQueue = new LinkedBlockingDeque<>();
    /**
     * 云端地址.
     */
    private String website;
    /**
     * mysql配置信息
     */
    private Map<String, String> mysqlSetting;
    /**
     * 新订单列表.
     */
    private Queue<OrderInfo> orderQueue = new LinkedBlockingDeque<>();

    public String getWebsite() {
        return website;
    }

    public Map<String, String> getMysqlSetting() {
        return mysqlSetting;
    }

    public Queue<OrderInfo> getOrderQueue() {
        return orderQueue;
    }

    public Queue<String> getStateQueue() {
        return stateQueue;
    }

    public Queue<String> getPosQueue() {
        return posQueue;
    }

    @Override
    protected void setup() {
        super.setup();
        // 载入未完成订单
        //loadOrders();
        // 注册DF服务 订单状态更新服务
        loadIni();
        registerDF(DFServiceType.CLOUD_UPDATE);


        ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
        //独立线程 定时从云端 获取新订单
        Behaviour b = new GetOrder(this, TICKER_TIME);
        addBehaviour(tbf.wrap(b));

        // 独立线程 定时检查新推送消息
        b = new DetectUpdateMsg(this, TICKER_TIME);
        addBehaviour(tbf.wrap(b));

        // 独立线程 处理获得的订单
        b = new HandleOrders(this, TICKER_TIME);
        addBehaviour(tbf.wrap(b));


    }

    /**
     * 载入INI文件 并读取对应配置
     */
    protected void loadIni() {
        Map<String, String> setting = IniLoader.load(getLocalName());
        mysqlSetting = IniLoader.load(IniLoader.SECTION_MYSQL);
        website = setting.get("website");

        if (website == null) {
            LoggerUtil.agent.error("Load website info error. Null pointer");
            System.exit(2);
        }
    }

    /**
     * 从mysql数据库中载入未完成订单
     */
    private void loadOrders() {
        Vector<OrderInfo> list = new CloudMysql(mysqlSetting).loadOrders();
        for (OrderInfo oi : list) {
            orderQueue.offer(oi);
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
            LoggerUtil.agent.error("Deregister DF service error!" + fe.getMessage());
        }
    }
}
