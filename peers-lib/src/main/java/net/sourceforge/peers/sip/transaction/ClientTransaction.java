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
 * @time 15:04
 * @Description 客户端事务,可以有的也就是接收响应 ... start / contact ???
 */
public interface ClientTransaction {

    /**
     * 接收到响应 ...
     * @param sipResponse
     */
    public void receivedResponse(SipResponse sipResponse);

    /**
     * 开启事务
     */
    public void start();

    // 客户端事务中的联系人 ...(也就是caller)
    public String getContact();
}
