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
    
    Copyright 2007, 2008, 2009, 2010, 2011 Yohann Martineau 
*/

package net.sourceforge.peers.sip.core.useragent;

import net.sourceforge.peers.Logger;
import net.sourceforge.peers.sip.RFC3261;
import net.sourceforge.peers.sip.Utils;
import net.sourceforge.peers.sip.core.useragent.handlers.ByeHandler;
import net.sourceforge.peers.sip.core.useragent.handlers.CancelHandler;
import net.sourceforge.peers.sip.core.useragent.handlers.InviteHandler;
import net.sourceforge.peers.sip.core.useragent.handlers.OptionsHandler;
import net.sourceforge.peers.sip.core.useragent.handlers.RegisterHandler;
import net.sourceforge.peers.sip.syntaxencoding.NameAddress;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaderFieldName;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaderFieldValue;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaderParamName;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaders;
import net.sourceforge.peers.sip.syntaxencoding.SipURI;
import net.sourceforge.peers.sip.syntaxencoding.SipUriSyntaxException;
import net.sourceforge.peers.sip.transaction.ClientTransaction;
import net.sourceforge.peers.sip.transaction.ServerTransaction;
import net.sourceforge.peers.sip.transaction.ServerTransactionUser;
import net.sourceforge.peers.sip.transaction.TransactionManager;
import net.sourceforge.peers.sip.transactionuser.DialogManager;
import net.sourceforge.peers.sip.transport.SipRequest;
import net.sourceforge.peers.sip.transport.SipResponse;
import net.sourceforge.peers.sip.transport.TransportManager;
/**
 * @author FLJ
 * @date 2022/7/13
 * @time 14:36
 * @Description 初始化请求管理器
 */
