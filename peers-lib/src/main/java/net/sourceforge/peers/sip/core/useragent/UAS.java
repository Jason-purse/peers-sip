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

package net.sourceforge.peers.sip.core.useragent;

import java.net.SocketException;
import java.util.ArrayList;

import net.sourceforge.peers.sip.RFC3261;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaderFieldName;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaderFieldValue;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaderParamName;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaders;
import net.sourceforge.peers.sip.transaction.TransactionManager;
import net.sourceforge.peers.sip.transactionuser.Dialog;
import net.sourceforge.peers.sip.transactionuser.DialogManager;
import net.sourceforge.peers.sip.transport.SipMessage;
import net.sourceforge.peers.sip.transport.SipRequest;
import net.sourceforge.peers.sip.transport.SipResponse;
import net.sourceforge.peers.sip.transport.SipServerTransportUser;
import net.sourceforge.peers.sip.transport.TransportManager;
/**
 * @author FLJ
 * @date 2022/7/13
 * @time 16:45
 * @Description UAS 用于代理UAC ...
 */
public class UAS implements SipServerTransportUser {

    public final static ArrayList<String> SUPPORTED_METHODS;
    
    static {
        /**
         * 支持的几种方法 ...
         */
        SUPPORTED_METHODS = new ArrayList<String>();
        SUPPORTED_METHODS.add(RFC3261.METHOD_INVITE);
        SUPPORTED_METHODS.add(RFC3261.METHOD_ACK);
        SUPPORTED_METHODS.add(RFC3261.METHOD_CANCEL);
        SUPPORTED_METHODS.add(RFC3261.METHOD_OPTIONS);
        SUPPORTED_METHODS.add(RFC3261.METHOD_BYE);
    };
    
    private InitialRequestManager initialRequestManager;
    private MidDialogRequestManager midDialogRequestManager;
    
    private DialogManager dialogManager;
    
    /**
     * should be instanciated only once, it was a singleton.
     */
    public UAS(UserAgent userAgent,
            InitialRequestManager initialRequestManager,
            MidDialogRequestManager midDialogRequestManager,
            DialogManager dialogManager,
            TransactionManager transactionManager,
            TransportManager transportManager) throws SocketException {
        this.initialRequestManager = initialRequestManager;
        this.midDialogRequestManager = midDialogRequestManager;
        this.dialogManager = dialogManager;
        transportManager.setSipServerTransportUser(this);
        // transport Manager 创建服务端Transport ....

        // 它同样用了SipPort ... 看一下做了什么 ...
        transportManager.createServerTransport(
                RFC3261.TRANSPORT_UDP, userAgent.getConfig().getSipPort());
    }
    
    public void messageReceived(SipMessage sipMessage) {
        if (sipMessage instanceof SipRequest) {
            requestReceived((SipRequest) sipMessage);
        } else if (sipMessage instanceof SipResponse) {
            responseReceived((SipResponse) sipMessage);
        } else {
            throw new RuntimeException("unknown message type");
        }
    }

    private void responseReceived(SipResponse sipResponse) {
        // 这也是我们猜测的 ...
        // 它根本不关心响应 ...
    }

    // 接收到请求,我们才需要转发给USC 判断是否需要 处理 ....
    // 也就是说,我们如果没有注册,那么服务器会发送验证Dialog 到UAS,然后UAS 帮我们处理了 ...
    // 仅仅是猜测 ...
    private void requestReceived(SipRequest sipRequest) {
        //TODO 8.2
        
        //TODO JTA to make request processing atomic
        
        SipHeaders headers = sipRequest.getSipHeaders();
        
        //TODO find whether the request is within an existing dialog or not
        // 需要判断请求是否在存在的dialog中或者不是 ...
        // 判断To,是谁 ...
        SipHeaderFieldValue to =
            headers.get(new SipHeaderFieldName(RFC3261.HDR_TO));
        // 拿取tag
        String toTag = to.getParam(new SipHeaderParamName(RFC3261.PARAM_TAG));
        if (toTag != null) {
            // 获取这个请求中的Dialog ....
            Dialog dialog = dialogManager.getDialog(sipRequest);
            if (dialog != null) {
                //this is a mid-dialog request
                midDialogRequestManager.manageMidDialogRequest(sipRequest, dialog);
                //TODO continue processing
            } else {
                //TODO reject the request with a 481 Call/Transaction Does Not Exist
                
            }
        } else {
            // 否则 交给初始化请求管理器,管理初始化请求 ....
            initialRequestManager.manageInitialRequest(sipRequest);
            
        }
    }

    void acceptCall(SipRequest sipRequest, Dialog dialog) {
        initialRequestManager.getInviteHandler().acceptCall(sipRequest,
                dialog);
    }

    void rejectCall(SipRequest sipRequest) {
        initialRequestManager.getInviteHandler().rejectCall(sipRequest);
    }

    public InitialRequestManager getInitialRequestManager() {
        return initialRequestManager;
    }

    public MidDialogRequestManager getMidDialogRequestManager() {
        return midDialogRequestManager;
    }
    
}
