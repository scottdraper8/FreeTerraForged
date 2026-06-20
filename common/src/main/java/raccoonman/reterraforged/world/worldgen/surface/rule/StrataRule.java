package raccoonman.reterraforged.world.worldgen.surface.rule;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceRules.Context;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;
import raccoonman.reterraforged.world.worldgen.surface.RTFSurfaceSystem;

public record StrataRule(ResourceLocation name, Holder<Noise> selector, Holder<Noise> depthNoise, int iterations) implements SurfaceRules.RuleSource {

	private static final BlockState[] HARDCODED_LAYERS = {
			Blocks.MOSS_BLOCK.defaultBlockState(),
			Blocks.MOSSY_COBBLESTONE.defaultBlockState(),
			Blocks.STONE.defaultBlockState(),
			Blocks.ANDESITE.defaultBlockState(),
			Blocks.COBBLESTONE.defaultBlockState(),
			Blocks.TUFF.defaultBlockState(),
			Blocks.DEEPSLATE.defaultBlockState(),
			Blocks.SMOOTH_BASALT.defaultBlockState()
	};

	public static final MapCodec<StrataRule> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			ResourceLocation.CODEC.fieldOf("name").forGetter(StrataRule::name),
			Noise.CODEC.fieldOf("selector").forGetter(StrataRule::selector),
			Noise.CODEC.fieldOf("depth_noise").forGetter(StrataRule::depthNoise),
			Codec.INT.fieldOf("iterations").forGetter(StrataRule::iterations)
	).apply(instance, StrataRule::new));

	@Override
	public Source apply(Context ctx) {
		if(ctx.system instanceof RTFSurfaceSystem rtfSurfaceSystem && (Object) ctx.randomState instanceof RTFRandomState rtfRandomState) {
			return new Source(ctx, rtfRandomState.seed(this.selector.value()), rtfSurfaceSystem.getOrCreateStrata(this.name, this::generateStrata));
		} else {
			throw new IllegalStateException();
		}
	}

	@Override
	public KeyDispatchDataCodec<StrataRule> codec() {
		return new KeyDispatchDataCodec<>(CODEC);
	}

	private List<List<Layer>> generateStrata(RandomSource random) {
		List<List<Layer>> layers = new ArrayList<>();
		List<Layer> singleStrataSequence = new ArrayList<>();
		int seed = random.nextInt();

		for (BlockState state : HARDCODED_LAYERS) {
			float minDepth = 1.5F;
			float maxDepth = 4.0F;
			float depth = minDepth + random.nextFloat() * (maxDepth - minDepth);

			singleStrataSequence.add(new Layer(
					state,
					Noises.shiftSeed(Noises.mul(this.depthNoise.value(), depth), random.nextInt()),
					seed
			));
		}

		layers.add(singleStrataSequence);
		return layers;
	}

	public record Layer(BlockState material, Noise depth, int seed) {
		public float computeDepth(float x, float z) {
			return this.depth.compute(x, z, this.seed);
		}
	}

	private class Source implements SurfaceRules.SurfaceRule {
		private Context surfaceContext;
		private List<List<Layer>> strata;
		private List<Layer> layers;
		private float[] depthBuffer;
		private long lastUpdateXZ;
		private boolean isUnderwater;

		public Source(Context surfaceContext, Noise selector, List<List<Layer>> strata) {
			this.surfaceContext = surfaceContext;
			this.strata = strata;
			this.lastUpdateXZ = Long.MIN_VALUE;
		}

		@Nullable
		@Override
		public BlockState tryApply(int x, int y, int z) {
			if(this.lastUpdateXZ != this.surfaceContext.lastUpdateXZ) {
				this.initBuffer(x, z);
				this.lastUpdateXZ = this.surfaceContext.lastUpdateXZ;
			}

			// Safely skip ocean beds and river floors so vanilla sand/gravel can spawn
			if (this.isUnderwater) {
				return null;
			}

			for(int i = 0; i < this.layers.size(); i++) {
				Layer layer = this.layers.get(i);
				if(y > this.depthBuffer[i]) {
					return layer.material();
				}
			}

			return null;
		}

		private void initBuffer(int x, int z) {
			this.layers = this.selectLayers(x, z);
			int layerCount = this.layers.size();

			if (this.depthBuffer == null || this.depthBuffer.length < layerCount) {
				this.depthBuffer = new float[layerCount];
			}

			// TRICK: Because surface rules evaluate from top to bottom, the first
			// Y coordinate hit when the XZ column updates IS the true ground surface.
			int oceanFloor = this.surfaceContext.blockY;

			// Check the block directly above the surface stone. If it's water, we are in an ocean/river!
			int localX = this.surfaceContext.blockX & 0xF;
			int localZ = this.surfaceContext.blockZ & 0xF;
			BlockState blockAbove = this.surfaceContext.chunk.getBlockState(new BlockPos(localX, oceanFloor + 1, localZ));
			this.isUnderwater = blockAbove.is(Blocks.WATER) || !blockAbove.getFluidState().isEmpty();

			int currentY = oceanFloor;
			for(int i = 0; i < layerCount; i++) {
				Layer layer = this.layers.get(i);
				float thickness = Math.max(1.0F, layer.computeDepth(x, z));
				currentY -= Math.round(thickness);

				int jitter = getCoordJitter(x, z, i);
				float targetDepth = currentY + jitter;

				// Maintain the 1-block minimum thickness guarantee
				float maxAllowedDepth = (i == 0 ? oceanFloor : this.depthBuffer[i - 1]) - 1.0F;
				this.depthBuffer[i] = Math.min(targetDepth, maxAllowedDepth);
			}
		}

		private int getCoordJitter(int x, int z, int layerIndex) {
			long hash = (long) x * 3129871L ^ (long) z * 116129781L ^ (long) layerIndex * 999983L;
			hash = hash * hash * 42317861L + hash * 11L;
			return (int) (Math.abs(hash) % 5) - 2;
		}

		private List<Layer> selectLayers(int x, int z) {
			return this.strata.get(0);
		}
	}
}