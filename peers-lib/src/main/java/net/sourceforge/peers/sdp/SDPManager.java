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
    
    Copyright 2007, 2008, 2009, 2010, 2012 Yohann Martineau 
*/

package net.sourceforge.peers.sdp;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import net.sourceforge.peers.Config;
import net.sourceforge.peers.Logger;
import net.sourceforge.peers.rtp.RFC3551;
import net.sourceforge.peers.rtp.RFC4733;
import net.sourceforge.peers.sip.core.useragent.UserAgent;
/**
 * @author FLJ
 * @date 2022/7/13
 * @time 14:03
 * @Description SDP 管理器
 *
 * SDP 协议处理 ..
 */
public class SDPManager {
    
    private SdpParser sdpParser;
    private UserAgent userAgent;
    private List<Codec> supportedCodecs;
    private Random random;

    private Logger logger;
    
    public SDPManager(UserAgent userAgent, Logger logger) {
        this.userAgent = userAgent;
        this.logger = logger;
        sdpParser = new SdpParser();
        supportedCodecs = new ArrayList<Codec>();
        random = new Random();
        //TODO retrieve codecs from configuration file
        Codec codec = new Codec();
        codec.setPayloadType(RFC3551.PAYLOAD_TYPE_PCMU);
        codec.setName(RFC3551.PCMU);
        supportedCodecs.add(codec);
        codec = new Codec();
        codec.setPayloadType(RFC3551.PAYLOAD_TYPE_PCMA);
        codec.setName(RFC3551.PCMA);
        supportedCodecs.add(codec);
        codec = new Codec();
        codec.setPayloadType(RFC4733.PAYLOAD_TYPE_TELEPHONE_EVENT);
        codec.setName(RFC4733.TELEPHONE_EVENT);

        //TODO add fmtp:101 0-15 attribute
        supportedCodecs.add(codec);
    }
    
    public SessionDescription parse(byte[] sdp) {
        try {
            return sdpParser.parse(sdp);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public MediaDestination getMediaDestination(
            SessionDescription sessionDescription) throws NoCodecException {
        InetAddress destAddress = sessionDescription.getIpAddress();
        List<MediaDescription> mediaDescriptions = sessionDescription.getMediaDescriptions();
        for (MediaDescription mediaDescription: mediaDescriptions) {
            if (RFC4566.MEDIA_AUDIO.equals(mediaDescription.getType())) {
                for (Codec offerCodec: mediaDescription.getCodecs()) {
                    if (supportedCodecs.contains(offerCodec)) {
                        String offerCodecName = offerCodec.getName();
                        if (offerCodecName.equalsIgnoreCase(RFC3551.PCMU) ||
                                offerCodecName.equalsIgnoreCase(RFC3551.PCMA)) {
                            int destPort = mediaDescription.getPort();
                            if (mediaDescription.getIpAddress() != null) {
                                destAddress = mediaDescription.getIpAddress();
                            }
                            MediaDestination mediaDestination =
                                new MediaDestination();
                            mediaDestination.setDestination(
                                    destAddress.getHostAddress());
                            mediaDestination.setPort(destPort);
                            mediaDestination.setCodec(offerCodec);
                            return mediaDestination;
                        }
                    }
                }
            }
        }
        throw new NoCodecException();
    }

    /**
     * 包含了用户信息 / 媒体协商信息 ...
     * 创建会话描述 ....
     * @param offer 有无offer 单独处理
     * @param localRtpPort 本地rtp端口
     * @return
     * @throws IOException
     */
    public SessionDescription createSessionDescription(SessionDescription offer,
            int localRtpPort)
            throws IOException {
        SessionDescription sessionDescription = new SessionDescription();
        // ??? user1 ... 是真草率 牛逼
        sessionDescription.setUsername("user1");
        // 随机id
        sessionDescription.setId(random.nextInt(Integer.MAX_VALUE));
        // 随机版本
        sessionDescription.setVersion(random.nextInt(Integer.MAX_VALUE));
        // 获取配置
        Config config = userAgent.getConfig();

        // ------------------------- 这里些许可能有些问题, 无法进行sip 通信就歇菜 ..
        // 所以最好使用 共有地址,本身sip 信令也必须能够进行数据交互才对 ..
        // 幸好我们的服务和freebpx 在同一个网络中 ... 能够连接 ...
        // a ------------- serverA ------- NAT | ----------------- | NAT -------------- serverB --------------- b
        // 拿取公共地址
        InetAddress inetAddress = config.getPublicInetAddress();
        if (inetAddress == null) {
            // 不行就用私有地址 ...
            inetAddress = config.getLocalInetAddress();
        }
        sessionDescription.setIpAddress(inetAddress);
        sessionDescription.setName("-");
        sessionDescription.setAttributes(new Hashtable<String, String>());
        // 设置支持的编码 就构造器中的三种 ...
        List<Codec> codecs;
        if (offer == null) {
            codecs = supportedCodecs;
        } else {
            // offer中必然收集了想要的codec 属性 ...
            codecs = new ArrayList<Codec>();
            for (MediaDescription mediaDescription:
                    offer.getMediaDescriptions()) {
                if (RFC4566.MEDIA_AUDIO.equals(mediaDescription.getType())) {
                    for (Codec codec: mediaDescription.getCodecs()) {
                        //支持的才进行加入 ...
                        if (supportedCodecs.contains(codec)) {
                            codecs.add(codec);
                        }
                    }
                }
            }
        }
        MediaDescription mediaDescription = new MediaDescription();
        Hashtable<String, String> attributes = new Hashtable<String, String>();
        // 这是什么属性 ... 需要查看文档 ...
        attributes.put(RFC4566.ATTR_SENDRECV, "");
        mediaDescription.setAttributes(attributes);
        mediaDescription.setType(RFC4566.MEDIA_AUDIO);
        // 媒体端口
        mediaDescription.setPort(localRtpPort);
        mediaDescription.setCodecs(codecs);
        List<MediaDescription> mediaDescriptions =
            new ArrayList<MediaDescription>();
        mediaDescriptions.add(mediaDescription);
        sessionDescription.setMediaDescriptions(mediaDescriptions);
        return sessionDescription;
    }

}
