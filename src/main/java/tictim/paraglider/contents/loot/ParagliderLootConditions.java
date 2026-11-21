package tictim.paraglider.contents.loot;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jspecify.annotations.NullMarked;
import tictim.paraglider.api.ParagliderAPI;
import tictim.paraglider.config.Cfg;
import tictim.paraglider.config.FeatureCfg;

/** Config predicates used by the Fabric loot-table additions. */
@NullMarked
public enum ParagliderLootConditions implements LootItemCondition {
	SPIRIT_ORB_GENS("config_spirit_orb_gens"),
	WITHER_DROPS_VESSEL("config_wither_drops_vessel"),
	ELDER_GUARDIAN_DROPS_SPIRIT_ORB("config_elder_guardian_drops_spirit_orb"),
	SPIRIT_ORB_LOOTS("config_spirit_orb_loots"),
	HEART_VESSEL_AVAILABLE("config_heart_vessel_available"),
	STAMINA_VESSEL_FALLBACK("config_stamina_vessel_fallback");

	private final MapCodec<ParagliderLootConditions> codec = MapCodec.unit(this);
	private final String id;

	ParagliderLootConditions(String id) {
		this.id = id;
	}

	@Override public MapCodec<ParagliderLootConditions> codec() {
		return this.codec;
	}

	@Override public boolean test(LootContext lootContext) {
		return switch (this) {
			case SPIRIT_ORB_GENS -> FeatureCfg.get().enableSpiritOrbGens();
			case WITHER_DROPS_VESSEL -> Cfg.get().witherDropsVessel();
			case ELDER_GUARDIAN_DROPS_SPIRIT_ORB -> Cfg.get().elderGuardianDropsSpiritOrb();
			case SPIRIT_ORB_LOOTS -> Cfg.get().spiritOrbLoots();
			case HEART_VESSEL_AVAILABLE -> FeatureCfg.get().enableHeartContainers();
			case STAMINA_VESSEL_FALLBACK -> !FeatureCfg.get().enableHeartContainers()
					&& FeatureCfg.get().enableStaminaVessels();
		};
	}

	public LootItemCondition.Builder builder() {
		return () -> this;
	}

	public static void bootstrap() {
		for (ParagliderLootConditions condition : values()) {
			Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE,
					ParagliderAPI.id(condition.id), condition.codec);
		}
	}
}
