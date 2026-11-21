package tictim.paraglider.mixin;

import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tictim.paraglider.contents.item.ParagliderItem;

/** Upstream disables grindstone repair of paragliders. */
@Mixin(GrindstoneMenu.class)
public abstract class MixinGrindstoneMenu {
	@Inject(method = "mergeItems", at = @At("HEAD"), cancellable = true)
	private void paraglider$preventRepair(ItemStack first, ItemStack second,
	                                      CallbackInfoReturnable<ItemStack> info) {
		if (first.getItem() instanceof ParagliderItem || second.getItem() instanceof ParagliderItem) {
			info.setReturnValue(ItemStack.EMPTY);
		}
	}
}
