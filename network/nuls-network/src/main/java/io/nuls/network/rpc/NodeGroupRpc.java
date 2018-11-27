/*
 * MIT License
 *
 * Copyright (c) 2017-2018 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package io.nuls.network.rpc;

import io.nuls.network.constant.NetworkConstant;
import io.nuls.network.constant.NetworkErrorCode;
import io.nuls.network.constant.NetworkParam;
import io.nuls.network.manager.NodeGroupManager;
import io.nuls.network.manager.StorageManager;
import io.nuls.network.model.NodeGroup;
import io.nuls.network.model.NodeGroupConnector;
import io.nuls.network.model.po.NodeGroupPo;
import io.nuls.network.model.vo.NodeGroupVo;
import io.nuls.network.storage.DbService;
import io.nuls.rpc.cmd.BaseCmd;
import io.nuls.rpc.model.CmdAnnotation;
import io.nuls.rpc.model.Parameter;
import io.nuls.rpc.model.message.Response;
import io.nuls.tools.log.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @program: nuls2.0
 * @description: 远程调用接口
 * @author: lan
 * @create: 2018/11/07
 **/
public class NodeGroupRpc extends BaseCmd {
NodeGroupManager nodeGroupManager=NodeGroupManager.getInstance();
DbService dbService=StorageManager.getInstance().getDbService();
    /**
     * nw_createNodeGroup
     * 创建跨链网络
     */
    @CmdAnnotation(cmd = "nw_createNodeGroup", version = 1.0,
            description = "createNodeGroup")
    @Parameter(parameterName = "chainId", parameterType = "int", parameterValidRange = "[1,65535]", parameterValidRegExp = "")
    @Parameter(parameterName = "magicNumber", parameterType = "long", parameterValidRange = "[1,4294967295]")
    @Parameter(parameterName = "maxOut", parameterType = "int",parameterValidRange = "[1,65535]")
    @Parameter(parameterName = "maxIn", parameterType = "int",parameterValidRange = "[1,65535]")
    @Parameter(parameterName = "minAvailableCount", parameterType = "int",parameterValidRange = "[1,65535]")
    @Parameter(parameterName = "seedIps", parameterType = "String")
    @Parameter(parameterName = "isMoonNode", parameterType = "int",parameterValidRange = "[0,1]")
    public Response createNodeGroup(Map params) {
        List<NodeGroupPo> nodeGroupPos=new ArrayList<>();
        int chainId = Integer.valueOf(String.valueOf(params.get("chainId")));
        long magicNumber = Long.valueOf(String.valueOf(params.get("magicNumber")));
        int maxOut = Integer.valueOf(String.valueOf(params.get("maxOut")));
        int maxIn = Integer.valueOf(String.valueOf(params.get("maxIn")));
        int minAvailableCount = Integer.valueOf(String.valueOf(params.get("minAvailableCount")));
        String seedIps=String.valueOf(params.get("seedIps"));
        int isMoonNode=Integer.valueOf(String.valueOf(params.get("isMoonNode")));
        boolean isMoonNet = isMoonNode == 1 ? true : false;
        //友链创建的是链工厂，isSelf 为true
        boolean isSelf = isMoonNet? false : true;
        if(!NetworkParam.getInstance().isMoonNode()){
            Log.info("MoonNode is false，but param isMoonNode is 1");
            return failed(NetworkErrorCode.PARAMETER_ERROR);
        }
        NodeGroup nodeGroup= nodeGroupManager.getNodeGroupByMagic(magicNumber);
        if(null != nodeGroup){
            Log.info("getNodeGroupByMagic: nodeGroup  exist");
            return failed(NetworkErrorCode.PARAMETER_ERROR);
        }
        nodeGroup = new NodeGroup(magicNumber,chainId,maxIn,maxOut,minAvailableCount,true);
        nodeGroup.setSelf(isSelf);
        //存储nodegroup
        nodeGroupPos.add((NodeGroupPo)nodeGroup.parseToPo());
        dbService.saveNodeGroups(nodeGroupPos);
        nodeGroupManager.addNodeGroup(nodeGroup.getChainId(),nodeGroup);
        // 成功
        return success();
    }
    /**
     * nw_activeCross
     * 友链激活跨链
     */
    @CmdAnnotation(cmd = "nw_activeCross", version = 1.0,
            description = "activeCross")
    @Parameter(parameterName = "chainId", parameterType = "int", parameterValidRange = "[1,65535]", parameterValidRegExp = "")
    @Parameter(parameterName = "maxOut", parameterType = "int",parameterValidRange = "[1,65535]")
    @Parameter(parameterName = "maxIn", parameterType = "int",parameterValidRange = "[1,65535]")
    @Parameter(parameterName = "seedIps", parameterType = "String")
    public Response activeCross(Map  params) {
        List<NodeGroupPo> nodeGroupPos=new ArrayList<>();
        int chainId = Integer.valueOf(String.valueOf(params.get("chainId")));
        int maxOut = Integer.valueOf(String.valueOf(params.get("maxOut")));
        int maxIn = Integer.valueOf(String.valueOf(params.get("maxIn")));
        String seedIps=String.valueOf(params.get("seedIps"));
        //友链的跨链协议调用
        NodeGroup nodeGroup= nodeGroupManager.getNodeGroupByChainId(chainId);
        if(null == nodeGroup){
            Log.info("getNodeGroupByMagic is null");
            return failed(NetworkErrorCode.PARAMETER_ERROR);
        }if(chainId != nodeGroup.getChainId()){
            Log.info("chainId != nodeGroup.getChainId()");
            return failed(NetworkErrorCode.PARAMETER_ERROR);
        }
        nodeGroup.setMaxCrossIn(maxIn);
        nodeGroup.setMaxCrossOut(maxOut);
        List<String> ipList = new ArrayList<>();
        String [] ips = seedIps.split(NetworkConstant.COMMA);
        for (String ip : ips) {
            ipList.add(ip);
        }
        NetworkParam.getInstance().setMoonSeedIpList(ipList);
        nodeGroup.setCrossActive(true);
        return success();
    }
    /**
     * nw_getGroupByChainId
     * 查看指定网络组信息
     */
    @CmdAnnotation(cmd = "nw_getGroupByChainId", version = 1.0,
            description = "getGroupByChainId")
    @Parameter(parameterName = "chainId", parameterType = "int", parameterValidRange = "[1,65535]", parameterValidRegExp = "")
    public Response getGroupByChainId(Map  params) {
        int chainId = Integer.valueOf(String.valueOf(params.get("chainId")));
        NodeGroup nodeGroup=NodeGroupManager.getInstance().getNodeGroupByChainId(chainId);
        NodeGroupVo  nodeGroupVo=buildNodeGroupVo(nodeGroup);
        return success(nodeGroupVo );
    }

