package com.ormcore.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * spring容器
 *
 * @author 2014-11-27 上午11:23:06
 */
public class GameContext {
    private static final Logger logger = LoggerFactory.getLogger(GameContext.class);

    /**
     * spring 配置文件路径
     */
    private static String confPath =
            System.getProperty("file.separator")
                    + System.getProperty("user.dir")
                    + System.getProperty("file.separator") + "conf"
                    + System.getProperty("file.separator");

    /**
     * spring容器
     */
    private static ApplicationContext context = null;

    public GameContext(String path) {
        if (path != null) {
            if ("".equals(path)) path = this.getClass().getResource("/").getPath();
            confPath = System.getProperty("file.separator")
                    + path + System.getProperty("file.separator") + "conf"
                    + System.getProperty("file.separator");
        }
        logger.info("confPath：{}", confPath);

        String[] files = {
                confPath + "applicationContext.xml"
                , confPath + "mybatis.xml"
                , confPath + "applicationContext-task.xml"
        };

        for (String file : files) {
            logger.info("file：{}", file);
        }
        context = new FileSystemXmlApplicationContext(files);
        logger.info("初始化spring配置结束...");
    }

    /**
     * 获取Spring容器管理的对象
     *
     * @param beanName
     * @return
     * @author haojian
     * Apr 8, 2013 4:46:11 PM
     */
    public static Object getBean(String beanName) {
        return context.getBean(beanName);
    }

    public static void main(String[] args) {
        String s = GameContext.class.getResource("").getPath();
        s = s.substring(1, s.indexOf("EpGateTrunk"));
        System.out.println(s);
    }
}
