package net.sourceforge.peers.media;
/**
 * @author FLJ
 * @date 2022/7/13
 * @time 10:17
 * @Description 可能包含 音视频,目前只处理音频
 */
public interface MediaResource {
    /**
     * 由于底层设备会尝试一直读取它,那么当它第二次读取的时候,返回null(需要) ..
     * 如果需要一直回放,那么就一直返回这段数据即可 ...
     * @return
     */
    byte[] getAudioData();

    byte[] getVideoData();

    MediaFormat getMediaFormat();


    enum MediaFormat {
        video,
        audio
    }
}
