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

import net.sourceforge.peers.Logger;

/**
 * @author FLJ
 * @date 2022/7/13
 * @time 16:31
 * @Description 事务calling中 , 定时器A触发 ...
 */
public class InviteClientTransactionStateCalling extends InviteClientTransactionState {

    public InviteClientTransactionStateCalling(String id,
            InviteClientTransaction inviteClientTransaction, Logger logger) {
        super(id, inviteClientTransaction, logger);
    }

    @Override
    public void timerAFires() {
        // 下一个阶段还是 Calling ...
        InviteClientTransactionState nextState = inviteClientTransaction.CALLING;
        inviteClientTransaction.setState(nextState);
        // 发送重试
        inviteClientTransaction.sendRetrans();
    }
    
    @Override
    public void timerBFires() {
        // 当timerB 事务超时触发 ...
        timerBFiresOrTransportError();
    }
    
    @Override
    public void transportError() {
        timerBFiresOrTransportError();
    }


    // 事务失败 ....
    private void timerBFiresOrTransportError() {
        // 状态直接进入终止
        InviteClientTransactionState nextState = inviteClientTransaction.TERMINATED;
        inviteClientTransaction.setState(nextState);
        // 触发 这个事务的拥有者事务超时
        inviteClientTransaction.transactionUser.transactionTimeout(
                inviteClientTransaction);
    }
    
    @Override
    public void received2xx() {
        // 直接变成中断状态 ...
        InviteClientTransactionState nextState = inviteClientTransaction.TERMINATED;
        inviteClientTransaction.setState(nextState);
        // 然后触发 成功响应接收 ...
        inviteClientTransaction.transactionUser.successResponseReceived(
                inviteClientTransaction.getLastResponse(), inviteClientTransaction);
    }
    
    @Override
    public void received1xx() {
        // 进入处理中 ...
        InviteClientTransactionState nextState = inviteClientTransaction.PROCEEDING;
        inviteClientTransaction.setState(nextState);
        inviteClientTransaction.transactionUser.provResponseReceived(
                inviteClientTransaction.getLastResponse(), inviteClientTransaction);
    }
    
    @Override
    public void received300To699() {
        // 进入完成了 ....
        InviteClientTransactionState nextState = inviteClientTransaction.COMPLETED;
        inviteClientTransaction.setState(nextState);
        // 创建并发送ACK ...
        inviteClientTransaction.createAndSendAck();
        // 接收到错误消息 ...
        inviteClientTransaction.transactionUser.errResponseReceived(
                inviteClientTransaction.getLastResponse());
    }
    
    
}