    private NodeGroupVo buildNodeGroupVo(NodeGroup nodeGroup){
        NodeGroupVo nodeGroupVo=new NodeGroupVo();
        nodeGroupVo.setChainId(nodeGroup.getChainId());
        nodeGroupVo.setMagicNumber(nodeGroup.getMagicNumber());
        NodeGroupConnector nodeGroupConnector=nodeGroup.getHightestNodeGroupInfo();
        if(null != nodeGroupConnector){
            nodeGroupVo.setBlockHash(nodeGroupConnector.getBlockHash());
            nodeGroupVo.setBlockHeight(nodeGroupConnector.getBlockHeight());
        }
        nodeGroupVo.setInCount(nodeGroup.getHadConnectIn());
        nodeGroupVo.setOutCount(nodeGroup.getHadConnectOut());
        nodeGroupVo.setInCrossCount(nodeGroup.getHadCrossConnectIn());
        nodeGroupVo.setOutCrossCount(nodeGroup.getHadCrossConnectOut());
        //网络连接，并能正常使用
        nodeGroupVo.setIsActive(nodeGroup.isActive()? 1 : 0);
        //跨链模块是否可用
        nodeGroupVo.setIsCrossActive(nodeGroup.isCrossActive()? 1 : 0);
        nodeGroupVo.setIsMoonNet(nodeGroup.isMoonNet()? 1 : 0);
        nodeGroupVo.setTotalCount(nodeGroupVo.getInCount()+nodeGroupVo.getOutCount()+nodeGroupVo.getInCrossCount()+nodeGroupVo.getOutCrossCount());
        return nodeGroupVo;
    }
    /**
     * nw_delNodeGroup
     * 注销指定网络组信息
     */
    @CmdAnnotation(cmd = "nw_delNodeGroup", version = 1.0,
            description = "delGroupByChainId")
    @Parameter(parameterName = "chainId", parameterType = "int", parameterValidRange = "[1,65535]", parameterValidRegExp = "")
    public Response delGroupByChainId(Map  params) {
        int chainId = Integer.valueOf(String.valueOf(params.get("chainId")));
        dbService.deleteGroup(chainId);
        dbService.deleteGroupNodeKeys(chainId);
        //删除网络连接
        NodeGroup nodeGroup=NodeGroupManager.getInstance().getNodeGroupByChainId(chainId);
        nodeGroup.destroy();
        return success();
    }

