package com.alipay.sofa.ark.web.embed.tomcat;

import com.alipay.sofa.ark.spi.web.EmbeddedServerService;
import org.apache.catalina.startup.Tomcat;

/**
 * This implementation would be published as ark service.
 *
 * @author qilong.zql
 * @since 0.6.0
 */
public class EmbeddedServerServiceImpl implements EmbeddedServerService<Tomcat> {
    private Tomcat tomcat;
    private Object lock = new Object();

    @Override
    public Tomcat getEmbedServer() {
        return tomcat;
    }

    @Override
    public void setEmbedServer(Tomcat tomcat) {
        if (this.tomcat == null) {
            synchronized (lock) {
                if (this.tomcat == null) {
                    this.tomcat = tomcat;
                }
            }
        }
    }
}