public class InitialRequestManager extends RequestManager
        implements ServerTransactionUser {

    public InitialRequestManager(UserAgent userAgent,
            InviteHandler inviteHandler,
            CancelHandler cancelHandler,
            ByeHandler byeHandler,
            OptionsHandler optionsHandler,
            RegisterHandler registerHandler,
            DialogManager dialogManager,
            TransactionManager transactionManager,
            TransportManager transportManager,
            Logger logger) {
        super(userAgent,
                inviteHandler,
                cancelHandler,
                byeHandler,
                optionsHandler,
                registerHandler,
                dialogManager,
                transactionManager,
                transportManager,
                logger);
        registerHandler.setInitialRequestManager(this);
    }

    /**
     * gives a new request outside of a dialog
     *
     * 一个请求的基本形式
     * 
     * @param requestUri
     * @param method
     * @return
     * @throws SipUriSyntaxException 
     */
    public SipRequest getGenericRequest(String requestUri, String method,
            String profileUri, String callId, String fromTag)
            throws SipUriSyntaxException {
        //8.1.1
        SipRequest request = new SipRequest(method, new SipURI(requestUri));
        SipHeaders headers = request.getSipHeaders();
        //String hostAddress = utils.getMyAddress().getHostAddress();
        
        //Via
        
        //TODO no Via should be added directly by UAC, Via is normally added by Transaction layer
        
//        StringBuffer viaBuf = new StringBuffer();
//        viaBuf.append(RFC3261.DEFAULT_SIP_VERSION);
//        // TODO choose real transport
//        viaBuf.append("/UDP ");
//        viaBuf.append(hostAddress);
//        SipHeaderFieldValue via = new SipHeaderFieldValue(viaBuf.toString());
//        via.addParam(new SipHeaderParamName(RFC3261.PARAM_BRANCHID),
//                utils.generateBranchId());
//        headers.add(new SipHeaderFieldName(RFC3261.HDR_VIA), via);
        
        Utils.addCommonHeaders(headers);
        
        //To
        
        NameAddress to = new NameAddress(requestUri);
        headers.add(new SipHeaderFieldName(RFC3261.HDR_TO),
                new SipHeaderFieldValue(to.toString()));
        
        //From
        
        NameAddress fromNA = new NameAddress(profileUri);
        SipHeaderFieldValue from = new SipHeaderFieldValue(fromNA.toString());
        String localFromTag;
        if (fromTag != null) {
            localFromTag = fromTag;
        } else {
            localFromTag = Utils.generateTag();
        }
        from.addParam(new SipHeaderParamName(RFC3261.PARAM_TAG), localFromTag);
        headers.add(new SipHeaderFieldName(RFC3261.HDR_FROM), from);
        
        //Call-ID
        
        SipHeaderFieldName callIdName =
            new SipHeaderFieldName(RFC3261.HDR_CALLID);
        String localCallId;
        if (callId != null) {
            localCallId = callId;
        } else {
            localCallId = Utils.generateCallID(
                    userAgent.getConfig().getLocalInetAddress());
        }
        headers.add(callIdName, new SipHeaderFieldValue(localCallId));
        
        //CSeq
        // 请求次数 ... 依次递增(例如注册,被要求凭证,第二次cSeq = 2)
        headers.add(new SipHeaderFieldName(RFC3261.HDR_CSEQ),
                new SipHeaderFieldValue(userAgent.generateCSeq(method)));
        
        return request;
    }
 
    public SipRequest createInitialRequest(String requestUri, String method,
            String profileUri) throws SipUriSyntaxException {
        return createInitialRequest(requestUri, method, profileUri, null);
    }

    /**
     * 创建初始化请求 ...
     * @param requestUri
     * @param method
     * @param profileUri
     * @param callId
     * @return
     * @throws SipUriSyntaxException
     */
    public SipRequest createInitialRequest(String requestUri, String method,
            String profileUri, String callId) throws SipUriSyntaxException {
        
        return createInitialRequest(requestUri, method, profileUri, callId,
                null, null);
    }

    /**
     *
     * @param requestUri
     * @param method
     * @param profileUri
     * @param callId
     * @param fromTag ??
     * @param messageInterceptor 进行消息拦截 ..
     * @return
     * @throws SipUriSyntaxException
     */
    public SipRequest createInitialRequest(String requestUri, String method,
            String profileUri, String callId, String fromTag,
            MessageInterceptor messageInterceptor)
                throws SipUriSyntaxException {

        // 生成一个通用的Sip请求 ...
        SipRequest sipRequest = getGenericRequest(requestUri, method,
                profileUri, callId, fromTag);
        
        // TODO add route header for outbound proxy give it to xxxHandler to create
        // clientTransaction
        SipURI outboundProxy = userAgent.getOutboundProxy();
        if (outboundProxy != null) {
            NameAddress outboundProxyNameAddress =
                new NameAddress(outboundProxy.toString());
            // 增加了路由 ... 但是不知道 外出代理是干嘛的
            // 为注册和呼叫提供出站代理 ...
            // 也就是说代理到这个地址上 ???
            sipRequest.getSipHeaders().add(new SipHeaderFieldName(RFC3261.HDR_ROUTE),
                    new SipHeaderFieldValue(outboundProxyNameAddress.toString()), 0);
        }
        ClientTransaction clientTransaction = null;
        // 也就是初次请求等于invite ...
        // 如果等于invite
        if (RFC3261.METHOD_INVITE.equals(method)) {
            // 执行PreProcessInvite
            // 于是这个 客户端事务就被创建出来了 ...
            clientTransaction = inviteHandler.preProcessInvite(sipRequest);
        } else if (RFC3261.METHOD_REGISTER.equals(method)) {
            // 否则 注册
            clientTransaction = registerHandler.preProcessRegister(sipRequest);
        }

        // 创建初始化请求末尾 ...
        createInitialRequestEnd(sipRequest, clientTransaction, profileUri,
                messageInterceptor, true);
        return sipRequest;
    }
    // 这里加入了消息拦截器 / addContact 用来干什么 ...
    private void createInitialRequestEnd(SipRequest sipRequest,
            ClientTransaction clientTransaction, String profileUri,
            MessageInterceptor messageInterceptor, boolean addContact) {
    	if (clientTransaction == null) {
    		logger.error("method not supported");
    		return;
    	}
    	if (addContact) {
            // 增加协商 ...联系
    	    addContact(sipRequest, clientTransaction.getContact(), profileUri);
    	}
        if (messageInterceptor != null) {
            // 消息拦截器后置处理 ...
            messageInterceptor.postProcess(sipRequest);
        }
        // 然后开启事务 ...
        // 应该在客户端传输端口上创建消息接收器 ...
        // 有点懵,本身不就是已经根据需要监听的sip 端口进行消息接收器创建了嘛 ...
        // TODO create message receiver on client transport port
        clientTransaction.start();
    }
    
    public void createCancel(SipRequest inviteRequest,
            MidDialogRequestManager midDialogRequestManager, String profileUri) {
        SipHeaders inviteHeaders = inviteRequest.getSipHeaders();
        SipHeaderFieldValue callId = inviteHeaders.get(
                new SipHeaderFieldName(RFC3261.HDR_CALLID));
        SipRequest sipRequest;
        try {
            sipRequest = getGenericRequest(
                    inviteRequest.getRequestUri().toString(), RFC3261.METHOD_CANCEL,
                    profileUri, callId.getValue(), null);
        } catch (SipUriSyntaxException e) {
            logger.error("syntax error", e);
            return;
        }
        
        ClientTransaction clientTransaction = null;
            clientTransaction = cancelHandler.preProcessCancel(sipRequest,
                    inviteRequest, midDialogRequestManager);
        if (clientTransaction != null) {
            createInitialRequestEnd(sipRequest, clientTransaction, profileUri,
                    null, false);
        }
        
        
    }

    public void manageInitialRequest(SipRequest sipRequest) {
        SipHeaders headers = sipRequest.getSipHeaders();
        
        // TODO authentication
        
        //method inspection
        SipResponse sipResponse = null;
        if (!UAS.SUPPORTED_METHODS.contains(sipRequest.getMethod())) {
            //TODO generate 405 (using 8.2.6 &) with Allow header
            //(20.5) and send it
            sipResponse = generateResponse(sipRequest, null,
                    RFC3261.CODE_405_METHOD_NOT_ALLOWED,
                    RFC3261.REASON_405_METHOD_NOT_ALLOWED);
            SipHeaders sipHeaders = sipResponse.getSipHeaders();
            sipHeaders.add(new SipHeaderFieldName(RFC3261.HDR_ALLOW),
                    new SipHeaderFieldValue(Utils.generateAllowHeader()));
        }

        
        SipHeaderFieldValue contentType =
            headers.get(new SipHeaderFieldName(RFC3261.HDR_CONTENT_TYPE));
        if (contentType != null) {
            if (!RFC3261.CONTENT_TYPE_SDP.equals(contentType.getValue())) {
                //TODO generate 415 with a Accept header listing supported content types
                //8.2.3
            }
        }

        
        //etc.
        
        if (sipResponse != null) {
            ServerTransaction serverTransaction =
                transactionManager.createServerTransaction(
                    sipResponse, userAgent.getSipPort(), RFC3261.TRANSPORT_UDP,
                    this, sipRequest);
            serverTransaction.start();
            serverTransaction.receivedRequest(sipRequest);
            serverTransaction.sendReponse(sipResponse);
        }
        
        //TODO create server transaction
        String method = sipRequest.getMethod();
        if (RFC3261.METHOD_INVITE.equals(method)) {
            inviteHandler.handleInitialInvite(sipRequest);
        } else if (RFC3261.METHOD_CANCEL.equals(method)) {
            cancelHandler.handleCancel(sipRequest);
        } else if (RFC3261.METHOD_OPTIONS.equals(method)) {
            optionsHandler.handleOptions(sipRequest);
        }
    }

    public void addContact(SipRequest sipRequest, String contactEnd,
            String profileUri) {
        SipHeaders sipHeaders = sipRequest.getSipHeaders();
        
        
        
        //Contact
        // 也就是回来的路(回来的时候应该找谁) ....
        
        StringBuffer contactBuf = new StringBuffer();
        contactBuf.append(RFC3261.SIP_SCHEME);
        contactBuf.append(RFC3261.SCHEME_SEPARATOR);
        // 拿到用户的一部分 ...
        String userPart = Utils.getUserPart(profileUri);
        contactBuf.append(userPart);
        contactBuf.append(RFC3261.AT);
        contactBuf.append(contactEnd);

        NameAddress contactNA = new NameAddress(contactBuf.toString());
        SipHeaderFieldValue contact =
            new SipHeaderFieldValue(contactNA.toString());
        sipHeaders.add(new SipHeaderFieldName(RFC3261.HDR_CONTACT),
                new SipHeaderFieldValue(contact.toString()));
    }

    ///////////////////////////////////////////////////////////
    // ServerTransactionUser methods
    ///////////////////////////////////////////////////////////

    @Override
    public void transactionFailure() {
        // TODO Auto-generated method stub
        
    }

}
