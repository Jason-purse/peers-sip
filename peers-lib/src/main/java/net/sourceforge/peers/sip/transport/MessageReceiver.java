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

package net.sourceforge.peers.sip.transport;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;

import net.sourceforge.peers.Config;
import net.sourceforge.peers.Logger;
import net.sourceforge.peers.sip.RFC3261;
import net.sourceforge.peers.sip.Utils;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaderFieldValue;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaderParamName;
import net.sourceforge.peers.sip.syntaxencoding.SipParserException;
import net.sourceforge.peers.sip.transaction.ClientTransaction;
import net.sourceforge.peers.sip.transaction.ServerTransaction;
import net.sourceforge.peers.sip.transaction.TransactionManager;

public abstract class MessageReceiver implements Runnable {

    // 最大传输单元 应该是 1500
    public static final int BUFFER_SIZE = 2048;//FIXME should correspond to MTU 1024;
    // 使用ascill 码编码
    public static final String CHARACTER_ENCODING = "US-ASCII";
    
    protected int port;
    private boolean isListening;
    
    //private UAS uas;
    // 这个属于那个事务用户 ...
    private SipServerTransportUser sipServerTransportUser;
    // 事务管理器
    private TransactionManager transactionManager;
    // 传输管理器 ...
    private TransportManager transportManager;
    // 配置 ...
    private Config config;
    // 日志器
    protected Logger logger;

    public MessageReceiver(int port, TransactionManager transactionManager,
            TransportManager transportManager, Config config, Logger logger) {
        super();
        this.port = port;
        this.transactionManager = transactionManager;
        this.transportManager = transportManager;
        this.config = config;
        this.logger = logger;
        isListening = true;
    }
    
    public void run() {
        while (isListening) {
            try {
                // 进行listen 处理
                listen();
            } catch (IOException e) {
                logger.error("input/output error", e);
            }
        }
    }

    // 核心方法,监听 ..... 消息接收 ...
    protected abstract void listen() throws IOException;
    
    protected boolean isRequest(byte[] message) {
        String beginning = null;
        try {
            beginning = new String(message, 0,
                    RFC3261.DEFAULT_SIP_VERSION.length(), CHARACTER_ENCODING);
        } catch (UnsupportedEncodingException e) {
            logger.error("unsupported encoding", e);
        }
        if (RFC3261.DEFAULT_SIP_VERSION.equals(beginning)) {
            return false;
        }
        return true;
    }
    
    protected void processMessage(byte[] message, InetAddress sourceIp,
            int sourcePort, String transport) throws IOException {
        ByteArrayInputStream byteArrayInputStream =
            new ByteArrayInputStream(message);
        InputStreamReader inputStreamReader = new InputStreamReader(
                byteArrayInputStream);
        BufferedReader reader = new BufferedReader(inputStreamReader);
        String startLine = reader.readLine();
        while ("".equals(startLine)) {
            startLine = reader.readLine();
        }
        // 如果没有数据了,则返回 ...
        if (startLine == null) {
            return;
        }
        // 如果开始行,没有sip 版本 ... 发送存活 ... keep-alive ..
        if (!startLine.contains(RFC3261.DEFAULT_SIP_VERSION)) {
            // keep-alive, send back to sender
            SipTransportConnection sipTransportConnection =
                new SipTransportConnection(config.getLocalInetAddress(),
                        port, sourceIp, sourcePort, transport);
            MessageSender messageSender = transportManager.getMessageSender(
                    sipTransportConnection);
            if (messageSender != null) {
                // 发送这个消息
                // 也就是 ping / pong ...
                // 将消息发送回去 ...
                messageSender.sendBytes(message);
            }
            return;
        }
        StringBuffer direction = new StringBuffer();
        direction.append("-----------------------------------------------------------\n\r");
        direction.append("RECEIVED from ").append(sourceIp.getHostAddress());
        direction.append("/").append(sourcePort);
        logger.traceNetwork(new String(message),
                direction.toString());
        SipMessage sipMessage = null;
        try {
            // 进行消息解析
            // 解析headers / body
            sipMessage = transportManager.sipParser.parse(
                    new ByteArrayInputStream(message));
        } catch (IOException e) {
            logger.error("input/output error", e);
        } catch (SipParserException e) {
            logger.error("SIP parser error", e);
        }


        if (sipMessage == null) {
            logger.info("-----------------------------------------------------------------\n\r");
            return;
        }

        // RFC3261 18.2
//         判断消息
        if (sipMessage instanceof SipRequest) {
            SipRequest sipRequest = (SipRequest)sipMessage;
            
            
            SipHeaderFieldValue topVia = Utils.getTopVia(sipRequest);
            String sentBy =
                topVia.getParam(new SipHeaderParamName(RFC3261.PARAM_SENTBY));
            if (sentBy != null) {
                int colonPos = sentBy.indexOf(RFC3261.TRANSPORT_PORT_SEP);
                if (colonPos < 0) {
                    colonPos = sentBy.length();
                }
                sentBy = sentBy.substring(0, colonPos);
                if (!InetAddress.getByName(sentBy).equals(sourceIp)) {
                    topVia.addParam(new SipHeaderParamName(
                            RFC3261.PARAM_RECEIVED),
                            sourceIp.getHostAddress());
                }
            }
            //RFC3581
            //TODO check rport configuration
            SipHeaderParamName rportName = new SipHeaderParamName(
                    RFC3261.PARAM_RPORT);
            String rport = topVia.getParam(rportName);
            if (rport != null && "".equals(rport)) {
                topVia.removeParam(rportName);
                topVia.addParam(rportName, String.valueOf(sourcePort));
            }

            // 这个时候,先拿取服务端事务
            // 也就是说,别人请求我们,那么我们使用服务端事务 .... 没有则使用服务端传输用户处理 ...
            ServerTransaction serverTransaction =
                transactionManager.getServerTransaction(sipRequest);
            if (serverTransaction == null) {
                //uas.messageReceived(sipMessage);
                // 如果没有则  sipServer传输用户进行消息接收回调...
                // 没有事务可能是没有进行通信,可能是其他动作 ....
                sipServerTransportUser.messageReceived(sipMessage);
            } else {
                serverTransaction.receivedRequest(sipRequest);
            }
        } else {
            // 如果是响应 ....
            // 那么进行客户端事务处理 ...
            SipResponse sipResponse = (SipResponse)sipMessage;
            ClientTransaction clientTransaction =
                transactionManager.getClientTransaction(sipResponse);
            logger.debug("ClientTransaction = " + clientTransaction);
            if (clientTransaction == null) {
                //uas.messageReceived(sipMessage);
                // 如果客户端事务等于空,那么 sip serverTransportUser 处理 ....
                sipServerTransportUser.messageReceived(sipMessage);

                logger.info("客户端事务为空,UAS 帮忙处理 ...");
            } else {
                // 否则接收响应 ...
                clientTransaction.receivedResponse(sipResponse);
                logger.info("客户端事务不为空,UAC 处理");
            }
        }
    }
    
    public synchronized void setListening(boolean isListening) {
        this.isListening = isListening;
    }

    public synchronized boolean isListening() {
        return isListening;
    }

    public void setSipServerTransportUser(
            SipServerTransportUser sipServerTransportUser) {
        this.sipServerTransportUser = sipServerTransportUser;
    }

//    public void setUas(UAS uas) {
//        this.uas = uas;
//    }
    
}
