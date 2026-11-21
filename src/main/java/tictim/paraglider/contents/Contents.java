package tictim.paraglider.contents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.*;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import org.jspecify.annotations.NullMarked;
import tictim.paraglider.ParagliderMod;
import tictim.paraglider.api.ParagliderAPI;
import tictim.paraglider.api.StaminaEfficiencyAttribute;
import tictim.paraglider.api.bargain.Bargain;
import tictim.paraglider.api.bargain.BargainPreview;
import tictim.paraglider.contents.block.GoddessStatueBlock;
import tictim.paraglider.contents.block.HornedStatueBlock;
import tictim.paraglider.contents.item.*;
import tictim.paraglider.contents.item.consumeeffect.GiveExtraStaminaConsumeEffect;
import tictim.paraglider.contents.item.consumeeffect.GiveStaminaEfficiencyConsumeEffect;
import tictim.paraglider.contents.item.consumeeffect.RestoreStaminaConsumeEffect;
import tictim.paraglider.contents.mobeffect.StaminaEfficiencyMobEffect;
import tictim.paraglider.contents.recipe.CosmeticRecipe;
import tictim.paraglider.contents.recipe.SimpleBargain;
import tictim.paraglider.contents.recipe.preview.SimplePreview;
import tictim.paraglider.contents.recipe.preview.VesselPreview;
import tictim.paraglider.contents.recipe.WaterBottleIngredientType;
import tictim.paraglider.contents.loot.ParagliderLootConditions;
import tictim.paraglider.contents.worldgen.NetherHornedStatue;
import tictim.paraglider.contents.worldgen.TarreyTownGoddessStatue;
import tictim.paraglider.contents.worldgen.UndergroundHornedStatue;
import tictim.paraglider.impl.AttachmentProvider;
import tictim.paraglider.impl.MovementState;
import tictim.paraglider.impl.SimpleVesselContainer;
import tictim.paraglider.impl.movement.PlayerMovement;
import tictim.paraglider.impl.stamina.BotWStaminaData;
import tictim.paraglider.wind.WindSourceRegistry;

import static tictim.paraglider.api.ParagliderAPI.MODID;
import static tictim.paraglider.contents.CommonContents.*;

@NullMarked
public class Contents {
	private static @org.jetbrains.annotations.Nullable Contents instance;

	/** First field initializer: makes {@link #get()} usable while this object is still being built. */
	@SuppressWarnings("unused")
	private final Contents paraglider$self = (instance = this);

	public static Contents get() {
		return instance != null ? instance : ParagliderMod.instance().getContents();
	}

	/** Registries that must exist before resources are loaded. */
	public static void bootstrapPlatformRegistries() {
		BargainTypeRegistry.bootstrap();
		WindSourceRegistry.bootstrap();
		ParagliderConfigCondition.bootstrap();
		WaterBottleIngredientType.bootstrap();
		ParagliderLootConditions.bootstrap();
		VesselSlotDisplay.bootstrap();
	}

	public final Block goddessStatue = register(BuiltInRegistries.BLOCK, "goddess_statue",
			new GoddessStatueBlock(statueBlock(id("goddess_statue"))));
	public final Block kakarikoGoddessStatue = register(BuiltInRegistries.BLOCK, "kakariko_goddess_statue",
			new GoddessStatueBlock(statueBlock(id("kakariko_goddess_statue"))));
	public final Block goronGoddessStatue = register(BuiltInRegistries.BLOCK, "goron_goddess_statue",
			new GoddessStatueBlock(statueBlock(id("goron_goddess_statue")).lightLevel(value -> 15)));
	public final Block ritoGoddessStatue = register(BuiltInRegistries.BLOCK, "rito_goddess_statue",
			new GoddessStatueBlock(statueBlock(id("rito_goddess_statue"))));
	public final Block hornedStatue = register(BuiltInRegistries.BLOCK, "horned_statue",
			new HornedStatueBlock(statueBlock(id("horned_statue"))));

