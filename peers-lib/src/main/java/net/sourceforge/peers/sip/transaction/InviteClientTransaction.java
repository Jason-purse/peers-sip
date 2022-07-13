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

import java.io.IOException;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;

import net.sourceforge.peers.Logger;
import net.sourceforge.peers.sip.RFC3261;
import net.sourceforge.peers.sip.Utils;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaderFieldName;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaderFieldValue;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaderParamName;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaders;
import net.sourceforge.peers.sip.transport.MessageSender;
import net.sourceforge.peers.sip.transport.SipClientTransportUser;
import net.sourceforge.peers.sip.transport.SipRequest;
import net.sourceforge.peers.sip.transport.SipResponse;
import net.sourceforge.peers.sip.transport.TransportManager;

/**
 * @author FLJ
 * @date 2022/7/13
 * @time 15:04
 * @Description 一个Invite client transaction ...
 */
public class InviteClientTransaction extends InviteTransaction
        implements ClientTransaction, SipClientTransportUser {


    /**
     * 包含集中状态
     */

    public final InviteClientTransactionState INIT;
    public final InviteClientTransactionState CALLING;
    public final InviteClientTransactionState PROCEEDING;
    public final InviteClientTransactionState COMPLETED;
    public final InviteClientTransactionState TERMINATED;

    protected ClientTransactionUser transactionUser;
    /**
     * 传输方式 ...
     */
    protected String transport;

    /**
     * 当前的 客户端事务状态
     */
    private InviteClientTransactionState state;
    //private SipClientTransport sipClientTransport;

    private MessageSender messageSender;
    /**
     * 重试次数
     */
    private int nbRetrans;
    private SipRequest ack;

    // 请求远程端口 ...
    private int remotePort;
    // 远程地址
    private InetAddress remoteInetAddress;
    
    InviteClientTransaction(String branchId, InetAddress inetAddress,
            int port, String transport, SipRequest sipRequest,
            ClientTransactionUser transactionUser, Timer timer,
            TransportManager transportManager,
            TransactionManager transactionManager, Logger logger) {
        super(branchId, timer, transportManager, transactionManager,
                logger);
        
        this.transport = transport;
        
        SipHeaderFieldValue via = new SipHeaderFieldValue("");

        // 这里开始封装 via 字段信息了 ...
        // branchId ... 设置 ...
        via.addParam(new SipHeaderParamName(RFC3261.PARAM_BRANCH), branchId);
        // 也就是说 via的形式例如:
        // via=key1=value1,key2=value2;
        sipRequest.getSipHeaders().add(new SipHeaderFieldName(RFC3261.HDR_VIA), via, 0);
        
        nbRetrans = 0;

        // 初始化状态
        INIT = new InviteClientTransactionStateInit(getId(), this, logger);
        // 刚开始状态等于初始化
        state = INIT;
        // 调用中
        CALLING = new InviteClientTransactionStateCalling(getId(), this,
                logger);
        // 处理中
        /**
         * 也可以说是通话中 ...
         */
        PROCEEDING = new InviteClientTransactionStateProceeding(getId(), this,
                logger);
        // 完成
        COMPLETED = new InviteClientTransactionStateCompleted(getId(), this,
                logger);
        // 中断
        TERMINATED = new InviteClientTransactionStateTerminated(getId(), this,
                logger);

        //17.1.1.2
        
        request = sipRequest;
        // 这个事务属于那个用户
        this.transactionUser = transactionUser;

        remotePort = port;
        remoteInetAddress = inetAddress;
        
        try {
            // 传输管理器则创建一个客户端传输(底层也就是通信channel)
            messageSender = transportManager.createClientTransport(
                    request, remoteInetAddress, remotePort, transport);
        } catch (IOException e) {
            logger.error("input/output error", e);
            transportError();
        }

    }
    
    public void setState(InviteClientTransactionState state) {
        this.state.log(state);
        this.state = state;
        if(TERMINATED.equals(state)) {
            //transactionManager.removeClientTransaction(branchId, method);
            transactionManager = null;
        }
    }
    
    public void start() {
        // 状态开始
        state.start();
        //send request using transport information and sipRequest
//        try {
//            sipClientTransport = SipTransportFactory.getInstance()
//                .createClientTransport(this, request, remoteInetAddress,
//                        remotePort, transport);
//            sipClientTransport.send(request);
//        } catch (IOException e) {
//            //e.printStackTrace();
//            transportError();
//        }
        
        try {
            // 发送请求 ...
            messageSender.sendMessage(request);
        } catch (IOException e) {
            logger.error("input/output error", e);
            transportError();
        }
        logger.debug("InviteClientTransaction.start");

        // 如果为 UDP ...
        if (RFC3261.TRANSPORT_UDP.equals(transport)) {
            // 重发
            //start timer A with value T1 for retransmission
            timer.schedule(new TimerA(), RFC3261.TIMER_T1);
        }
        //  事务超时 ....调度 ...
        //TODO start timer B with value 64*T1 for transaction timeout
        timer.schedule(new TimerB(), 64 * RFC3261.TIMER_T1);
    }
    
    public synchronized void receivedResponse(SipResponse sipResponse) {

        // 响应无非几种情况 ...
        // 成功 / 失败 ...
        responses.add(sipResponse);
        // 17.1.1
        int statusCode = sipResponse.getStatusCode();
        if (statusCode < RFC3261.CODE_MIN_PROV) {
            logger.error("invalid response code");
        } else if (statusCode < RFC3261.CODE_MIN_SUCCESS) {
            state.received1xx();
        } else if (statusCode < RFC3261.CODE_MIN_REDIR) {
            state.received2xx();
        } else if (statusCode <= RFC3261.CODE_MAX) {
            state.received300To699();
        } else {
            logger.error("invalid response code");
        }
    }
    
    public void transportError() {
        state.transportError();
    }
    
    void createAndSendAck() {
        
        //p.126 last paragraph
        
        //17.1.1.3
        ack = new SipRequest(RFC3261.METHOD_ACK, request.getRequestUri());
        SipHeaderFieldValue topVia = Utils.getTopVia(request);
        SipHeaders ackSipHeaders = ack.getSipHeaders();
        ackSipHeaders.add(new SipHeaderFieldName(RFC3261.HDR_VIA), topVia);
        Utils.copyHeader(request, ack, RFC3261.HDR_CALLID);
        Utils.copyHeader(request, ack, RFC3261.HDR_FROM);
        Utils.copyHeader(getLastResponse(), ack, RFC3261.HDR_TO);
        //TODO what happens if a prov response is received after a 200+ ...
        SipHeaders requestSipHeaders = request.getSipHeaders();
        SipHeaderFieldName cseqName = new SipHeaderFieldName(RFC3261.HDR_CSEQ);
        SipHeaderFieldValue cseq = requestSipHeaders.get(cseqName);
        cseq.setValue(cseq.toString().replace(RFC3261.METHOD_INVITE, RFC3261.METHOD_ACK));
        ackSipHeaders.add(cseqName, cseq);
        Utils.copyHeader(request, ack, RFC3261.HDR_ROUTE);
        
        sendAck();
    }
    
    void sendAck() {
        //ack is passed to the transport layer...
        //TODO manage ACK retrans
        //sipClientTransport.send(ack);
        try {
            messageSender.sendMessage(ack);
        } catch (IOException e) {
            logger.error("input/output error", e);
            transportError();
        }
    }
    
    void sendRetrans() {
        // 重试次数增加
        ++nbRetrans;
        //sipClientTransport.send(request);
        try {
            // 消息发送器发送消息 ...
            messageSender.sendMessage(request);
        } catch (IOException e) {
            logger.error("input/output error", e);
            transportError();
        }
        // 再一次尝试调度 ....
        // 根据算法 2的倍数形式重试 ...
        timer.schedule(new TimerA(), (long)Math.pow(2, nbRetrans) * RFC3261.TIMER_T1);
    }
    
    public void requestTransportError(SipRequest sipRequest, Exception e) {
        // TODO Auto-generated method stub
        
    }

    public void responseTransportError(Exception e) {
        // TODO Auto-generated method stub
        
    }

    /**
     * timer A 重试
     */
    class TimerA extends TimerTask {
        @Override
        public void run() {
            state.timerAFires();
        }
    }
    
    class TimerB extends TimerTask {
        @Override
        public void run() {
            state.timerBFires();
        }
    }
    
    class TimerD extends TimerTask {
        @Override
        public void run() {
            state.timerDFires();
        }
    }

    public String getContact() {
        // 从消息发送者中获取
        if (messageSender != null) {
            return messageSender.getContact();
        }
        return null;
    }

}
