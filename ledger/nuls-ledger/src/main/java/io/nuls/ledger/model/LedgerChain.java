package io.nuls.ledger.model;

import io.nuls.ledger.storage.impl.RepositoryImpl;
import io.nuls.ledger.storage.impl.UnconfirmedRepositoryImpl;
import io.nuls.ledger.utils.LoggerUtil;
import io.nuls.tools.core.ioc.SpringLiteContext;

/**
 * 链信息类
 * Chain information class
 *
 * @author lanjinsheng
 * @date 2019/02/11
 **/
public class LedgerChain {

    int chainId;

    public LedgerChain(int chainId) {
        this.chainId = chainId;
        //建立日志
        LoggerUtil.createLogger(chainId);
        //建立数据库
        SpringLiteContext.getBean(RepositoryImpl.class).initChainDb(chainId);
        SpringLiteContext.getBean(UnconfirmedRepositoryImpl.class).initChainDb(chainId);
    }
}