	public final Holder.Reference<Attribute> maxStamina = registerForHolder(BuiltInRegistries.ATTRIBUTE, "max_stamina",
			new RangedAttribute("attribute.paraglider.max_stamina", 0, 0, Double.MAX_VALUE).setSyncable(true));
	public final Holder.Reference<Attribute> staminaEfficiency = registerForHolder(BuiltInRegistries.ATTRIBUTE, "stamina_efficiency",
			new StaminaEfficiencyAttribute("attribute.paraglider.stamina_efficiency"));
	public final Holder.Reference<Attribute> staminaRecovery = registerForHolder(BuiltInRegistries.ATTRIBUTE, "stamina_recovery",
			new StaminaEfficiencyAttribute("attribute.paraglider.stamina_recovery"));
	public final Holder.Reference<Attribute> paraglidingStaminaEfficiency = registerForHolder(BuiltInRegistries.ATTRIBUTE,
			"paragliding_stamina_efficiency",
			new StaminaEfficiencyAttribute("attribute.paraglider.paragliding_stamina_efficiency"));
	public final Holder.Reference<Attribute> runningStaminaEfficiency = registerForHolder(BuiltInRegistries.ATTRIBUTE,
			"running_stamina_efficiency",
			new StaminaEfficiencyAttribute("attribute.paraglider.running_stamina_efficiency"));
	public final Holder.Reference<Attribute> swimmingStaminaEfficiency = registerForHolder(BuiltInRegistries.ATTRIBUTE,
			"swimming_stamina_efficiency",
			new StaminaEfficiencyAttribute("attribute.paraglider.swimming_stamina_efficiency"));

	public final ConsumeEffect.Type<RestoreStaminaConsumeEffect> restoreStaminaConsumeEffectType =
			register(BuiltInRegistries.CONSUME_EFFECT_TYPE, "restore_stamina", RestoreStaminaConsumeEffect.TYPE);
	public final ConsumeEffect.Type<GiveExtraStaminaConsumeEffect> giveExtraStaminaConsumeEffectType =
			register(BuiltInRegistries.CONSUME_EFFECT_TYPE, "give_extra_stamina", GiveExtraStaminaConsumeEffect.TYPE);
	public final ConsumeEffect.Type<GiveStaminaEfficiencyConsumeEffect> giveStaminaEfficiency =
			register(BuiltInRegistries.CONSUME_EFFECT_TYPE, "give_stamina_efficiency", GiveStaminaEfficiencyConsumeEffect.TYPE);

	public final DataComponentType<Unit> paraglidingFlagComponent = register(BuiltInRegistries.DATA_COMPONENT_TYPE, "paragliding",
			DataComponentType.<Unit>builder()
					.persistent(Unit.CODEC)
					.networkSynchronized(Unit.STREAM_CODEC)
					.build());

