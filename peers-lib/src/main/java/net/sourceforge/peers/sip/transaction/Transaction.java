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
    
    Copyright 2007-2013 Yohann Martineau 
*/

package net.sourceforge.peers.sip.transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;

import net.sourceforge.peers.Logger;
import net.sourceforge.peers.sip.transport.SipRequest;
import net.sourceforge.peers.sip.transport.SipResponse;
import net.sourceforge.peers.sip.transport.TransportManager;

/**
 * @author FLJ
 * @date 2022/7/13
 * @time 15:06
 * @Description 事务抽象 ...
 */
public abstract class Transaction {

    public static final char ID_SEPARATOR = '|';
    /**
     * 有 branchId
     */
    protected String branchId;
    /**
     * 事务属于什么请求方法
     */
    protected String method;

    /**
     * 对应的sip 请求
     */
    protected SipRequest request;
    /**
     * 一堆响应 ...
     */
    protected List<SipResponse> responses;

    /**
     * 一个timer ...
     */
    protected Timer timer;
    /**
     * 传输管理器
     */
    protected TransportManager transportManager;
    /**
     * 事务管理器  ...
     */
    protected TransactionManager transactionManager;

    protected Logger logger;

    protected Transaction(String branchId, String method, Timer timer,
            TransportManager transportManager,
            TransactionManager transactionManager, Logger logger) {
        this.branchId = branchId;
        this.method = method;
        this.timer = timer;
        this.transportManager = transportManager;
        this.transactionManager = transactionManager;
        this.logger = logger;
        responses = Collections.synchronizedList(new ArrayList<SipResponse>());
    }

    /**
     * 这也就是事务的Id ....
     * @see TransactionManager#getTransactionId(String, String)
     */
    protected String getId() {
        StringBuffer buf = new StringBuffer();
        buf.append(branchId).append(ID_SEPARATOR);
        buf.append(method);
        return buf.toString();
    }

    /**
     * sip 的最新响应 ...
     * @return
     */
    public SipResponse getLastResponse() {
        if (responses.isEmpty()) {
            return null;
        }
        return responses.get(responses.size() - 1);
    }

    public SipRequest getRequest() {
        return request;
    }
    
}
