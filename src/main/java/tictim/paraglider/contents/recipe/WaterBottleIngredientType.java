package tictim.paraglider.contents.recipe;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.jspecify.annotations.NullMarked;
import tictim.paraglider.ParagliderUtils;
import tictim.paraglider.api.ParagliderAPI;

import java.util.stream.Stream;

/** Fabric custom ingredient matching only vanilla water bottles. */
@NullMarked
public enum WaterBottleIngredientType implements CustomIngredient {
	INSTANCE;

	private static final MapCodec<WaterBottleIngredientType> CODEC = MapCodec.unit(INSTANCE);
	private static final StreamCodec<RegistryFriendlyByteBuf, WaterBottleIngredientType> PACKET_CODEC =
			StreamCodec.unit(INSTANCE);

	public static final CustomIngredientSerializer<WaterBottleIngredientType> SERIALIZER =
			new CustomIngredientSerializer<>() {
				@Override public Identifier getIdentifier() {
					return ParagliderAPI.id("water_bottle");
				}

				@Override public MapCodec<WaterBottleIngredientType> getCodec() {
					return CODEC;
				}

				@Override public StreamCodec<RegistryFriendlyByteBuf, WaterBottleIngredientType> getStreamCodec() {
					return PACKET_CODEC;
				}
			};

	private static boolean bootstrapped;

	@Override public boolean test(ItemStack stack) {
		return stack.is(Items.POTION) &&
				stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.WATER);
	}

	@Override public Stream<Holder<Item>> items() {
		return Stream.of(BuiltInRegistries.ITEM.wrapAsHolder(Items.POTION));
	}

	@Override public boolean requiresTesting() {
		return true;
	}

	@Override public CustomIngredientSerializer<?> getSerializer() {
		return SERIALIZER;
	}

	@Override public SlotDisplay display() {
		return ParagliderUtils.stackDisplay(PotionContents.createItemStack(Items.POTION, Potions.WATER));
	}

	public static synchronized void bootstrap() {
		if (bootstrapped) return;
		CustomIngredientSerializer.register(SERIALIZER);
		bootstrapped = true;
	}

	@Override public String toString() {
		return "WaterBottleIngredientType";
	}
}
