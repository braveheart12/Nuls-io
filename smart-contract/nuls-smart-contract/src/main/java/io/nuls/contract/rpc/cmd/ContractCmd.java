/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.nuls.contract.rpc.cmd;

import io.nuls.base.data.Transaction;
import io.nuls.contract.helper.ContractHelper;
import io.nuls.contract.manager.ContractTxProcessorManager;
import io.nuls.contract.manager.ContractTxValidatorManager;
import io.nuls.contract.model.bo.ContractResult;
import io.nuls.contract.model.bo.ContractTempTransaction;
import io.nuls.contract.model.bo.ContractWrapperTransaction;
import io.nuls.contract.model.dto.ContractPackageDto;
import io.nuls.contract.model.tx.CallContractTransaction;
import io.nuls.contract.model.tx.CreateContractTransaction;
import io.nuls.contract.model.tx.DeleteContractTransaction;
import io.nuls.contract.model.txdata.CallContractData;
import io.nuls.contract.model.txdata.CreateContractData;
import io.nuls.contract.model.txdata.DeleteContractData;
import io.nuls.contract.processor.CallContractTxProcessor;
import io.nuls.contract.processor.CreateContractTxProcessor;
import io.nuls.contract.processor.DeleteContractTxProcessor;
import io.nuls.contract.service.ContractService;
import io.nuls.contract.util.MapUtil;
import io.nuls.rpc.cmd.BaseCmd;
import io.nuls.rpc.model.CmdAnnotation;
import io.nuls.rpc.model.Parameter;
import io.nuls.rpc.model.message.Response;
import io.nuls.tools.basic.Result;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Component;
import io.nuls.tools.log.Log;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nuls.contract.constant.ContractCmdConstant.*;
import static io.nuls.contract.constant.ContractConstant.*;

/**
 * @author: PierreLuo
 * @date: 2019-03-11
 */
@Component
public class ContractCmd extends BaseCmd {

    @Autowired
    private ContractService contractService;
    @Autowired
    private ContractHelper contractHelper;
    @Autowired
    private ContractTxProcessorManager contractTxProcessorManager;
    @Autowired
    private ContractTxValidatorManager contractTxValidatorManager;

    @CmdAnnotation(cmd = INVOKE_CONTRACT, version = 1.0, description = "invoke contract")
    @Parameter(parameterName = "chainId", parameterType = "int")
    @Parameter(parameterName = "blockHeight", parameterType = "long")
    @Parameter(parameterName = "blockTime", parameterType = "long")
    @Parameter(parameterName = "packingAddress", parameterType = "String")
    @Parameter(parameterName = "preStateRoot", parameterType = "String")
    @Parameter(parameterName = "txHexList", parameterType = "List<String>")
    public Response invokeContract(Map<String,Object> params){
        try {
            Integer chainId = (Integer) params.get("chainId");
            Long blockHeight = Long.parseLong(params.get("blockHeight").toString()) ;
            Long blockTime = Long.parseLong(params.get("blockTime").toString()) ;
            String packingAddress = (String) params.get("packingAddress");
            String preStateRoot = (String) params.get("preStateRoot");
            List<String> txHexList = (List<String>)params.get("txHexList");

            List<ContractTempTransaction> txList = new ArrayList<>();
            ContractTempTransaction tx;
            for(String txHex : txHexList) {
                tx = new ContractTempTransaction();
                tx.setTxHex(txHex);
                tx.parse(Hex.decode(txHex), 0);
                txList.add(tx);
            }
            Result result = contractService.invokeContract(chainId, txList, blockHeight, blockTime, packingAddress, preStateRoot);
            if(result.isFailed()){
                return failed(result.getErrorCode());
            }
            ContractPackageDto dto = (ContractPackageDto) result.getData();
            List<String> resultTxHexList = new ArrayList<>();
            List<Transaction> resultTxList = dto.getResultTxList();
            for(Transaction resultTx : resultTxList) {
                resultTxHexList.add(Hex.toHexString(resultTx.serialize()));
            }

            Map<String, Object> resultMap = MapUtil.createHashMap(2);
            resultMap.put("stateRoot", Hex.toHexString(dto.getStateRoot()));
            resultMap.put("txHexList", resultTxHexList);

            return success(resultMap);
        } catch (Exception e) {
            Log.error(e);
            return failed(e.getMessage());
        }
    }


