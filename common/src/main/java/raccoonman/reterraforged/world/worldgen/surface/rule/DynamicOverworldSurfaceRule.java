package raccoonman.reterraforged.world.worldgen.surface.rule;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;

import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;

public record DynamicOverworldSurfaceRule(int deepslateFullY, int deepslateTransitionTopY) implements SurfaceRules.RuleSource {
	public static final MapCodec<DynamicOverworldSurfaceRule> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Codec.INT.fieldOf("deepslate_full_y").forGetter(DynamicOverworldSurfaceRule::deepslateFullY),
		Codec.INT.fieldOf("deepslate_transition_top_y").forGetter(DynamicOverworldSurfaceRule::deepslateTransitionTopY)
	).apply(instance, DynamicOverworldSurfaceRule::new));

	@Override
	public SurfaceRules.SurfaceRule apply(SurfaceRules.Context ctx) {
		SurfaceRules.SurfaceRule overworld = SurfaceRuleData.overworld().apply(ctx);
		SurfaceRules.Condition deepslate = SurfaceRules.verticalGradient(
			"reterraforged:deepslate",
			VerticalAnchor.absolute(this.deepslateFullY),
			VerticalAnchor.absolute(this.deepslateTransitionTopY)
		).apply(ctx);
		return new Source(overworld, deepslate);
	}

	@Override
	public KeyDispatchDataCodec<DynamicOverworldSurfaceRule> codec() {
		return new KeyDispatchDataCodec<>(CODEC);
	}

	private record Source(SurfaceRules.SurfaceRule overworld, SurfaceRules.Condition deepslate) implements SurfaceRules.SurfaceRule {
		@Nullable
		@Override
		public BlockState tryApply(int x, int y, int z) {
			BlockState state = this.overworld.tryApply(x, y, z);
			if (state != null && state.is(Blocks.DEEPSLATE)) {
				return this.deepslate.test() ? state : null;
			}
			return state;
		}
	}
}
