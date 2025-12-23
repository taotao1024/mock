package com.taotao.drools.mock.a.mock;

import com.taotao.drools.mock.a.model.Order;
import com.taotao.drools.mock.a.model.User;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;

public class DiscountRule {
    public static void main(String[] args) {
        // 1. 初始化Drools引擎
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        // 加载文件
        kieFileSystem.write(ResourceFactory.newClassPathResource("rules/discount-rule.drl"));
        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();
        KieModule kieModule = kieBuilder.getKieModule();
        KieContainer kieContainer = kieServices.newKieContainer(kieModule.getReleaseId());
        // 获取KieBase 执行规则的基础设施
        KieBase kieBase = kieContainer.getKieBase();
        KieSession kieSession = kieBase.newKieSession();

        // 2. 常见事实对象（金额150元）
        Order order = new Order(150.0);
        User user = new User(true);

        // 3. 插入数据到规则引擎的工作内存
        kieSession.insert(order);
        kieSession.insert(user);

        // 4. 触发所有规则
        kieSession.fireAllRules();
        // 5. 销毁会话（释放资源）
        kieSession.dispose();

        // 输出结果
        System.out.println("订单金额：" + order.getAmount() + "元");
        System.out.println("最终折扣：" + order.getDiscount() + "元");
    }
}
