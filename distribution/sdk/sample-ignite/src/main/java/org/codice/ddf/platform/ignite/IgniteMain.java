package org.codice.ddf.platform.ignite;

import java.util.Arrays;
import java.util.List;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

/**
 * Hello world!
 */
public class IgniteMain {
    private static Ignite ignite;

    public static void main(String[] args) throws Exception {
        System.setProperty("IGNITE_QUIET", "false");

        TcpDiscoverySpi spi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        TcpCommunicationSpi commSpi = new TcpCommunicationSpi();

        // Set initial IP addresses.
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500", "127.0.0.1:47501"));

        spi.setIpFinder(ipFinder);

        // Override local port.
        commSpi.setLocalPort(47500);
        //        commSpi.setLocalAddress("XX.XX.XX.XX");
        commSpi.setLocalPortRange(50);

        IgniteConfiguration cfg = new IgniteConfiguration();

        // Override default communication SPI.
        cfg.setCommunicationSpi(commSpi);
        cfg.setDiscoverySpi(spi);
        //        cfg.setAddressResolver(new IotAddressResolver());
        cfg.setClientMode(true);

        // Start Ignite node
        ignite = Ignition.start(cfg);

        Thread.sleep(10000);
        String name = "cache";
        Cache<String, List> cache = Caching.getCache(name, String.class, List.class);
        if (cache == null) {
            CachingProvider cachingProvider = Caching.getCachingProvider();
            CacheManager mgr = cachingProvider.getCacheManager();
            MutableConfiguration<String, List> config = new MutableConfiguration<>();
            config.setTypes(String.class, List.class);
            config.setStoreByValue(true);
            config.setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(Duration.THIRTY_MINUTES));
            //            Notification.show("Creating cache",
            //                    Notification.Type.WARNING_MESSAGE);
            cache = mgr.createCache(name, config);
        }

        while (true) {

            Cache<String, List> cache2 = Caching.getCache(name, String.class, List.class);
            if (cache2 != null && cache2.get("Hello") != null) {
                //                cache2.get("Hello");
            }
            Thread.sleep(10000);
        }
    }
}
