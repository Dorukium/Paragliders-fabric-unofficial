package tictim.paraglider.contents;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.resources.RegistryOps;
import org.jspecify.annotations.NullMarked;
import tictim.paraglider.api.ParagliderAPI;
import tictim.paraglider.config.FeatureCfg;

import java.util.Locale;

/** Fabric resource conditions used by the vessel recipes via {@code fabric:load_conditions}. */
@NullMarked
public enum ParagliderConfigCondition implements ResourceCondition {
	HEART_CONTAINER_ENABLED,
	STAMINA_VESSEL_ENABLED;

	private static boolean bootstrapped;

	private final ResourceConditionType<ParagliderConfigCondition> type = ResourceConditionType.create(
			ParagliderAPI.id(name().toLowerCase(Locale.ROOT)), MapCodec.unit(this));

	@Override public ResourceConditionType<?> getType() {
		return this.type;
	}

	@Override public boolean test(RegistryOps.RegistryInfoLookup lookup) {
		return switch (this) {
			case HEART_CONTAINER_ENABLED -> FeatureCfg.get().enableHeartContainers();
			case STAMINA_VESSEL_ENABLED -> FeatureCfg.get().enableStaminaVessels();
		};
	}

	public static synchronized void bootstrap() {
		if (bootstrapped) return;
		for (ParagliderConfigCondition condition : values()) {
			ResourceConditions.register(condition.type);
		}
		bootstrapped = true;
	}
}
