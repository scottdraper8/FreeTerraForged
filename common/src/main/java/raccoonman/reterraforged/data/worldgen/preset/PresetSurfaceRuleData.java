package raccoonman.reterraforged.data.worldgen.preset;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.SurfaceRules;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.surface.rule.RTFSurfaceRules;

public class PresetSurfaceRuleData {

	public static SurfaceRules.RuleSource overworld(Preset preset, HolderGetter<DensityFunction> densityFunctions, HolderGetter<Noise> noise) {
		if (preset.miscellaneous().strataDecorator) {
			// Run your strata rule FIRST so it replaces the surface grass/dirt on land
			return SurfaceRules.sequence(makeStrataRule(noise), SurfaceRuleData.overworld());
		}
		return SurfaceRules.sequence(SurfaceRuleData.overworld());
	}
	private static SurfaceRules.RuleSource makeStrataRule(HolderGetter<Noise> noise) {
		// 1. Fetch the depth noise holder
		Holder<Noise> depth = noise.getOrThrow(PresetStrataNoise.STRATA_DEPTH);

		// 2. Pass the depth noise directly into the rule method instead of the old strata list
		return RTFSurfaceRules.strata(
				RTFCommon.location("overworld_strata"),
				noise.getOrThrow(PresetStrataNoise.STRATA_SELECTOR),
				depth,
				100
		);
	}
}