    /**
     * nw_getSeeds
     * 查询跨链种子节点
     */
    @CmdAnnotation(cmd = "nw_getSeeds", version = 1.0,
            description = "delGroupByChainId")
    @Parameter(parameterName = "chainId", parameterType = "int", parameterValidRange = "[1,65535]", parameterValidRegExp = "")
    public Response getCrossSeeds(List  params) {
        int chainId = Integer.valueOf(String.valueOf(params.get(0)));
        Log.info("chainId:"+chainId);
        List<String> seeds=NetworkParam.getInstance().getMoonSeedIpList();
        if(null == seeds){
            return success();
        }
        StringBuffer seedsStr=new StringBuffer();
        for(String seed:seeds){
            seedsStr.append(seed);
            seedsStr.append(",");
        }
        if(seedsStr.length()> 0) {
            return success(  seedsStr.substring(0, seedsStr.length()));
        }
        return success( );
    }




    /**
     * nw_reconnect
     * 重连网络
     */
//    @CmdAnnotation(cmd = "nw_reconnect", version = 1.0, preCompatible = true)
    @CmdAnnotation(cmd = "nw_reconnect", version = 1.0,
            description = "reconnect")
    @Parameter(parameterName = "chainId", parameterType = "int", parameterValidRange = "[1,65535]", parameterValidRegExp = "")
    public Response reconnect(Map  params) {
        int chainId = Integer.valueOf(String.valueOf(params.get("chainId")));
        Log.info("chainId:"+chainId);
        NodeGroup nodeGroup=NodeGroupManager.getInstance().getNodeGroupByChainId(chainId);
        nodeGroup.reconnect();
        return success();
    }

    /**
     * nw_getGroups
     * 重连网络
     */
    @CmdAnnotation(cmd = "nw_getGroups", version = 1.0,
            description = "getGroups")
    @Parameter(parameterName = "startPage", parameterType = "int", parameterValidRange = "[0,65535]", parameterValidRegExp = "")
    @Parameter(parameterName = "pageSize", parameterType = "int", parameterValidRange = "[0,65535]", parameterValidRegExp = "")
    public Response getGroups(Map  params) {
        int startPage=Integer.valueOf(String.valueOf(params.get("startPage")));
        int pageSize=Integer.valueOf(String.valueOf(params.get("pageSize")));
        List<NodeGroup> nodeGroups=nodeGroupManager.getNodeGroups();
        int total=nodeGroups.size();
        List<NodeGroupVo> pageList=new ArrayList<>();
        if(startPage == 0 && pageSize == 0){
          for(NodeGroup nodeGroup:nodeGroups){
              pageList.add(buildNodeGroupVo(nodeGroup));
          }
        }else {
            int currIdx = (startPage > 1 ? (startPage - 1) * pageSize : 0);
            for (int i = 0; i < pageSize && i < (total - currIdx); i++) {
                NodeGroup nodeGroup = nodeGroups.get(currIdx + i);
                NodeGroupVo nodeGroupVo = buildNodeGroupVo(nodeGroup);
                pageList.add(nodeGroupVo);
            }
        }
        return success(pageList);
    }

}