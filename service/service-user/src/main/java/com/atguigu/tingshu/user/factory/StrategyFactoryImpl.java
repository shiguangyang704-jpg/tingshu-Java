package com.atguigu.tingshu.user.factory;

import com.atguigu.tingshu.user.strategy.ItemTypeStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author atguigu-mqx
 * @ClassName StrategyFactoryImpl
 * @description: TODO
 * @date 2026年03月09日
 * @version: 1.0
 */
@Service
public class StrategyFactoryImpl implements StrategyFactory {
    //  直接从spring 中获取对应的map集合;
    @Autowired
    private Map<String, ItemTypeStrategy> strategyMap;

    @Override
    public ItemTypeStrategy getTypeStrategy(String itemType) {
        if (strategyMap.containsKey(itemType)) {
            //  根据itemType 获取对应的策略对象;
            ItemTypeStrategy itemTypeStrategy = strategyMap.get(itemType);
            //  返回对应的策略类
            return itemTypeStrategy;
        }
        //  默认返回
        return null;
    }
}
