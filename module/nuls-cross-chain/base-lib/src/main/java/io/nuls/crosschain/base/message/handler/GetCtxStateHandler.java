package io.nuls.crosschain.base.message.handler;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.rpc.protocol.MessageProcessor;
import io.nuls.core.rpc.util.RPCUtil;
import io.nuls.crosschain.base.constant.CommandConstant;
import io.nuls.crosschain.base.message.GetCtxStateMessage;
import io.nuls.crosschain.base.service.ProtocolService;

@Component("GetCtxStateHandlerV1")
public class GetCtxStateHandler implements MessageProcessor {
    @Autowired
    private ProtocolService protocolService;

    @Override
    public String getCmd() {
        return CommandConstant.GET_CTX_STATE_MESSAGE;
    }

    @Override
    public void process(int chainId, String nodeId, String message) {
        GetCtxStateMessage realMessage = RPCUtil.getInstanceRpcStr(message, GetCtxStateMessage.class);
        if (message == null) {
            return;
        }
        protocolService.getCtxState(chainId, nodeId, realMessage);
    }
}
