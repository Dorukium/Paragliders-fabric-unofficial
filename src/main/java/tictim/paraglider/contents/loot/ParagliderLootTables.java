package tictim.paraglider.contents.loot;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.jspecify.annotations.NullMarked;
import tictim.paraglider.ParagliderUtils;
import tictim.paraglider.config.Cfg;
import tictim.paraglider.config.FeatureCfg;
import tictim.paraglider.contents.Contents;

import java.util.List;
import java.util.Map;

/**
 * Fabric replacement for the NeoForge global loot modifiers. Every upstream modifier JSON is
 * represented here as a programmatic loot-table addition with identical conditions and counts.
 */
@NullMarked
public final class ParagliderLootTables {
	private static final Map<Identifier, Float> SPIRIT_ORB_CHESTS = Map.ofEntries(
			Map.entry(id("minecraft:chests/underwater_ruin_big"), 0.25f),
			Map.entry(id("minecraft:chests/underwater_ruin_small"), 0.25f),
			Map.entry(id("minecraft:chests/jungle_temple"), 0.66f),
			Map.entry(id("minecraft:chests/desert_pyramid"), 0.25f),
			Map.entry(id("minecraft:chests/bastion_other"), 1f),
			Map.entry(id("minecraft:chests/bastion_bridge"), 1f),
			Map.entry(id("minecraft:chests/bastion_treasure"), 1f),
			Map.entry(id("minecraft:chests/bastion_hoglin_stable"), 1f),
			Map.entry(id("minecraft:chests/stronghold_corridor"), 0.5f),
			Map.entry(id("minecraft:chests/stronghold_crossing"), 0.5f),
			Map.entry(id("minecraft:chests/stronghold_library"), 1f),
			Map.entry(id("minecraft:chests/nether_bridge"), 0.5f),
			Map.entry(id("minecraft:chests/buried_treasure"), 1f),
			Map.entry(id("minecraft:chests/woodland_mansion"), 1f),
			Map.entry(id("minecraft:chests/ancient_city"), 1f));

	private static boolean registered;

	private ParagliderLootTables() {}

	public static synchronized void register() {
		if (registered) return;
		registered = true;
		LootTableEvents.MODIFY.register((key, table, source, registries) -> {
			// Never modify datapack overrides - only the built-in vanilla tables, like upstream.
			if (source.isBuiltin()) modify(key.identifier(), table);
		});
	}

	private static void modify(Identifier id, LootTable.Builder table) {
		Contents contents = Contents.get();

		Float chance = SPIRIT_ORB_CHESTS.get(id);
		if (chance != null) {
			LootItem.Builder<?> entry = LootItem.lootTableItem(contents.spiritOrb())
					.when(ParagliderLootConditions.SPIRIT_ORB_GENS.builder())
					.when(ParagliderLootConditions.SPIRIT_ORB_LOOTS.builder());
			if (chance < 1f) entry.when(LootItemRandomChanceCondition.randomChance(chance));
			table.withPool(singleRoll().add(entry));
			return;
		}

		if (id.equals(id("minecraft:chests/trial_chambers/reward"))) {
			addConfiguredSpiritOrbs(table, Cfg::trialSpiritOrbDrops);
		} else if (id.equals(id("minecraft:chests/trial_chambers/reward_ominous"))) {
			addConfiguredSpiritOrbs(table, Cfg::ominousTrialSpiritOrbDrops);
		} else if (id.equals(id("minecraft:blocks/spawner"))) {
			addConfiguredSpiritOrbs(table, Cfg::spawnerSpiritOrbDrops);
		} else if (id.equals(id("minecraft:entities/elder_guardian"))) {
			table.withPool(singleRoll()
					.when(LootItemKilledByPlayerCondition.killedByPlayer())
					.when(ParagliderLootConditions.SPIRIT_ORB_GENS.builder())
					.when(ParagliderLootConditions.ELDER_GUARDIAN_DROPS_SPIRIT_ORB.builder())
					.add(LootItem.lootTableItem(contents.spiritOrb())));
		} else if (id.equals(id("minecraft:entities/wither"))) {
			table.withPool(singleRoll()
					.when(LootItemKilledByPlayerCondition.killedByPlayer())
					.when(ParagliderLootConditions.WITHER_DROPS_VESSEL.builder())
					.add(LootItem.lootTableItem(contents.heartContainer())
							.when(ParagliderLootConditions.HEART_VESSEL_AVAILABLE.builder()))
					.add(LootItem.lootTableItem(contents.staminaVessel())
							.when(ParagliderLootConditions.STAMINA_VESSEL_FALLBACK.builder())));
		} else if (id.equals(id("totw_reworked:tower_chest"))) {
			addTotwItems(table, false);
		} else if (id.equals(id("totw_reworked:ocean_tower_chest"))) {
			addTotwItems(table, true);
		}
	}

