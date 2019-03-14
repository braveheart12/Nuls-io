/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2019 nuls.io
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
package io.nuls.contract.model.dto;


import io.nuls.contract.model.bo.ContractTokenInfo;
import io.nuls.contract.util.ContractUtil;
import lombok.Getter;
import lombok.Setter;

/**
 * @author: PierreLuo
 * @date: 2018/8/19
 */
@Getter
@Setter
public class ContractTokenInfoDto {

    private String contractAddress;
    private String name;
    private String symbol;
    private String amount;
    private long decimals;
    private long blockHeight;
    private int status;

    public ContractTokenInfoDto() {
    }

    public ContractTokenInfoDto(ContractTokenInfo info) {
        this.contractAddress = info.getContractAddress();
        this.name = info.getName();
        this.symbol = info.getSymbol();
        this.amount = ContractUtil.bigInteger2String(info.getAmount());
        this.decimals = info.getDecimals();
        this.blockHeight = info.getBlockHeight();
        this.status = info.getStatus();
    }

}
