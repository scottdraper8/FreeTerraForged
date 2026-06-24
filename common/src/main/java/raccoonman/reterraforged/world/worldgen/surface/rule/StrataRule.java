package raccoonman.reterraforged.world.worldgen.surface.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceRules.Context;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.noise.module.Noises;
import raccoonman.reterraforged.world.worldgen.surface.RTFSurfaceSystem;

public record StrataRule(ResourceLocation name, Holder<Noise> selector, List<Strata> strata, int iterations) implements SurfaceRules.RuleSource {
	public static final MapCodec<StrataRule> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			ResourceLocation.CODEC.fieldOf("name").forGetter(StrataRule::name),
			Noise.CODEC.fieldOf("selector").forGetter(StrataRule::selector),
			Strata.CODEC.listOf().fieldOf("strata").forGetter(StrataRule::strata),
			Codec.INT.fieldOf("iterations").forGetter(StrataRule::iterations)
	).apply(instance, StrataRule::new));

	public StrataRule {
		strata = ImmutableList.copyOf(strata);
	}

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
		for(int i = 0; i < this.iterations; i++) {
			List<Layer> layer = new ArrayList<>();
			for(Strata strata : this.strata) {
				layer.addAll(strata.generateLayers(random));
			}
			layers.add(layer);
		}
		return layers;
	}

	public record Strata(@Nullable TagKey<Block> materials, List<WeightedMaterial> weightedMaterials, Holder<Noise> noise, int attempts, int minLayers, int maxLayers, float minDepth, float maxDepth) {
		public static final Codec<Strata> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				TagKey.hashedCodec(Registries.BLOCK).optionalFieldOf("materials").forGetter((s) -> Optional.ofNullable(s.materials)),
				WeightedMaterial.CODEC.listOf().optionalFieldOf("weighted_materials", List.of()).forGetter(Strata::weightedMaterials),
				Noise.CODEC.fieldOf("noise").forGetter(Strata::noise),
				Codec.INT.fieldOf("attempts").forGetter(Strata::attempts),
				Codec.INT.fieldOf("min_layers").forGetter(Strata::minLayers),
				Codec.INT.fieldOf("max_layers").forGetter(Strata::maxLayers),
				Codec.FLOAT.fieldOf("min_depth").forGetter(Strata::minDepth),
				Codec.FLOAT.fieldOf("max_depth").forGetter(Strata::maxDepth)
		).apply(instance, (materials, weightedMaterials, noise, attempts, minLayers, maxLayers, minDepth, maxDepth) -> new Strata(materials.orElse(null), weightedMaterials, noise, attempts, minLayers, maxLayers, minDepth, maxDepth)));
		
		public Strata {
			weightedMaterials = ImmutableList.copyOf(weightedMaterials);
		}
		
		public Strata(TagKey<Block> materials, Holder<Noise> noise, int attempts, int minLayers, int maxLayers, float minDepth, float maxDepth) {
			this(materials, List.of(), noise, attempts, minLayers, maxLayers, minDepth, maxDepth);
		}

		public List<Layer> generateLayers(RandomSource random) {
			int lastIndex = -1;
			int minLayers = Math.max(0, this.minLayers);
			int maxLayers = Math.max(minLayers, this.maxLayers);
			float minDepth = Math.max(0.0F, Math.min(this.minDepth, this.maxDepth));
			float maxDepth = Math.max(minDepth, Math.max(this.minDepth, this.maxDepth));
			int layers = minLayers + NoiseUtil.round(random.nextFloat() * (maxLayers - minLayers));
			List<Layer> result = new ArrayList<>();
			List<WeightedMaterial> materials = this.getMaterials();
			if (materials.isEmpty()) {
				return result;
			}
			float totalWeight = totalWeight(materials);
			if (totalWeight <= 0.0F) {
				return result;
			}

			int seed = random.nextInt();
			for (int i = 0; i < layers; i++) {
				int attempts = this.attempts;
				int index = selectIndex(materials, totalWeight, random);
				while (--attempts >= 0 && index == lastIndex) {
					index = selectIndex(materials, totalWeight, random);
				}
				if (index != lastIndex) {
					lastIndex = index;
					BlockState material = materials.get(index).material().defaultBlockState();
					float depth = minDepth + random.nextFloat() * (maxDepth - minDepth);
					result.add(new Layer(material, Noises.shiftSeed(Noises.mul(this.noise.value(), depth), random.nextInt()), seed));
				}
			}
			return result;
		}
		
		private List<WeightedMaterial> getMaterials() {
			if (!this.weightedMaterials.isEmpty()) {
				return this.weightedMaterials;
			}
			if (this.materials == null) {
				return List.of();
			}
			List<WeightedMaterial> materials = new ArrayList<>();
			for (Holder<Block> material : BuiltInRegistries.BLOCK.getTagOrEmpty(this.materials)) {
				materials.add(new WeightedMaterial(material.value(), 1.0F));
			}
			return materials;
		}
		
		private static int selectIndex(List<WeightedMaterial> materials, float totalWeight, RandomSource random) {
			float value = random.nextFloat() * totalWeight;
			float sum = 0.0F;
			for (int i = 0; i < materials.size(); i++) {
				float weight = Math.max(0.0F, materials.get(i).weight());
				if (weight <= 0.0F) {
					continue;
				}
				sum += weight;
				if (value <= sum) {
					return i;
				}
			}
			for (int i = materials.size() - 1; i >= 0; i--) {
				if (materials.get(i).weight() > 0.0F) {
					return i;
				}
			}
			return 0;
		}
		
		private static float totalWeight(List<WeightedMaterial> materials) {
			float total = 0.0F;
			for (WeightedMaterial material : materials) {
				total += Math.max(0.0F, material.weight());
			}
			return total;
		}
	}
	
	public record WeightedMaterial(Block material, float weight) {
		public static final Codec<WeightedMaterial> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				BuiltInRegistries.BLOCK.byNameCodec().fieldOf("material").forGetter(WeightedMaterial::material),
				Codec.FLOAT.fieldOf("weight").forGetter(WeightedMaterial::weight)
		).apply(instance, WeightedMaterial::new));
	}

	public record Layer(BlockState material, Noise depth, int seed) {
		public float computeDepth(float x, float z) {
			return this.depth.compute(x, z, this.seed);
		}
	}

	private class Source implements SurfaceRules.SurfaceRule {
		private Context surfaceContext;
		private Noise selector;
		private List<List<Layer>> strata;
		private List<Layer> layers;
		private float[] depthBuffer;
		private long lastUpdateXZ;

		public Source(Context surfaceContext, Noise selector, List<List<Layer>> strata) {
			this.surfaceContext = surfaceContext;
			this.selector = selector;
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

			Layer last = null;
			for(int i = 0; i < this.layers.size(); i++) {
				Layer layer = last = this.layers.get(i);
				if(y > this.depthBuffer[i]) {
					return layer.material();
				}
			}

			return last != null ? last.material() : null;
		}

		private void initBuffer(int x, int z) {
			this.layers = this.selectLayers(x, z);
			int layerCount = this.layers.size();

			if (this.depthBuffer == null || this.depthBuffer.length < layerCount) {
				this.depthBuffer = new float[layerCount];
			}

			int localX = this.surfaceContext.blockX & 0xF;
			int localZ = this.surfaceContext.blockZ & 0xF;
			int height = this.surfaceContext.chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, localX, localZ);
			int minY = this.surfaceContext.chunk.getMinBuildHeight();
			int totalDepth = Math.max(1, height - minY);

			float sum = 0.0F;
			for(int i = 0; i < layerCount; i++) {
				Layer layer = this.layers.get(i);
				float depth = layer.computeDepth(x, z);
				sum += depth;
				this.depthBuffer[i] = depth;
			}

			int y = height;
			for(int i = 0; i < layerCount; i++) {
				this.depthBuffer[i] = y -= Math.round((this.depthBuffer[i] / sum) * totalDepth);
			}
		}

		private List<Layer> selectLayers(int x, int z) {
			float selector = this.selector.compute(x, z, 0);
			int index = (int) (selector * this.strata.size());
			index = Math.min(this.strata.size() - 1, index);
			return this.strata.get(index);
		}
	}
}
