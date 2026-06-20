package raccoonman.reterraforged.world.worldgen.surface.rule;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.SurfaceRules;
import raccoonman.reterraforged.platform.RegistryUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;

public class RTFSurfaceRules {

	public static void bootstrap() {
		register("strata", StrataRule.CODEC);
	}

	public static StrataRule strata(ResourceLocation name, Holder<Noise> selector, Holder<Noise> depthNoise, int iterations) {
		return new StrataRule(name, selector, depthNoise, iterations);
	}

	public static void register(String name, MapCodec<? extends SurfaceRules.RuleSource> value) {
		RegistryUtil.register(BuiltInRegistries.MATERIAL_RULE, name, value);
	}
}