	public final Item paraglider = register(BuiltInRegistries.ITEM, "paraglider",
			new ParagliderItem(p(id("paraglider")).stacksTo(1)));
	public final Item dekuLeaf = register(BuiltInRegistries.ITEM, "deku_leaf",
			new ParagliderItem(p(id("deku_leaf")).stacksTo(1)));
	public final Item heartContainer = register(BuiltInRegistries.ITEM, "heart_container",
			new HeartContainerItem(p(id("heart_container")).rarity(Rarity.RARE).fireResistant()));
	public final Item staminaVessel = register(BuiltInRegistries.ITEM, "stamina_vessel",
			new StaminaVesselItem(p(id("stamina_vessel")).rarity(Rarity.RARE).fireResistant()));
	public final Item spiritOrb = register(BuiltInRegistries.ITEM, "spirit_orb",
			new SpiritOrbItem(p(id("spirit_orb")).rarity(Rarity.UNCOMMON).fireResistant()));
	public final Item antiVessel = register(BuiltInRegistries.ITEM, "anti_vessel",
			new AntiVesselItem(p(id("anti_vessel")).rarity(Rarity.EPIC).fireResistant()));
	public final Item essence = register(BuiltInRegistries.ITEM, "essence",
			new EssenceItem(p(id("essence")).rarity(Rarity.RARE).fireResistant()));
	public final Item energizingElixir1 = register(BuiltInRegistries.ITEM, "energizing_elixir_1",
			new Item(staminaPotion(id("energizing_elixir_1"), new RestoreStaminaConsumeEffect(1000))));
	public final Item energizingElixir2 = register(BuiltInRegistries.ITEM, "energizing_elixir_2",
			new Item(staminaPotion(id("energizing_elixir_2"),
					new RestoreStaminaConsumeEffect(2000),
					new GiveStaminaEfficiencyConsumeEffect(0, 60 * 20))));
	public final Item energizingElixir3 = register(BuiltInRegistries.ITEM, "energizing_elixir_3",
			new Item(staminaPotion(id("energizing_elixir_3"),
					new RestoreStaminaConsumeEffect(3000),
					new GiveStaminaEfficiencyConsumeEffect(1, 120 * 20))));
	public final Item enduringElixir1 = register(BuiltInRegistries.ITEM, "enduring_elixir_1",
			new Item(staminaPotion(id("enduring_elixir_1"), new GiveExtraStaminaConsumeEffect(500))));
	public final Item enduringElixir2 = register(BuiltInRegistries.ITEM, "enduring_elixir_2",
			new Item(staminaPotion(id("enduring_elixir_2"), new GiveExtraStaminaConsumeEffect(1000))));
	public final Item enduringElixir3 = register(BuiltInRegistries.ITEM, "enduring_elixir_3",
			new Item(staminaPotion(id("enduring_elixir_3"), new GiveExtraStaminaConsumeEffect(2000))));
	public final Item energizingMixture = register(BuiltInRegistries.ITEM, "energizing_mixture",
			new Item(p(id("energizing_mixture")).stacksTo(1)));
	public final Item enduringMixture = register(BuiltInRegistries.ITEM, "enduring_mixture",
			new Item(p(id("enduring_mixture")).stacksTo(1)));

