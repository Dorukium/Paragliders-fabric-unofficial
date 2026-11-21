package tictim.paraglider.bargain;

import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jspecify.annotations.NullMarked;
import tictim.paraglider.ParagliderMod;
import tictim.paraglider.api.bargain.Bargain;
import tictim.paraglider.api.bargain.BargainType;
import tictim.paraglider.contents.BargainTypeRegistry;
import tictim.paraglider.contents.Contents;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@NullMarked
public class BargainRecipeChecker extends SimplePreparableReloadListener<Void> {
	private final RecipeManager recipeManager;
	private final net.minecraft.core.HolderLookup.Provider registryLookup;

	public BargainRecipeChecker(RecipeManager recipeManager, net.minecraft.core.HolderLookup.Provider registryLookup) {
		this.recipeManager = recipeManager;
		this.registryLookup = registryLookup;
	}

	@Override protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
		return null;
	}

	@Override protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
		check();
	}

	/** Public entry point for Fabric's data-pack reload callback. */
	public void check() {
		Map<Identifier, List<RecipeHolder<Bargain>>> missingBargainTypes = new Object2ObjectAVLTreeMap<>();
		HolderLookup.RegistryLookup<BargainType> bargainTypes = this.registryLookup.lookupOrThrow(BargainTypeRegistry.REGISTRY_KEY);
		int count = 0;

		for (RecipeHolder<?> holder : this.recipeManager.getRecipes()) {
			if (holder.value().getType() != Contents.get().bargainRecipeType()) continue;
			RecipeHolder<Bargain> b = (RecipeHolder<Bargain>)(Object)holder;
			Identifier bargainType = b.value().getBargainType();
			var type = bargainTypes.get(ResourceKey.create(BargainTypeRegistry.REGISTRY_KEY, bargainType));

			if (type.isEmpty()) {
				missingBargainTypes
						.computeIfAbsent(bargainType, s -> new ArrayList<>())
						.add(b);
				count++;
			}
		}

		if (count > 0) {
			ParagliderMod.LOGGER.error("Found {} issues in bargain recipes:\n  {}", count,
					missingBargainTypes.entrySet().stream()
							.map(e ->
									"Cannot resolve bargain type " + e.getKey() + " for bargain recipe(s) " +
											e.getValue().stream()
													.map(r -> r.id().identifier().toString())
													.collect(Collectors.joining(", ")))
							.collect(Collectors.joining("\n  ")));
		}
	}
}
