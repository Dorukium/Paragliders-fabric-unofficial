package tictim.paraglider.contents.recipe;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.jspecify.annotations.NullMarked;
import tictim.paraglider.contents.Contents;

/** Brewing recipes that the vanilla item-only brewing registry cannot express. */
@NullMarked
public final class ParagliderBrewingRecipes {
	private ParagliderBrewingRecipes() {}

	public static boolean isCustomIngredient(ItemStack ingredient) {
		return ingredient.is(Items.CARROT)
				|| ingredient.is(Items.GLOWSTONE_DUST)
				|| ingredient.is(Items.GOLDEN_CARROT);
	}

	public static Item result(ItemStack input, ItemStack ingredient) {
		Contents contents = Contents.get();
		if (ingredient.is(Items.CARROT) && isWaterBottle(input)) return contents.energizingElixir2();
		if (ingredient.is(Items.GLOWSTONE_DUST)) {
			if (input.is(contents.energizingElixir2())) return contents.energizingElixir3();
			if (input.is(contents.enduringElixir2())) return contents.enduringElixir3();
		}
		if (ingredient.is(Items.GOLDEN_CARROT)) {
			if (input.is(contents.energizingElixir2())) return contents.enduringElixir2();
			if (input.is(contents.energizingElixir3())) return contents.enduringElixir3();
		}
		return null;
	}

	private static boolean isWaterBottle(ItemStack stack) {
		return stack.is(Items.POTION)
				&& stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.WATER);
	}
}
