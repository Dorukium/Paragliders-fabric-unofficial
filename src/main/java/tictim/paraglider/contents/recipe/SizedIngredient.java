package tictim.paraglider.contents.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jspecify.annotations.NullMarked;

import java.util.function.Predicate;

/**
 * Fabric replacement for NeoForge's {@code SizedIngredient}: an ingredient paired with a count.
 * Keeps the upstream JSON shape ({@code ingredient} + {@code count}) so all data files stay valid.
 */
@NullMarked
public record SizedIngredient(Ingredient ingredient, int count) implements Predicate<ItemStack> {
	public static final MapCodec<SizedIngredient> NESTED_CODEC = RecordCodecBuilder.mapCodec(b -> b.group(
			Ingredient.CODEC.fieldOf("ingredient").forGetter(SizedIngredient::ingredient),
			Codec.INT.fieldOf("count").forGetter(SizedIngredient::count)
	).apply(b, SizedIngredient::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, SizedIngredient> STREAM_CODEC = StreamCodec.composite(
			Ingredient.CONTENTS_STREAM_CODEC, SizedIngredient::ingredient,
			ByteBufCodecs.VAR_INT, SizedIngredient::count,
			SizedIngredient::new
	);

	public SizedIngredient {
		count = Math.max(0, count);
	}

	@Override public boolean test(ItemStack stack) {
		return this.ingredient.test(stack);
	}
}
