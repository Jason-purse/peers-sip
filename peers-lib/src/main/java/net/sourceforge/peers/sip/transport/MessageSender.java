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

package net.sourceforge.peers.sip.transport;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;

import net.sourceforge.peers.Config;
import net.sourceforge.peers.Logger;
import net.sourceforge.peers.sip.RFC3261;

/**
 * @author FLJ
 * @date 2022/7/13
 * @time 15:09
 * @Description 消息发送器  用来发送消息 ...
 */
public abstract class MessageSender {

    public static final int KEEY_ALIVE_INTERVAL = 25; // seconds

    protected InetAddress inetAddress;
    protected int port;
    protected int localPort;
    private Config config;
    private String transportName;
    private Timer timer;
    protected Logger logger;
    
    public MessageSender(int localPort, InetAddress inetAddress,
            int port, Config config,
            String transportName, Logger logger) {
        super();
        this.localPort = localPort;
        this.inetAddress = inetAddress;
        this.port = port;
        this.config = config;
        this.transportName = transportName;
        timer = new Timer(getClass().getSimpleName() + " "
            + Timer.class.getSimpleName());
        this.logger = logger;
        //TODO check config
        timer.scheduleAtFixedRate(new KeepAlive(), 0,
                1000 * KEEY_ALIVE_INTERVAL);
    }

    // -------------------- core -------------------
    // 发送Sip 消息
    // 发送字节
    public abstract void sendMessage(SipMessage sipMessage) throws IOException;
    public abstract void sendBytes(byte[] bytes) throws IOException;


    // 于是消息发送者中 就尝试先拿取公有地址,否则本地 ....
    // 也就是回来的时候,消息格式(用户信息处理)
    public String getContact() {
        StringBuffer buf = new StringBuffer();
        InetAddress myAddress = config.getPublicInetAddress();
        if (myAddress == null) {
            myAddress = config.getLocalInetAddress();
        }
        // hostname 也可以(需要做额外的处理,对应作者的话) ...
        buf.append(myAddress.getHostAddress());
        buf.append(RFC3261.TRANSPORT_PORT_SEP);
        //buf.append(config.getSipPort());
        buf.append(localPort);
        buf.append(RFC3261.PARAM_SEPARATOR);
        buf.append(RFC3261.PARAM_TRANSPORT);
        buf.append(RFC3261.PARAM_ASSIGNMENT);
        buf.append(transportName);
        return buf.toString();
    }

    public int getLocalPort() {
        return localPort;
    }

    public void stopKeepAlives() {
        timer.cancel();
    }

    class KeepAlive extends TimerTask {

        @Override
        public void run() {
            byte[] bytes = (RFC3261.CRLF + RFC3261.CRLF).getBytes();
            try {
                sendBytes(bytes);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }

    }

}
