package tictim.paraglider.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tictim.paraglider.contents.recipe.ParagliderBrewingRecipes;

/** Fabric replacement for NeoForge's RegisterBrewingRecipesEvent. */
@Mixin(PotionBrewing.class)
public abstract class MixinPotionBrewing {
	@Inject(method = "isIngredient", at = @At("HEAD"), cancellable = true)
	private void paraglider$acceptCustomIngredient(ItemStack ingredient, CallbackInfoReturnable<Boolean> info) {
		if (ParagliderBrewingRecipes.isCustomIngredient(ingredient)) info.setReturnValue(true);
	}

	@Inject(method = "hasMix", at = @At("HEAD"), cancellable = true)
	private void paraglider$hasCustomMix(ItemStack input, ItemStack ingredient,
	                                     CallbackInfoReturnable<Boolean> info) {
		if (ParagliderBrewingRecipes.result(input, ingredient) != null) info.setReturnValue(true);
	}

	@Inject(method = "mix", at = @At("HEAD"), cancellable = true)
	private void paraglider$applyCustomMix(ItemStack ingredient, ItemStack input,
	                                       CallbackInfoReturnable<ItemStack> info) {
		net.minecraft.world.item.Item result = ParagliderBrewingRecipes.result(input, ingredient);
		if (result != null) info.setReturnValue(new ItemStack(result));
	}
}
