package net.sourceforge.peers.media;
/**
 * @author FLJ
 * @date 2022/7/13
 * @time 10:15
 * @Description 通过将 客户端执行 invite 操作前实现将音频数据存入 ...
 */
public class MediaUtil {
    /**
     * 实现线程安全 ....
     * 本质上,也仅仅只是为了方便,尽可能少的修改peer 源代码 ...
     *
     * 同时,一个client 同时也只能发起一个invite 通话 ...
     */
    private final static ThreadLocal<MediaResource> local = new ThreadLocal<>();

    private MediaUtil() {

    }

    public static MediaResource get() {

        MediaResource data = local.get();

        if(data == null) {
            local.remove();
        }
        return data;
    }

    public static void set(MediaResource resource) {

        if(resource == null) {
            local.remove();
        }

        local.set(resource);
    }


}
