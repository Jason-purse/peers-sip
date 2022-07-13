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

import net.sourceforge.peers.sip.transport.SipRequest;
import net.sourceforge.peers.sip.transport.SipResponse;
/**
 * @author FLJ
 * @date 2022/7/13
 * @time 13:56
 * @Description Sip 监听器,监听各种事件
 */
public interface SipListener {

    /**
     * 注册中
     * @param sipRequest
     */
    public void registering(SipRequest sipRequest);

    public void registerSuccessful(SipResponse sipResponse);

    public void registerFailed(SipResponse sipResponse);

    /**
     * 电话进入 ...
     * @param sipRequest
     * @param provResponse
     */
    public void incomingCall(SipRequest sipRequest, SipResponse provResponse);

    /**
     * 远程挂断 ...
     * @param sipRequest
     */
    public void remoteHangup(SipRequest sipRequest);

    /**
     * 振铃中 ...
     * @param sipResponse
     */
    public void ringing(SipResponse sipResponse);

    /**
     * 被调方 接听 ...
     * @param sipResponse
     */
    public void calleePickup(SipResponse sipResponse);

    /**
     * 错误
     * @param sipResponse
     */
    public void error(SipResponse sipResponse);

    /**
     * 事务失败 ... 请求错误 ....
     */
    default void inviteError() {
        // TODO
    }

}
