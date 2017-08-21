/*
 * Copyright 2017 The ZRPC Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhuyiren.rpc.utils;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @author zhuyiren
 * @date 2017/8/21
 */
public class ZookeeperUtils {



    public static String createNode(ZooKeeper zk, String path,byte[] data, List<ACL> acls, CreateMode createMode) throws InterruptedException,KeeperException{
        if(!StringUtils.hasText(path) || !path.startsWith("/") || path.endsWith("/")){
            throw new IllegalArgumentException("path must not be null or empty");
        }

        int currentIndex=0;
        while (true) {
            if((currentIndex = path.indexOf("/", currentIndex+1))==-1){
                break;
            }
            String currentDirectory = path.substring(0, currentIndex);
            if(zk.exists(currentDirectory,false)!=null){
                continue;
            }
            zk.create(currentDirectory,null,acls,CreateMode.PERSISTENT);
        }

        zk.create(path,data,acls,createMode);
        return path;

    }
}
