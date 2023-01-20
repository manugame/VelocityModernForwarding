package be.manugame.velocitymodernforwarding.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;

public class VelocityModernForwardingConfig {

    public static final Server SERVER;
    public static final ForgeConfigSpec SERVER_SPEC;

    static {
        Pair<Server, ForgeConfigSpec> specPair = new Builder().configure(Server::new);
        SERVER = specPair.getLeft();
        SERVER_SPEC = specPair.getRight();
    }

    public static void register() {
        ModLoadingContext context = ModLoadingContext.get();
        context.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
    }

    public static String getSecretToken() {
        return SERVER.secretToken.get();
    }

    public static boolean isEnabled() {
        return SERVER.isEnabled.get();
    }

    public static class Server {
        public final ConfigValue<String> secretToken;
        public final BooleanValue isEnabled;

        Server(Builder builder) {
            builder.push("Velocity Modern Forwarding");

            secretToken = builder
                    .comment("Secret token of your velocity proxy.")
                    .define("secret-token", "CHANGEME");

            isEnabled = builder
                    .comment("Is Velocity Modern Forwarding is enabled.")
                    .define("enabled", true);
        }
    }
}
