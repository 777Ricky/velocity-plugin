package net.analyse.plugin;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ProxyReloadEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializerCollection;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

public class AnalysePlugin extends Plugin implements Listener {
    private Jedis redis;
    private AnalyseConfig config;

    @Override
    public void onEnable() {
        this.getLogger().info("Enabling Analyse");

        try {
            this.config = loadConfig();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config", e);
        }

        this.loadRedis();

        this.getProxy().getPluginManager().registerListener(this, this);
    }

    @EventHandler
    public void onJoin(PreLoginEvent event) {
        InetSocketAddress virtualDomain = event.getConnection().getVirtualHost();

        if (virtualDomain != null) {
            this.redis.set("analyse:connected_via:" + event.getConnection().getName(), virtualDomain.getHostName());
        }
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent event) {
        final ProxiedPlayer player = event.getPlayer();

        this.getProxy().getScheduler()
                .schedule(this, () -> {
                    if (player != null && this.getProxy().getPlayer(player.getUniqueId()).isConnected()) return;
                    this.redis.del("analyse:connected_via:" + player.getName());
                }, 10L, TimeUnit.SECONDS);
    }

    @EventHandler
    public void onReload(ProxyReloadEvent event) {
        reloadConfig();
    }

    public void reloadConfig() {
        try {
            this.config = loadConfig();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private AnalyseConfig loadConfig() throws Exception {
        TypeSerializerCollection serializerCollection = TypeSerializerCollection.create();

        ConfigurationOptions options = ConfigurationOptions.defaults()
                .withSerializers(serializerCollection);

        ConfigurationNode configNode = YAMLConfigurationLoader.builder()
                .setDefaultOptions(options)
                .setFile(getBundledFile("config.yml"))
                .build()
                .load();

        return new AnalyseConfig(configNode);
    }

    private File getBundledFile(String name) {
        File file = new File(this.getDataFolder(), name);

        if (!file.exists()) {
            this.getDataFolder().mkdir();
            try (InputStream in = AnalysePlugin.class.getResourceAsStream("/" + name)) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return file;
    }

    public Jedis loadRedis() {
        this.getLogger().info("Connecting to Redis under " + this.config.getHost() + ":" + config.getPort() + "..");
        this.redis = new Jedis(config.getHost(), config.getPort());
        return redis;
    }
}
