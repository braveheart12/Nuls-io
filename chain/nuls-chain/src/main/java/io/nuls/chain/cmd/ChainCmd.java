package io.nuls.chain.cmd;


import io.nuls.base.basic.AddressTool;
import io.nuls.chain.model.dto.Chain;
import io.nuls.chain.model.tx.CrossChainDestroyTransaction;
import io.nuls.chain.model.tx.CrossChainRegTransaction;
import io.nuls.chain.service.AssetService;
import io.nuls.chain.service.ChainService;
import io.nuls.chain.service.RpcService;
import io.nuls.rpc.cmd.BaseCmd;
import io.nuls.rpc.model.CmdAnnotation;
import io.nuls.rpc.model.Parameter;
import io.nuls.rpc.model.message.Response;
import io.nuls.tools.constant.ErrorCode;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Component;
import io.nuls.tools.data.ByteUtils;
import io.nuls.tools.log.Log;
import io.nuls.tools.thread.TimeService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author tangyi
 * @date 2018/11/7
 * @description
 */
@Component
public class ChainCmd extends BaseCmd {

    @Autowired
    private ChainService chainService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private RpcService rpcService;
    @CmdAnnotation(cmd = "cm_chain", version = 1.0,
            description = "chain")
    @Parameter(parameterName = "chainId", parameterType = "int", parameterValidRange = "[1,65535]", parameterValidRegExp = "")
    public Response chain(Map params) {
        try {
            int chainId = Integer.valueOf(params.get("chainId").toString());
            Chain chain = chainService.getChain(chainId);
            if (chain == null) {
                return failed("C10003");
            }
            return success(chain);
        } catch (Exception e) {
            Log.error(e);
            return failed(ErrorCode.init("-100"));
        }
    }

    @CmdAnnotation(cmd = "cm_chainReg", version = 1.0,description = "chainReg")
    @Parameter(parameterName = "chainId", parameterType = "int", parameterValidRange = "[1,65535]", parameterValidRegExp = "")
    @Parameter(parameterName = "name", parameterType = "String")
    @Parameter(parameterName = "addressType", parameterType = "String")
    @Parameter(parameterName = "magicNumber", parameterType = "long", parameterValidRange = "[1,4294967295]", parameterValidRegExp = "")
    @Parameter(parameterName = "supportInflowAsset", parameterType = "String")
    @Parameter(parameterName = "minAvailableNodeNum", parameterType = "int", parameterValidRange = "[1,65535]", parameterValidRegExp = "")
    @Parameter(parameterName = "singleNodeMinConnectionNum", parameterType = "int")
    @Parameter(parameterName = "txConfirmedBlockNum", parameterType = "int")
    @Parameter(parameterName = "address", parameterType = "String")
    @Parameter(parameterName = "symbol", parameterType = "array")
    @Parameter(parameterName = "name", parameterType = "String")
    @Parameter(parameterName = "initNumber", parameterType = "long", parameterValidRange = "[1,4294967295]", parameterValidRegExp = "")
    @Parameter(parameterName = "decimalPlaces", parameterType = "short", parameterValidRange = "[1,128]", parameterValidRegExp = "")
    public Response chainReg(Map params) {
        try {
            Chain chain = new Chain();
            chain.setChainId(Integer.valueOf(params.get("chainId").toString()));
            chain.setName((String) params.get("name"));
            chain.setAddressType((String) params.get("addressType"));
            chain.setMagicNumber(Long.valueOf(params.get("magicNumber").toString()));
            chain.setSupportInflowAsset(Boolean.valueOf(params.get("supportInflowAsset").toString()));
            chain.setMinAvailableNodeNum(Integer.valueOf(params.get("minAvailableNodeNum").toString()));
            chain.setSingleNodeMinConnectionNum(Integer.valueOf(params.get("singleNodeMinConnectionNum").toString()));
            chain.setTxConfirmedBlockNum(Integer.valueOf(params.get("txConfirmedBlockNum").toString()));
            Map<String,Object> paramMap = new HashMap<>();
            paramMap.put("chainId",chain.getChainId());
            chain.setAddress(AddressTool.getAddress(String.valueOf(params.get("address"))));
            chain.setCreateTime(TimeService.currentTimeMillis());
            Chain dbChain = chainService.getChain(chain.getChainId());
            if (dbChain != null) {
                return failed("C10001");
            }
            // 组装交易发送
            CrossChainRegTransaction crossChainRegTransaction = new CrossChainRegTransaction();
            crossChainRegTransaction.setTxData(chain.parseToTransaction());
            //TODO:coindata 未封装
            boolean rpcReslt = rpcService.newTx(crossChainRegTransaction);
            if(rpcReslt) {
                return success(chain);
            }else{
                return failed(new ErrorCode());
            }
        } catch (Exception e) {
            Log.error(e);
            return failed(ErrorCode.init("-100"));
        }
    }



    /**
     * 删除链
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = "cm_chainDestroy", version = 1.0,description = "chainDestroy")
    @Parameter(parameterName = "chainId", parameterType = "int", parameterValidRange = "[1,65535]", parameterValidRegExp = "")
    @Parameter(parameterName = "address", parameterType = "String")
    public Response chainDestroy(Map params) {
        int chainId = Integer.valueOf(params.get("chainId").toString());
       byte [] address = (AddressTool.getAddress(String.valueOf(params.get("address"))));
        //身份的校验，地址账户校验
        Chain chain = chainService.getChain(chainId);
        if (chain == null) {
            return failed("C10003");
        }
        if(!ByteUtils.arrayEquals(chain.getAddress(),address)){
            return failed("C10004");
        }
        CrossChainDestroyTransaction crossChainDestroyTransaction = new CrossChainDestroyTransaction();
        try {
            crossChainDestroyTransaction.setTxData(chain.parseToTransaction());
            //TODO:coindata 未封装
            boolean rpcReslt = rpcService.newTx(crossChainDestroyTransaction);
            if(rpcReslt) {
                return success(chain);
            }else{
                return failed(new ErrorCode());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return failed(new ErrorCode());
    }



//    public CmdResponse setChainAssetCurrentNumber(List params) {
//        short chainId = Short.valueOf(params.get(0).toString());
//        long assetId = Long.valueOf(params.get(1).toString());
//        long currentNumber = Long.valueOf(params.get(2).toString());
//        chainService.setAssetNumber(chainId, assetId, currentNumber);
//        return success("setChainAssetCurrentNumber", null);
//    }
//
//    public CmdResponse setChainAssetCurrentNumberValidator(List params) {
//        long assetId = Long.valueOf(params.get(1).toString());
//        long currentNumber = Long.valueOf(params.get(2).toString());
//        Asset asset = assetService.getAsset(assetId);
//        if (currentNumber > asset.getInitNumber()) {
//            return failed(CmConstants.ERROR_ASSET_EXCEED_INIT);
//        }
//        return success();
//    }

}