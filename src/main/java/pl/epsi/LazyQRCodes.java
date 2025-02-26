package pl.epsi;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LazyQRCodes implements ModInitializer {
	public static final String MOD_ID = "lazyqrcodes";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Loading Lazy QR Codes!");
	}
}