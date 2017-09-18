# ZRPC

## ZRPC介绍

ZRPC是一个高效、非侵入式的RPC调用框架，采用Netty NIO传输数据，Zookeeper管理服务，Protostuff、fastjson等多种序列化方式，并提供`Engine`接口供用户自定义序列化方式。


## Architecture


                                             ----------------                     
                    Subscribe               |                |      Register    
          ----------->>>>>>>>>--------------|                |-----<<<<<<<<----  
         |                                  |    Zookeeper   |                  |  
         |                                  |                |                  |  
         |                                   ----------------                   |  
         |                                       |                              |  
         |                        Notify         |                              |  
         |                   -----<<<<<----------                               |  
         |                  |                                                   |  
         |                  |                                                   |    
         |                  |                                                   |  
         |                  |                                                   |  
         |                  |                                                   |   
         |                  |                                                   |    
         |            --------------                                 --------------------    
         |           |              |     RPC INVOKE(LoadBalance)   |                    |  
          -----------|   Consumer   |---------->>>----------------- |      Provider      |   
                     |              |     HeartBeat                 |                    |
                     |              |---------<<<<<<--->>>>>>>------|                    |
                      --------------                                 --------------------    
    
[详细说明](https://github.com/zhuyiren/zrpc/wiki/Architecture)




## 系统要求

[系统要求](https://github.com/zhuyiren/zrpc/wiki/%E7%B3%BB%E7%BB%9F%E8%A6%81%E6%B1%82)




## 性能

不带业务，在16core的机子上达到430000 TPS,见[Benchmark](https://github.com/zhuyiren/zrpc/wiki/Benchmark)





## 使用说明

ZRPC支持编程式和注解式两种编码方式，见详细[使用说明](https://github.com/zhuyiren/zrpc/wiki/%E4%BD%BF%E7%94%A8%E8%AF%B4%E6%98%8E)
