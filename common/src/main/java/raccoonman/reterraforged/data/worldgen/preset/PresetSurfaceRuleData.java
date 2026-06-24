package raccoonman.reterraforged.data.worldgen.preset;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.data.worldgen.preset.settings.MiscellaneousSettings;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.tags.RTFBlockTags;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.surface.rule.RTFSurfaceRules;
import raccoonman.reterraforged.world.worldgen.surface.rule.StrataRule.Strata;
import raccoonman.reterraforged.world.worldgen.surface.rule.StrataRule.WeightedMaterial;

public class PresetSurfaceRuleData {

	private static final SurfaceRules.RuleSource AIR = block(Blocks.AIR);
	private static final SurfaceRules.RuleSource BEDROCK = block(Blocks.BEDROCK);
	private static final SurfaceRules.RuleSource WHITE_TERRACOTTA = block(Blocks.WHITE_TERRACOTTA);
	private static final SurfaceRules.RuleSource ORANGE_TERRACOTTA = block(Blocks.ORANGE_TERRACOTTA);
	private static final SurfaceRules.RuleSource TERRACOTTA = block(Blocks.TERRACOTTA);
	private static final SurfaceRules.RuleSource RED_SAND = block(Blocks.RED_SAND);
	private static final SurfaceRules.RuleSource RED_SANDSTONE = block(Blocks.RED_SANDSTONE);
	private static final SurfaceRules.RuleSource STONE = block(Blocks.STONE);
	private static final SurfaceRules.RuleSource DEEPSLATE = block(Blocks.DEEPSLATE);
	private static final SurfaceRules.RuleSource DIRT = block(Blocks.DIRT);
	private static final SurfaceRules.RuleSource PODZOL = block(Blocks.PODZOL);
	private static final SurfaceRules.RuleSource COARSE_DIRT = block(Blocks.COARSE_DIRT);
	private static final SurfaceRules.RuleSource MYCELIUM = block(Blocks.MYCELIUM);
	private static final SurfaceRules.RuleSource GRASS_BLOCK = block(Blocks.GRASS_BLOCK);
	private static final SurfaceRules.RuleSource CALCITE = block(Blocks.CALCITE);
	private static final SurfaceRules.RuleSource GRAVEL = block(Blocks.GRAVEL);
	private static final SurfaceRules.RuleSource SAND = block(Blocks.SAND);
	private static final SurfaceRules.RuleSource SANDSTONE = block(Blocks.SANDSTONE);
	private static final SurfaceRules.RuleSource PACKED_ICE = block(Blocks.PACKED_ICE);
	private static final SurfaceRules.RuleSource SNOW_BLOCK = block(Blocks.SNOW_BLOCK);
	private static final SurfaceRules.RuleSource MUD = block(Blocks.MUD);
	private static final SurfaceRules.RuleSource POWDER_SNOW = block(Blocks.POWDER_SNOW);
	private static final SurfaceRules.RuleSource ICE = block(Blocks.ICE);
	private static final SurfaceRules.RuleSource WATER = block(Blocks.WATER);

