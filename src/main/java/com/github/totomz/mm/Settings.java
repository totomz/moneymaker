package com.github.totomz.mm;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

/**
 *
 * @author Tommaso Doninelli <tommaso.doninelli@gmail.com>
 */
public class Settings {
    
    private static final JedisPool jedisPool;
    private static Logger log = LoggerFactory.getLogger(Settings.class);
    
    static{
        jedisPool = Optional.ofNullable(System.getenv("REDISCLOUD_URL"))
            .map(uri -> {
                try {
                        URI redisUri = new URI(uri);
                        return new JedisPool(new JedisPoolConfig(),
                                redisUri.getHost(),
                                redisUri.getPort(),
                                Protocol.DEFAULT_TIMEOUT,
                                redisUri.getUserInfo().split(":",2)[1]);
                } 
                catch (URISyntaxException e) {
                    log.error("Could not parse REDIS URI - " + uri, e);
                    return null;
                }                    
            })
            .orElseGet(() -> {return new JedisPool("localhost");});
    }

    public static Jedis jedis() {
        return jedisPool.getResource();
    }
    
}
