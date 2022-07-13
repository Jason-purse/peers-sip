/*
    This file is part of Peers, a java SIP softphone.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright 2007, 2008, 2009, 2010 Yohann Martineau 
*/

package net.sourceforge.peers.sip.transaction;

import net.sourceforge.peers.sip.transport.SipResponse;
/**
 * @author FLJ
 * @date 2022/7/13
 * @time 14:30
 * @Description 客户端事务用户(也就是最终客户端的事务属于那个用户,用户可以得到响应) ...
 */
public interface ClientTransactionUser {
    /**
     * 事务超时
     * @param clientTransaction
     */
    public void transactionTimeout(ClientTransaction clientTransaction);

    /**
     * 校验 接收的响应 ...
     * @param sipResponse
     * @param transaction
     */
    public void provResponseReceived(SipResponse sipResponse, Transaction transaction);

    /**
     * 接收到的错误响应
     * @param sipResponse
     */
    // TODO 最终需要传递事务到事务user
    //TODO eventually pass transaction to the transaction user
    public void errResponseReceived(SipResponse sipResponse);//3XX is considered as an error response

    // 成功响应接收 ...
    public void successResponseReceived(SipResponse sipResponse, Transaction transaction);
    // 事务传输失败 ..
    public void transactionTransportError();
}