	private static void addConfiguredSpiritOrbs(LootTable.Builder table,
	                                            java.util.function.ToDoubleFunction<Cfg> drops) {
		NumberProvider count = new NumberProvider() {
			@Override public float getFloat(LootContext context) {
				return (float)drops.applyAsDouble(Cfg.get());
			}

			/** Upstream treats fractional config values as a chanced extra drop. */
			@Override public int getInt(LootContext context) {
				double value = drops.applyAsDouble(Cfg.get());
				if (value <= 0) return 0;
				int result = (int)value;
				double fraction = value % 1;
				if (fraction > 0 && context.getRandom().nextDouble() < fraction) result++;
				return result;
			}

			@Override public MapCodec<? extends NumberProvider> codec() {
				return MapCodec.unit(this);
			}
		};
		table.withPool(singleRoll()
				.when(ParagliderLootConditions.SPIRIT_ORB_GENS.builder())
				.add(LootItem.lootTableItem(Contents.get().spiritOrb())
						.apply(SetItemCountFunction.setCount(count))));
	}

	private static void addTotwItems(LootTable.Builder table, boolean dekuLeafSource) {
		Contents contents = Contents.get();
		LootItemFunction.Builder dyeFunction = () -> new LootItemFunction() {
			@Override public ItemStack apply(ItemStack stack, LootContext context) {
				Cfg.TotwCompatConfigOption option = Cfg.get().paragliderInTowersOfTheWild();
				if (option == Cfg.TotwCompatConfigOption.DISABLE) return stack;
				RandomSource random = context.getRandom();
				if (!random.nextBoolean()) return stack;
				return DyedItemColor.applyDyes(stack, List.of(
						DyeColor.byId(random.nextInt(16)), DyeColor.byId(random.nextInt(16))));
			}
				@Override public MapCodec<? extends LootItemFunction> codec() {
					return MapCodec.unit(this);
				}
			};

		table.withPool(singleRoll()
				.add(LootItem.lootTableItem(contents.paraglider())
						.when(totwCondition(dekuLeafSource, false))
						.apply(dyeFunction))
				.add(LootItem.lootTableItem(contents.dekuLeaf())
						.when(totwCondition(dekuLeafSource, true))
						.apply(dyeFunction)));
	}

	private static net.minecraft.world.level.storage.loot.predicates.LootItemCondition.Builder totwCondition(
			boolean dekuLeafSource, boolean dekuLeafItem) {
		return () -> new net.minecraft.world.level.storage.loot.predicates.LootItemCondition() {
			@Override public MapCodec<? extends net.minecraft.world.level.storage.loot.predicates.LootItemCondition> codec() {
				return MapCodec.unit(this);
			}
			@Override public boolean test(LootContext context) {
				return isTotwAvailable(dekuLeafSource, dekuLeafItem);
			}
		};
	}

	private static boolean isTotwAvailable(boolean dekuLeafSource, boolean dekuLeafItem) {
		Cfg.TotwCompatConfigOption option = Cfg.get().paragliderInTowersOfTheWild();
		// Mirrors upstream: DEKU_LEAF_ONLY wins; otherwise DEFAULT uses a Deku leaf only in ocean chests.
		return switch (option) {
			case DISABLE -> false;
			case DEKU_LEAF_ONLY -> dekuLeafItem;
			case PARAGLIDER_ONLY -> !dekuLeafItem;
			case DEFAULT -> dekuLeafItem == dekuLeafSource;
		};
	}

	private static LootPool.Builder singleRoll() {
		return LootPool.lootPool().setRolls(ConstantValue.exactly(1));
	}

	private static Identifier id(String id) {
		return Identifier.parse(id);
	}
}