    @CmdAnnotation(cmd = CREATE_VALIDATOR, version = 1.0, description = "create contract validator")
    @Parameter(parameterName = "chainId", parameterType = "int")
    @Parameter(parameterName = "txHex", parameterType = "String")
    public Response createValidator(Map<String,Object> params){
        try {
            Integer chainId = (Integer) params.get("chainId");
            String txHex = (String) params.get("txHex");
            CreateContractTransaction tx = new CreateContractTransaction();
            tx.parse(Hex.decode(txHex), 0);
            Map<String, Boolean> result = new HashMap<>(2);
            if(tx.getType() != TX_TYPE_CREATE_CONTRACT) {
                return failed("non create contract tx");
            }
            Result validator = contractTxValidatorManager.createValidator(chainId, tx);
            if(validator.isFailed()) {
                return failed(validator.getErrorCode());
            }
            result.put("value", true);
            return success(result);
        } catch (Exception e) {
            Log.error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = CALL_VALIDATOR, version = 1.0, description = "call contract validator")
    @Parameter(parameterName = "chainId", parameterType = "int")
    @Parameter(parameterName = "txHex", parameterType = "String")
    public Response callValidator(Map<String,Object> params){
        try {
            Integer chainId = (Integer) params.get("chainId");
            String txHex = (String) params.get("txHex");
            CallContractTransaction tx = new CallContractTransaction();
            tx.parse(Hex.decode(txHex), 0);
            Map<String, Boolean> result = new HashMap<>(2);
            if(tx.getType() != TX_TYPE_CALL_CONTRACT) {
                return failed("non call contract tx");
            }
            Result validator = contractTxValidatorManager.callValidator(chainId, tx);
            if(validator.isFailed()) {
                return failed(validator.getErrorCode());
            }
            result.put("value", true);
            return success(result);
        } catch (Exception e) {
            Log.error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = DELETE_VALIDATOR, version = 1.0, description = "delete contract validator")
    @Parameter(parameterName = "chainId", parameterType = "int")
    @Parameter(parameterName = "txHex", parameterType = "String")
    public Response deleteValidator(Map<String,Object> params){
        try {
            Integer chainId = (Integer) params.get("chainId");
            String txHex = (String) params.get("txHex");
            DeleteContractTransaction tx = new DeleteContractTransaction();
            tx.parse(Hex.decode(txHex), 0);
            Map<String, Boolean> result = new HashMap<>(2);
            if(tx.getType() != TX_TYPE_DELETE_CONTRACT) {
                return failed("non delete contract tx");
            }
            Result validator = contractTxValidatorManager.deleteValidator(chainId, tx);
            if(validator.isFailed()) {
                return failed(validator.getErrorCode());
            }
            result.put("value", true);
            return success(result);
        } catch (Exception e) {
            Log.error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = INTEGRATE_VALIDATOR, version = 1.0, description = "transaction integrate validator")
    @Parameter(parameterName = "chainId", parameterType = "int")
    @Parameter(parameterName = "txHexList", parameterType = "String")
    public Response integrateValidator(Map<String,Object> params){
        try {
            Integer chainId = (Integer) params.get("chainId");
            List<String> txHexList = (List<String>) params.get("txHexList");
            /**
             *  暂无统一验证器
             */
            return success();
        } catch (Exception e) {
            Log.error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = COMMIT, version = 1.0, description = "commit contract")
    @Parameter(parameterName = "chainId", parameterType = "int")
    @Parameter(parameterName = "txHexList", parameterType = "List<String>")
    @Parameter(parameterName = "blockHeaderHex", parameterType = "String")
    public Response commit(Map<String,Object> params){
        try {
            Integer chainId = (Integer) params.get("chainId");
            List<String> txHexList = (List<String>) params.get("txHexList");
            String blockHeaderHex = (String) params.get("blockHeaderHex");

            ContractPackageDto contractPackageDto = contractHelper.getChain(chainId).getContractPackageDto();
            if(contractPackageDto != null) {
                Map<String, ContractResult> contractResultMap = contractPackageDto.getContractResultMap();
                ContractResult contractResult;
                ContractWrapperTransaction wrapperTx;
                for(String txHex : txHexList) {
                    contractResult = contractResultMap.get(txHex);
                    if(contractResult == null) {
                        Log.warn("empty contract result with txHex: {}", txHex);
                        continue;
                    }
                    wrapperTx = contractResult.getTx();
                    wrapperTx.setContractResult(contractResult);
                    switch (wrapperTx.getType()) {
                        case TX_TYPE_CREATE_CONTRACT:
                            contractTxProcessorManager.createCommit(chainId, wrapperTx);
                            break;
                        case TX_TYPE_CALL_CONTRACT:
                            contractTxProcessorManager.callCommit(chainId, wrapperTx);
                            break;
                        case TX_TYPE_DELETE_CONTRACT:
                            contractTxProcessorManager.callCommit(chainId, wrapperTx);
                            break;
                        default:
                            break;
                    }
                }
            }

            return success();
        } catch (Exception e) {
            Log.error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = ROLLBACK, version = 1.0, description = "commit contract")
    @Parameter(parameterName = "chainId", parameterType = "int")
    @Parameter(parameterName = "txHexList", parameterType = "List<String>")
    @Parameter(parameterName = "blockHeaderHex", parameterType = "String")
    public Response rollback(Map<String,Object> params){
        try {
            Integer chainId = (Integer) params.get("chainId");
            List<String> txHexList = (List<String>) params.get("txHexList");
            String blockHeaderHex = (String) params.get("blockHeaderHex");
            Transaction tx;
            for(String txHex : txHexList) {
                tx = new Transaction();
                tx.parse(Hex.decode(txHex), 0);
                switch (tx.getType()) {
                    case TX_TYPE_CREATE_CONTRACT:
                        CreateContractData create = new CreateContractData();
                        create.parse(tx.getTxData(), 0);
                        contractTxProcessorManager.createRollback(chainId, new ContractWrapperTransaction(tx, null, create));
                        break;
                    case TX_TYPE_CALL_CONTRACT:
                        CallContractData call = new CallContractData();
                        call.parse(tx.getTxData(), 0);
                        contractTxProcessorManager.callRollback(chainId, new ContractWrapperTransaction(tx, null, call));
                        break;
                    case TX_TYPE_DELETE_CONTRACT:
                        DeleteContractData delete = new DeleteContractData();
                        delete.parse(tx.getTxData(), 0);
                        contractTxProcessorManager.deleteRollback(chainId, new ContractWrapperTransaction(tx, null, delete));
                        break;
                    default:
                        break;
                }
            }

            return success();
        } catch (Exception e) {
            Log.error(e);
            return failed(e.getMessage());
        }
    }

}
