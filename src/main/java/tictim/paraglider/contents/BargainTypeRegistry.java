package tictim.paraglider.contents;

import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.NullMarked;
import tictim.paraglider.api.ParagliderAPI;
import tictim.paraglider.api.bargain.BargainPreview;
import tictim.paraglider.api.bargain.BargainType;

@NullMarked
public final class BargainTypeRegistry {
	private BargainTypeRegistry() {}

	public static final ResourceKey<Registry<BargainType>> REGISTRY_KEY =
			ResourceKey.createRegistryKey(ParagliderAPI.id("bargain_types"));

	/** Synced registry for preview codecs referenced by bargain packets. */
	public static final Registry<BargainPreview.Type<?>> PREVIEW_TYPE_REGISTRY =
			FabricRegistryBuilder.create(BargainPreview.TYPE_REGISTRY_KEY)
					.attribute(RegistryAttribute.SYNCED)
					.buildAndRegister();

	private static boolean bootstrapped;

	public static synchronized void bootstrap() {
		if (bootstrapped) return;
		DynamicRegistries.register(REGISTRY_KEY, BargainType.CODEC);
		bootstrapped = true;
	}

	public static BargainType getFromID(RegistryAccess registryAccess, Identifier id) {
		return registryAccess.lookupOrThrow(REGISTRY_KEY).getValue(id);
	}
}
