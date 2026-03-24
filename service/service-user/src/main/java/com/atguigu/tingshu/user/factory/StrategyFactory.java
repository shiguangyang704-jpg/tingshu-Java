package com.atguigu.tingshu.user.factory;

import com.atguigu.tingshu.user.strategy.ItemTypeStrategy;

/**
 * @author atguigu-mqx
 * @ClassName StrategyFactory
 * @description: TODO
 * @date 2026年03月09日
 * @version: 1.0
 */
public interface StrategyFactory {

    //  定义一个抽象方法：1001 1002 1003;
    ItemTypeStrategy getTypeStrategy(String itemType);
}