	public static SurfaceRules.RuleSource overworld(Preset preset, HolderGetter<DensityFunction> densityFunctions, HolderGetter<Noise> noise) {
		SurfaceRules.RuleSource deepslateRule = makeDeepslateRule(preset);

		if (preset.miscellaneous().strataDecorator) {
			SurfaceRules.RuleSource strataRule = makeStrataRule(preset, noise);
			return SurfaceRules.sequence(
				makeBedrockFloor(),
				SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(), makeBiomeSurfaces()),
				deepslateRule,
				strataRule
			);
		}
		return SurfaceRules.sequence(
			makeBedrockFloor(),
			SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(), makeBiomeSurfaces()),
			deepslateRule
		);
	}

	public static DeepslateBand computeDeepslateBand(Preset preset) {
		int worldDepth = Math.max(1, preset.world().properties.worldDepth);
		MiscellaneousSettings.StrataSettings strata = preset.miscellaneous().strata;
		float averageRockLayers = Math.max(1.0F, (strata.rockMinLayers + strata.rockMaxLayers) * 0.5F);
		int averageLayerDepth = Math.max(1, Math.round(worldDepth / averageRockLayers));
		int lowerRockLayers = Math.max(1, Math.round(averageRockLayers * 0.5F));
		int configuredFullDepth = averageLayerDepth * lowerRockLayers;
		int vanillaFullDepth = Math.min(worldDepth, 64);
		int fullDepth = Math.min(worldDepth, Math.max(vanillaFullDepth, configuredFullDepth));
		int transitionDepth = Math.max(8, averageLayerDepth);
		int worldBottom = -worldDepth;
		int fullY = worldBottom + fullDepth;
		int transitionTopY = Math.min(8, fullY + transitionDepth);
		return new DeepslateBand(worldDepth, fullY, transitionTopY, averageRockLayers, averageLayerDepth);
	}

	private static SurfaceRules.RuleSource makeBedrockFloor() {
		return SurfaceRules.ifTrue(
			SurfaceRules.verticalGradient("minecraft:bedrock_floor", VerticalAnchor.aboveBottom(0), VerticalAnchor.aboveBottom(5)),
			BEDROCK
		);
	}

	private static SurfaceRules.RuleSource makeDeepslateRule(Preset preset) {
		DeepslateBand band = computeDeepslateBand(preset);

		return SurfaceRules.ifTrue(
			SurfaceRules.verticalGradient("reterraforged:deepslate", VerticalAnchor.absolute(band.fullY()), VerticalAnchor.absolute(band.transitionTopY())),
			DEEPSLATE
		);
	}

	private static SurfaceRules.RuleSource makeBiomeSurfaces() {
		return SurfaceRules.sequence(
			makeFloorSpecialRules(),
			makeBadlandsRules(),
			makeMainSurfaceRules(),
			makeUnderwaterRules(),
			makeCaveFloorRules()
		);
	}

	private static SurfaceRules.RuleSource makeFloorSpecialRules() {
		SurfaceRules.RuleSource grassOrDirt = SurfaceRules.sequence(
			SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(0, 0), GRASS_BLOCK),
			DIRT
		);

		return SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
			SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.WOODED_BADLANDS),
				SurfaceRules.ifTrue(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(97), 2),
					surfaceNoiseBands(COARSE_DIRT, grassOrDirt)
				)
			),
			SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.SWAMP),
				SurfaceRules.ifTrue(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(62), 0),
					SurfaceRules.ifTrue(SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(63), 0)),
						SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SWAMP, 0.0, Double.MAX_VALUE), WATER)
					)
				)
			),
			SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.MANGROVE_SWAMP),
				SurfaceRules.ifTrue(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(60), 0),
					SurfaceRules.ifTrue(SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(63), 0)),
						SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SWAMP, 0.0, Double.MAX_VALUE), WATER)
					)
				)
			)
		));
	}

	private static SurfaceRules.RuleSource makeBadlandsRules() {
		return SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS), SurfaceRules.sequence(
			SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(256), 0), ORANGE_TERRACOTTA),
				SurfaceRules.ifTrue(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(74), 1),
					surfaceNoiseBands(TERRACOTTA, SurfaceRules.bandlands())
				),
				SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1, 0), SurfaceRules.sequence(
					SurfaceRules.ifTrue(SurfaceRules.ON_CEILING, RED_SANDSTONE),
					RED_SAND
				)),
				SurfaceRules.ifTrue(SurfaceRules.not(SurfaceRules.hole()), ORANGE_TERRACOTTA),
				SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-6, -1), WHITE_TERRACOTTA),
				SurfaceRules.sequence(
					SurfaceRules.ifTrue(SurfaceRules.ON_CEILING, STONE),
					GRAVEL
				)
			)),
			SurfaceRules.ifTrue(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(63), -1), SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(63), 0),
					SurfaceRules.ifTrue(SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(74), 1)), ORANGE_TERRACOTTA)
				),
				SurfaceRules.bandlands()
			)),
			SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR,
				SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-6, -1), WHITE_TERRACOTTA)
			)
		));
	}

	private static SurfaceRules.RuleSource makeMainSurfaceRules() {
		SurfaceRules.RuleSource grassOrDirt = SurfaceRules.sequence(
			SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(0, 0), GRASS_BLOCK),
			DIRT
		);
		SurfaceRules.RuleSource stoneOrGravel = SurfaceRules.sequence(
			SurfaceRules.ifTrue(SurfaceRules.ON_CEILING, STONE),
			GRAVEL
		);
		SurfaceRules.RuleSource sandstoneOrSand = SurfaceRules.sequence(
			SurfaceRules.ifTrue(SurfaceRules.ON_CEILING, SANDSTONE),
			SAND
		);

		return SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1, 0), SurfaceRules.sequence(
			SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN),
				SurfaceRules.ifTrue(SurfaceRules.hole(), SurfaceRules.sequence(
					SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(0, 0), AIR),
					SurfaceRules.ifTrue(SurfaceRules.temperature(), ICE),
					WATER
				))
			),
			SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.FROZEN_PEAKS), SurfaceRules.sequence(
					SurfaceRules.ifTrue(SurfaceRules.steep(), PACKED_ICE),
					SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.PACKED_ICE, 0.0, 0.2), PACKED_ICE),
					SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.ICE, 0.0, 0.025), ICE),
					SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(0, 0), SNOW_BLOCK)
				)),
				SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.SNOWY_SLOPES), SurfaceRules.sequence(
					SurfaceRules.ifTrue(SurfaceRules.steep(), STONE),
					SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.POWDER_SNOW, 0.35, 0.6),
						SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(0, 0), POWDER_SNOW)
					),
					SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(0, 0), SNOW_BLOCK)
				)),
				SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.JAGGED_PEAKS), SurfaceRules.sequence(
					SurfaceRules.ifTrue(SurfaceRules.steep(), STONE),
					SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(0, 0), SNOW_BLOCK)
				)),
				SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.GROVE), SurfaceRules.sequence(
					SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.POWDER_SNOW, 0.35, 0.6),
						SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(0, 0), POWDER_SNOW)
					),
					SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(0, 0), SNOW_BLOCK)
				)),
				SurfaceRules.sequence(
					SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.STONY_PEAKS), SurfaceRules.sequence(
						SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.CALCITE, -0.0125, 0.0125), CALCITE),
						STONE
					)),
					SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.STONY_SHORE), SurfaceRules.sequence(
						SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.GRAVEL, -0.05, 0.05), stoneOrGravel),
						STONE
					)),
					SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.WINDSWEPT_HILLS),
						SurfaceRules.ifTrue(surfaceNoiseAbove(1.0), STONE)
					),
					SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.WARM_OCEAN, Biomes.BEACH, Biomes.SNOWY_BEACH), sandstoneOrSand),
					SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.DESERT), sandstoneOrSand),
					SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.DRIPSTONE_CAVES), STONE)
				),
				SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.WINDSWEPT_SAVANNA), SurfaceRules.sequence(
					SurfaceRules.ifTrue(surfaceNoiseAbove(1.75), STONE),
					SurfaceRules.ifTrue(surfaceNoiseAbove(-0.5), COARSE_DIRT)
				)),
				SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.WINDSWEPT_GRAVELLY_HILLS), SurfaceRules.sequence(
					SurfaceRules.ifTrue(surfaceNoiseAbove(2.0), stoneOrGravel),
					SurfaceRules.ifTrue(surfaceNoiseAbove(1.0), STONE),
					SurfaceRules.ifTrue(surfaceNoiseAbove(-1.0), grassOrDirt),
					stoneOrGravel
				)),
				SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA), SurfaceRules.sequence(
					SurfaceRules.ifTrue(surfaceNoiseAbove(1.75), COARSE_DIRT),
					SurfaceRules.ifTrue(surfaceNoiseAbove(-0.95), PODZOL)
				)),
				SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.ICE_SPIKES),
					SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(0, 0), SNOW_BLOCK)
				),
				SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.MANGROVE_SWAMP), MUD),
				SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.MUSHROOM_FIELDS), MYCELIUM),
				grassOrDirt
			)
		)));
	}

	private static SurfaceRules.RuleSource makeUnderwaterRules() {
		SurfaceRules.RuleSource stoneOrGravel = SurfaceRules.sequence(
			SurfaceRules.ifTrue(SurfaceRules.ON_CEILING, STONE),
			GRAVEL
		);
		SurfaceRules.RuleSource sandstoneOrSand = SurfaceRules.sequence(
			SurfaceRules.ifTrue(SurfaceRules.ON_CEILING, SANDSTONE),
			SAND
		);

		return SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-6, -1), SurfaceRules.sequence(
			SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,
				SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN),
					SurfaceRules.ifTrue(SurfaceRules.hole(), WATER)
				)
			),
			SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.FROZEN_PEAKS), SurfaceRules.sequence(
					SurfaceRules.ifTrue(SurfaceRules.steep(), PACKED_ICE),
					SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.PACKED_ICE, -0.5, 0.2), PACKED_ICE),
					SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.ICE, -0.0625, 0.025), ICE),
					SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(0, 0), SNOW_BLOCK)
				)),
				SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.SNOWY_SLOPES), SurfaceRules.sequence(
					SurfaceRules.ifTrue(SurfaceRules.steep(), STONE),
					SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.POWDER_SNOW, 0.45, 0.58),
						SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(0, 0), POWDER_SNOW)
					),
					SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(0, 0), SNOW_BLOCK)
				)),
				SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.JAGGED_PEAKS), STONE),
				SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.GROVE), SurfaceRules.sequence(
					SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.POWDER_SNOW, 0.45, 0.58),
						SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(0, 0), POWDER_SNOW)
					),
					DIRT
				)),
				SurfaceRules.sequence(
					SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.STONY_PEAKS), SurfaceRules.sequence(
						SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.CALCITE, -0.0125, 0.0125), CALCITE),
						STONE
					)),
					SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.STONY_SHORE), SurfaceRules.sequence(
						SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.GRAVEL, -0.05, 0.05), stoneOrGravel),
						STONE
					)),
					SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.WINDSWEPT_HILLS),
						SurfaceRules.ifTrue(surfaceNoiseAbove(1.0), STONE)
					),
					SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.WARM_OCEAN, Biomes.BEACH, Biomes.SNOWY_BEACH), sandstoneOrSand),
					SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.DESERT), sandstoneOrSand),
					SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.DRIPSTONE_CAVES), STONE)
				),
				SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.WINDSWEPT_SAVANNA),
					SurfaceRules.ifTrue(surfaceNoiseAbove(1.75), STONE)
				),
				SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.WINDSWEPT_GRAVELLY_HILLS), SurfaceRules.sequence(
					SurfaceRules.ifTrue(surfaceNoiseAbove(2.0), stoneOrGravel),
					SurfaceRules.ifTrue(surfaceNoiseAbove(1.0), STONE),
					SurfaceRules.ifTrue(surfaceNoiseAbove(-1.0), DIRT),
					stoneOrGravel
				)),
				SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.MANGROVE_SWAMP), MUD),
				DIRT
			)),
			SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.WARM_OCEAN, Biomes.BEACH, Biomes.SNOWY_BEACH),
				SurfaceRules.ifTrue(SurfaceRules.stoneDepthCheck(0, true, 6, CaveSurface.FLOOR), SANDSTONE)
			),
			SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.DESERT),
				SurfaceRules.ifTrue(SurfaceRules.stoneDepthCheck(0, true, 30, CaveSurface.FLOOR), SANDSTONE)
			)
		));
	}

	private static SurfaceRules.RuleSource makeCaveFloorRules() {
		return SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
			SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.FROZEN_PEAKS, Biomes.JAGGED_PEAKS), STONE),
			SurfaceRules.ifTrue(SurfaceRules.isBiome(Biomes.WARM_OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN), SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.ON_CEILING, SANDSTONE),
				SAND
			)),
			SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.ON_CEILING, STONE),
				GRAVEL
			)
		));
	}

	private static SurfaceRules.RuleSource makeStrataRule(Preset preset, HolderGetter<Noise> noise) {
		Holder<Noise> depth = noise.getOrThrow(PresetStrataNoise.STRATA_DEPTH);
		MiscellaneousSettings.StrataSettings settings = preset.miscellaneous().strata;

		List<Strata> strata = new ArrayList<>();
		strata.add(new Strata(RTFBlockTags.SOIL, depth, 3, 0, 1, 0.1F, 0.25F));
		strata.add(new Strata(RTFBlockTags.SEDIMENT, depth, 3, 0, 2, 0.05F, 0.15F));
		strata.add(new Strata(RTFBlockTags.CLAY, depth, 3, 0, 2, 0.05F, 0.1F));
		strata.add(new Strata(null, List.of(
			new WeightedMaterial(Blocks.STONE, settings.stoneWeight),
			new WeightedMaterial(Blocks.GRANITE, settings.graniteWeight),
			new WeightedMaterial(Blocks.ANDESITE, settings.andesiteWeight),
			new WeightedMaterial(Blocks.DIORITE, settings.dioriteWeight)
		), depth, 3, settings.rockMinLayers, settings.rockMaxLayers, settings.rockMinDepth, settings.rockMaxDepth));
		return RTFSurfaceRules.strata(RTFCommon.location("overworld_strata"), noise.getOrThrow(PresetStrataNoise.STRATA_SELECTOR), strata, 100);
	}

	private static SurfaceRules.RuleSource surfaceNoiseBands(SurfaceRules.RuleSource material, SurfaceRules.RuleSource fallback) {
		return SurfaceRules.sequence(
			SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, -7.5 / 8.25, -4.5 / 8.25), material),
			SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, -1.5 / 8.25, 1.5 / 8.25), material),
			SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 4.5 / 8.25, 7.5 / 8.25), material),
			fallback
		);
	}

	private static SurfaceRules.ConditionSource surfaceNoiseAbove(double threshold) {
		return SurfaceRules.noiseCondition(Noises.SURFACE, threshold / 8.25, Double.MAX_VALUE);
	}

	private static SurfaceRules.RuleSource block(Block block) {
		return SurfaceRules.state(block.defaultBlockState());
	}

	public record DeepslateBand(int worldDepth, int fullY, int transitionTopY, float averageRockLayers, int averageLayerDepth) {
	}
}
