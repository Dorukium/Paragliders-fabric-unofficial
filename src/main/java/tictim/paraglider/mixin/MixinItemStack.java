package tictim.paraglider.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tictim.paraglider.config.Cfg;
import tictim.paraglider.contents.item.ParagliderItem;

/** Supplies the stack-aware, config-driven durability NeoForge adds to Item. */
@Mixin(ItemStack.class)
public abstract class MixinItemStack {
	@Inject(method = "getMaxDamage", at = @At("HEAD"), cancellable = true)
	private void paraglider$configuredMaxDamage(CallbackInfoReturnable<Integer> info) {
		ItemStack stack = (ItemStack)(Object)this;
		if (stack.getItem() instanceof ParagliderItem) {
			info.setReturnValue(Math.max(1, Cfg.get().paragliderDurability()));
		}
	}
}
