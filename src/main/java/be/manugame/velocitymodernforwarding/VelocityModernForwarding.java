package be.manugame.velocitymodernforwarding;

import be.manugame.velocitymodernforwarding.config.VelocityModernForwardingConfig;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkConstants;
import org.slf4j.Logger;

@Mod("velocitymodernforwarding")
public class VelocityModernForwarding {

    private static final Logger LOGGER = LogUtils.getLogger();

    public VelocityModernForwarding() {
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        VelocityModernForwardingConfig.register();
    }

}
