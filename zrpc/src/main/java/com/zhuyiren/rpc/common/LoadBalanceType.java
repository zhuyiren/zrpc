package com.zhuyiren.rpc.common;

import java.util.HashMap;

/**
 * @author zhuyiren
 * @date 2017/9/3
 */
public enum LoadBalanceType {

    RANDOM("random"), ROUNDROBIN("RoundRobin");

    private static HashMap<String,LoadBalanceType> cacheMap=new HashMap<>();
    static {
        cacheMap.put(RANDOM.type,RANDOM);
        cacheMap.put(ROUNDROBIN.type,ROUNDROBIN);
    }
    private String type;

    LoadBalanceType(String type) {
        this.type = type;
    }


    public static LoadBalanceType of(String type) {
        LoadBalanceType result = cacheMap.get(type);
        if(result==null){
            throw new IllegalArgumentException("The LoadBalanceType is not valid");
        }
        return result;
    }
}