	public final BlockItem goddessStatueItem = register(BuiltInRegistries.ITEM, "goddess_statue",
			new BlockItem(goddessStatue, p(id("goddess_statue")).rarity(Rarity.RARE)));
	public final BlockItem kakarikoGoddessStatueItem = register(BuiltInRegistries.ITEM, "kakariko_goddess_statue",
			new TooltipBlockItem(kakarikoGoddessStatue, p(id("kakariko_goddess_statue")).rarity(Rarity.RARE),
					kakarikoStatueTooltip()));
	public final BlockItem goronGoddessStatueItem = register(BuiltInRegistries.ITEM, "goron_goddess_statue",
			new TooltipBlockItem(goronGoddessStatue, p(id("goron_goddess_statue")).rarity(Rarity.RARE),
					goronStatueTooltip()));
	public final BlockItem ritoGoddessStatueItem = register(BuiltInRegistries.ITEM, "rito_goddess_statue",
			new TooltipBlockItem(ritoGoddessStatue, p(id("rito_goddess_statue")).rarity(Rarity.RARE),
					ritoStatueTooltip()));
	public final BlockItem hornedStatueItem = register(BuiltInRegistries.ITEM, "horned_statue",
			new TooltipBlockItem(hornedStatue, p(id("horned_statue")).rarity(Rarity.EPIC),
					Component.translatable("tooltip.paraglider.horned_statue.0")
							.setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY))));

	public final RecipeSerializer<?> cosmeticRecipe = register(BuiltInRegistries.RECIPE_SERIALIZER, "cosmetic",
			CosmeticRecipe.SERIALIZER);
	public final RecipeSerializer<?> bargainRecipe = register(BuiltInRegistries.RECIPE_SERIALIZER, "statue_bargain",
			SimpleBargain.SERIALIZER);
	public final RecipeType<Bargain> bargainRecipeType = register(BuiltInRegistries.RECIPE_TYPE, "bargain",
			new RecipeType<Bargain>() {
				@Override public String toString() {
					return ParagliderAPI.id("bargain").toString();
				}
			});

	public final Holder.Reference<MobEffect> staminaEfficiencyEffect = registerForHolder(
			BuiltInRegistries.MOB_EFFECT, "stamina_efficiency", new StaminaEfficiencyMobEffect());

	public final StructureType<TarreyTownGoddessStatue> tarreyTownGoddessStatue =
			structureType("tarrey_town_goddess_statue", TarreyTownGoddessStatue.CODEC);
	public final StructureType<NetherHornedStatue> netherHornedStatue =
			structureType("nether_horned_statue", NetherHornedStatue.CODEC);
	public final StructureType<UndergroundHornedStatue> undergroundHornedStatue =
			structureType("underground_horned_statue", UndergroundHornedStatue.CODEC);

	public final StructurePieceType tarreyTownGoddessStatuePiece = register(BuiltInRegistries.STRUCTURE_PIECE,
			"tarrey_town_goddess_statue", TarreyTownGoddessStatue.pieceType());
	public final StructurePieceType netherHornedStatuePiece = register(BuiltInRegistries.STRUCTURE_PIECE,
			"nether_horned_statue", NetherHornedStatue.pieceType());
	public final StructurePieceType undergroundHornedStatuePiece = register(BuiltInRegistries.STRUCTURE_PIECE,
			"underground_horned_statue", UndergroundHornedStatue.pieceType());

	public final CreativeModeTab tab = register(BuiltInRegistries.CREATIVE_MODE_TAB, MODID, FabricCreativeModeTab.builder()
			.icon(() -> new ItemStack(paraglider))
			.title(Component.translatable("itemGroup." + MODID))
			.displayItems((features, out) -> {
				out.accept(paraglider);
				out.accept(dekuLeaf);
				out.accept(heartContainer);
				out.accept(staminaVessel);
				out.accept(spiritOrb);
				out.accept(antiVessel);
				out.accept(essence);
				out.accept(energizingElixir1());
				out.accept(energizingElixir2());
				out.accept(energizingElixir3());
				out.accept(enduringElixir1());
				out.accept(enduringElixir2());
				out.accept(enduringElixir3());
				out.accept(energizingMixture());
				out.accept(enduringMixture());

				out.accept(goddessStatue);
				out.accept(kakarikoGoddessStatue);
				out.accept(goronGoddessStatue);
				out.accept(ritoGoddessStatue);
				out.accept(hornedStatue);
			}).build());

	// Player movement needs its owning player, so it is attached lazily via
	// AttachmentProvider.getAttachedOrCreate(type, () -> createPlayerMovement(player)).
	public final AttachmentType<PlayerMovement> playerMovement =
			AttachmentRegistry.create(ParagliderAPI.id("player_movement"));
	public final AttachmentType<BotWStaminaData> botwStaminaData =
			AttachmentRegistry.create(ParagliderAPI.id("botw_stamina"), builder -> builder
					.initializer(BotWStaminaData::new)
					.persistent(BotWStaminaData.CODEC.codec()));
	public final AttachmentType<SimpleVesselContainer> vesselContainer =
			AttachmentRegistry.create(ParagliderAPI.id("vessel_container"), builder -> builder
					.initializer(SimpleVesselContainer::new)
					.persistent(SimpleVesselContainer.CODEC.codec())
					.copyOnDeath());
	public final AttachmentType<MovementState> movementState =
			AttachmentRegistry.create(ParagliderAPI.id("movement_state"), builder -> builder
					.initializer(MovementState::new)
					.persistent(MovementState.CODEC.codec()));
	public final AttachmentType<Boolean> movementInitialized =
			AttachmentRegistry.create(ParagliderAPI.id("movement_init"), builder -> builder
					.initializer(() -> false)
					.persistent(Codec.BOOL));

	public final BargainPreview.Type<SimplePreview> simplePreviewType = register(
			BargainTypeRegistry.PREVIEW_TYPE_REGISTRY, "simple", SimplePreview.TYPE);
	public final BargainPreview.Type<VesselPreview> vesselPreviewType = register(
			BargainTypeRegistry.PREVIEW_TYPE_REGISTRY, "vessel", VesselPreview.TYPE);

	private static net.minecraft.resources.Identifier id(String path) {
		return ParagliderAPI.id(path);
	}

	private static <T, V extends T> V register(Registry<T> registry, String path, V value) {
		return Registry.register(registry, id(path), value);
	}

	private static <T> Holder.Reference<T> registerForHolder(Registry<T> registry, String path, T value) {
		return Registry.registerForHolder(registry, id(path), value);
	}

	private static <T extends Structure> StructureType<T> structureType(String path, MapCodec<T> codec) {
		return register(BuiltInRegistries.STRUCTURE_TYPE, path, () -> codec);
	}

	public Item paraglider() { return paraglider; }
	public Item dekuLeaf() { return dekuLeaf; }
	public Item heartContainer() { return heartContainer; }
	public Item staminaVessel() { return staminaVessel; }
	public Item spiritOrb() { return spiritOrb; }
	public Item antiVessel() { return antiVessel; }
	public Item essence() { return essence; }
	public Item energizingElixir1() { return energizingElixir1; }
	public Item energizingElixir2() { return energizingElixir2; }
	public Item energizingElixir3() { return energizingElixir3; }
	public Item enduringElixir1() { return enduringElixir1; }
	public Item enduringElixir2() { return enduringElixir2; }
	public Item enduringElixir3() { return enduringElixir3; }
	public Item energizingMixture() { return energizingMixture; }
	public Item enduringMixture() { return enduringMixture; }
	public Block goddessStatue() { return goddessStatue; }
	public Block kakarikoGoddessStatue() { return kakarikoGoddessStatue; }
	public Block goronGoddessStatue() { return goronGoddessStatue; }
	public Block ritoGoddessStatue() { return ritoGoddessStatue; }
	public Block hornedStatue() { return hornedStatue; }
	public BlockItem goddessStatueItem() { return goddessStatueItem; }
	public BlockItem kakarikoGoddessStatueItem() { return kakarikoGoddessStatueItem; }
	public BlockItem goronGoddessStatueItem() { return goronGoddessStatueItem; }
	public BlockItem ritoGoddessStatueItem() { return ritoGoddessStatueItem; }
	public BlockItem hornedStatueItem() { return hornedStatueItem; }
	public Holder<Attribute> maxStamina() { return maxStamina; }
	public Holder<Attribute> staminaEfficiency() { return staminaEfficiency; }
	public Holder<Attribute> staminaRecovery() { return staminaRecovery; }
	public Holder<Attribute> paraglidingStaminaEfficiency() { return paraglidingStaminaEfficiency; }
	public Holder<Attribute> runningStaminaEfficiency() { return runningStaminaEfficiency; }
	public Holder<Attribute> swimmingStaminaEfficiency() { return swimmingStaminaEfficiency; }
	public DataComponentType<Unit> paraglidingFlagComponent() { return paraglidingFlagComponent; }
	public RecipeType<Bargain> bargainRecipeType() { return bargainRecipeType; }
	public StructureType<TarreyTownGoddessStatue> tarreyTownGoddessStatue() { return tarreyTownGoddessStatue; }
	public StructureType<NetherHornedStatue> netherHornedStatue() { return netherHornedStatue; }
	public StructureType<UndergroundHornedStatue> undergroundHornedStatue() { return undergroundHornedStatue; }
	public StructurePieceType tarreyTownGoddessStatuePiece() { return tarreyTownGoddessStatuePiece; }
	public StructurePieceType netherHornedStatuePiece() { return netherHornedStatuePiece; }
	public StructurePieceType undergroundHornedStatuePiece() { return undergroundHornedStatuePiece; }
	public AttachmentType<PlayerMovement> playerMovement() { return playerMovement; }
	public AttachmentType<BotWStaminaData> botwStaminaData() { return botwStaminaData; }
	public AttachmentType<SimpleVesselContainer> vesselContainer() { return vesselContainer; }
	public AttachmentType<MovementState> movementState() { return movementState; }
	public AttachmentType<Boolean> movementInitialized() { return movementInitialized; }
}
