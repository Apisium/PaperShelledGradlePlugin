package cn.apisium.example;

import net.minecraft.server.dedicated.DedicatedServer;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("unused")
public class Main extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info(((DedicatedServer) getServer()).getServerModName());
    }
}
