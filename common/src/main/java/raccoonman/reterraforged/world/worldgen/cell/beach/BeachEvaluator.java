package raccoonman.reterraforged.world.worldgen.cell.beach;

import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.ControlPoints;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.cell.terrain.TerrainType;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;

public class BeachEvaluator {
	private static final int CLASSIFY_SAMPLE_DISTANCE = 8;
	private static final float CLASSIFY_GRADIENT_LIMIT = 0.275F;
	private static final float CONTINUITY_ORTHOGONAL_WEIGHT = 1.0F;
	private static final float CONTINUITY_DIAGONAL_WEIGHT = 0.7F;
	private static final float CONTINUITY_MAX_SUPPORT = CONTINUITY_ORTHOGONAL_WEIGHT * 4.0F + CONTINUITY_DIAGONAL_WEIGHT * 4.0F;
	private static final float CONTINUITY_BLEND_STRENGTH = 0.45F;
	private static final float CONTINUITY_PROMOTION_THRESHOLD = 2.4F;
	private static final float CONTINUITY_PROMOTION_MIN = 0.35F;
	private final ControlPoints controlPoints;
	private final Levels levels;
	private final WorldSettings.Beach settings;

	public BeachEvaluator(Levels levels, ControlPoints controlPoints, WorldSettings.Beach settings) {
		this.levels = levels;
		this.controlPoints = controlPoints;
		this.settings = settings;
	}

	public void evaluate(Cell cell, Cell north, Cell south, Cell east, Cell west) {
		this.evaluate(cell, north, south, east, west, true);
	}

	public void evaluate(Cell cell, Cell north, Cell south, Cell east, Cell west, boolean resolveMaterial) {
		float oceanAlpha = this.getOceanSurfaceAlpha(cell);
		float riverAlpha = this.getRiverSurfaceAlpha(cell);
		float lakeAlpha = this.getLakeSurfaceAlpha(cell);
		cell.beachType = BeachType.NONE;
		cell.beachMaterial = BeachMaterial.NONE;
		cell.beachSurfaceAlpha = 0.0F;

		if (oceanAlpha > 0.0F) {
			this.applyResolvedShore(cell, BeachType.OCEAN, oceanAlpha, resolveMaterial);
			if (this.isBeachBiomeCandidate(cell) && this.getClassificationGradient(north, south, east, west, cell) < CLASSIFY_GRADIENT_LIMIT) {
				cell.terrain = TerrainType.BEACH;
			}
			return;
		}

		if (riverAlpha >= lakeAlpha && riverAlpha > 0.0F) {
			this.applyResolvedShore(cell, BeachType.RIVER, riverAlpha, resolveMaterial);
			cell.terrain = TerrainType.RIVER_SHORE;
		} else if (lakeAlpha > 0.0F) {
			this.applyResolvedShore(cell, BeachType.LAKE, lakeAlpha, resolveMaterial);
			cell.terrain = TerrainType.LAKE_SHORE;
		}
	}

	public void applyContinuity(Cell cell, Neighborhood neighborhood) {
		if (cell.beachType == BeachType.RIVER || cell.beachType == BeachType.LAKE || !this.isContinuityCandidate(cell)) {
			return;
		}
		Support support = this.collectSupport(neighborhood);
		if (support.weight <= 0.0F) {
			return;
		}
		float averageAlpha = support.alpha / support.weight;
		float supportRatio = NoiseUtil.clamp(support.weight / CONTINUITY_MAX_SUPPORT, 0.0F, 1.0F);
		if (cell.beachType == BeachType.OCEAN) {
			float blendAlpha = supportRatio * CONTINUITY_BLEND_STRENGTH;
			float targetAlpha = Math.max(cell.beachSurfaceAlpha, averageAlpha);
			cell.beachSurfaceAlpha = NoiseUtil.lerp(cell.beachSurfaceAlpha, targetAlpha, blendAlpha);
			if (cell.beachMaterial == BeachMaterial.NONE) {
				cell.beachMaterial = this.selectMaterial(cell, BeachType.OCEAN, cell.beachSurfaceAlpha);
			}
			return;
		}
		if (cell.beachType != BeachType.NONE || support.weight < CONTINUITY_PROMOTION_THRESHOLD) {
			return;
		}
		float geometryAlpha = this.getContinuityGeometryAlpha(cell);
		if (geometryAlpha <= 0.0F) {
			return;
		}
		float candidateAlpha = NoiseUtil.clamp(this.settings.ocean.coverage * geometryAlpha, 0.0F, 1.0F);
		float promotionAlpha = NoiseUtil.lerp(support.weight, CONTINUITY_PROMOTION_THRESHOLD, CONTINUITY_MAX_SUPPORT, CONTINUITY_PROMOTION_MIN, 1.0F);
		promotionAlpha = NoiseUtil.clamp(promotionAlpha, CONTINUITY_PROMOTION_MIN, 1.0F);
		float promotedAlpha = Math.min(candidateAlpha, averageAlpha) * promotionAlpha;
		if (promotedAlpha > 0.0F) {
			this.applyResolvedShore(cell, BeachType.OCEAN, promotedAlpha);
			cell.oceanShoreAlpha = Math.max(cell.oceanShoreAlpha, geometryAlpha * promotionAlpha);
			cell.terrain = TerrainType.BEACH;
		}
	}

	private void applyResolvedShore(Cell cell, BeachType type, float alpha) {
		this.applyResolvedShore(cell, type, alpha, true);
	}

	private void applyResolvedShore(Cell cell, BeachType type, float alpha, boolean resolveMaterial) {
		cell.beachType = type;
		cell.beachSurfaceAlpha = alpha;
		cell.beachMaterial = resolveMaterial ? this.selectMaterial(cell, type, alpha) : BeachMaterial.NONE;
	}

	private boolean isOceanBeach(Cell cell) {
		if (!this.isOceanEnvelope(cell)) {
			return false;
		}
		return this.inHeightRange(cell, this.settings.ocean) && this.inSlopeRange(cell, this.settings.ocean.maxSlope);
	}

	private float getOceanSurfaceAlpha(Cell cell) {
		if (!this.isOceanBeach(cell)) {
			cell.oceanShoreDistance = 0.0F;
			cell.oceanShoreAlpha = 0.0F;
			return 0.0F;
		}
		float geometryAlpha = this.getOceanGeometryAlpha(cell, this.settings.ocean.geometry.coastBandScale, this.settings.ocean.geometry.transitionBias);
		cell.oceanShoreAlpha = geometryAlpha;
		return NoiseUtil.clamp(this.settings.ocean.coverage * geometryAlpha, 0.0F, 1.0F);
	}

	private float getContinuityGeometryAlpha(Cell cell) {
		if (!this.isContinuityCandidate(cell)) {
			return 0.0F;
		}
		float coastBandScale = this.settings.ocean.geometry.coastBandScale + this.settings.ocean.geometry.continuityPadding;
		return this.getOceanGeometryAlpha(cell, coastBandScale, this.settings.ocean.geometry.transitionBias);
	}

	private boolean isContinuityCandidate(Cell cell) {
		if (!this.isOceanEnvelope(cell)) {
			return false;
		}
		if (cell.continentEdge >= this.controlPoints.coastMarker()) {
			return false;
		}
		return this.inHeightRange(cell, this.settings.ocean) && this.inSlopeRange(cell, this.settings.ocean.maxSlope);
	}

	private float getRiverSurfaceAlpha(Cell cell) {
		if (!cell.terrain.isOverground() || cell.terrain.isCoast() || cell.terrain.isInlandShore()) {
			return 0.0F;
		}
		if (cell.terrain.isWetland() || cell.terrain.isLake() || cell.terrain.isRiver()) {
			return 0.0F;
		}
		if (cell.riverBankAlpha <= 0.0F) {
			return 0.0F;
		}

		WorldSettings.River river = this.settings.river;
		if (!this.inRiverHeightRange(cell, river) || !this.inSlopeRange(cell, river.maxSlope)) {
			return 0.0F;
		}
		if (cell.riverWidth < river.minWidth || cell.riverWidth > river.maxWidth || cell.riverBankHeight < river.minBankHeight || cell.riverBankHeight > river.maxBankHeight) {
			return 0.0F;
		}

		float depthAlpha = 1.0F - NoiseUtil.clamp(NoiseUtil.map(cell.riverDepth, 0.0F, 1.0F, 0.0F, river.maxDepth), 0.0F, 1.0F);
		return NoiseUtil.clamp(river.coverage * cell.riverShoreAlpha * depthAlpha, 0.0F, 1.0F);
	}

	private float getLakeSurfaceAlpha(Cell cell) {
		if (!cell.terrain.isOverground() || cell.terrain.isCoast() || cell.terrain.isInlandShore()) {
			return 0.0F;
		}
		if (cell.terrain.isWetland() || cell.terrain.isLake() || cell.terrain.isRiver()) {
			return 0.0F;
		}
		if (cell.lakeBankAlpha <= 0.0F) {
			return 0.0F;
		}

		WorldSettings.Lake lake = this.settings.lake;
		if (!this.inLakeHeightRange(cell, lake) || !this.inSlopeRange(cell, lake.maxSlope)) {
			return 0.0F;
		}
		if (cell.lakeDepth > lake.maxDepth || cell.lakeBankHeight < lake.minBankHeight || cell.lakeBankHeight > lake.maxBankHeight) {
			return 0.0F;
		}

		float depthAlpha = 1.0F - NoiseUtil.clamp(NoiseUtil.map(cell.lakeDepth, 0.0F, 1.0F, 0.0F, lake.maxDepth), 0.0F, 1.0F);
		return NoiseUtil.clamp(lake.coverage * cell.lakeShoreAlpha * depthAlpha, 0.0F, 1.0F);
	}

	private boolean isBeachBiomeCandidate(Cell cell) {
		return cell.continentEdge < this.controlPoints.beach();
	}

	private float getOceanGeometryAlpha(Cell cell, float coastBandScale, float transitionBias) {
		float normalizedDistance = this.getOceanNormalizedDistance(cell, coastBandScale);
		float edgeAlpha = 1.0F - normalizedDistance;
		return ShoreGeometry.applyBias(edgeAlpha, transitionBias);
	}

	private float getOceanNormalizedDistance(Cell cell, float coastBandScale) {
		float normalizedDistance = ShoreGeometry.getOceanNormalizedDistance(cell.continentEdge, this.controlPoints, coastBandScale);
		cell.oceanShoreDistance = normalizedDistance;
		return normalizedDistance;
	}

	private boolean inHeightRange(Cell cell, WorldSettings.Ocean settings) {
		float minHeight = this.levels.water(settings.minHeight);
		float maxHeight = this.levels.water(settings.maxHeight);
		return cell.height >= minHeight && cell.height <= maxHeight;
	}

	private boolean inRiverHeightRange(Cell cell, WorldSettings.River settings) {
		float localBase = resolveLocalWaterBase(cell);
		float minHeight = localBase + settings.minHeight * this.levels.unit;
		float maxHeight = localBase + settings.maxHeight * this.levels.unit;
		return cell.height >= minHeight && cell.height <= maxHeight;
	}

	private boolean inLakeHeightRange(Cell cell, WorldSettings.Lake settings) {
		float localBase = resolveLocalWaterBase(cell);
		float minHeight = localBase + settings.minHeight * this.levels.unit;
		float maxHeight = localBase + settings.maxHeight * this.levels.unit;
		return cell.height >= minHeight && cell.height <= maxHeight;
	}

	private float resolveLocalWaterBase(Cell cell) {
		return (cell.riverWaterLevel > 0.0F) ? this.levels.water + cell.riverWaterLevel : this.levels.water;
	}

	private boolean inSlopeRange(Cell cell, float maxSlope) {
		return cell.gradient <= maxSlope;
	}

	private BeachMaterial selectMaterial(Cell cell, BeachType type, float surfaceAlpha) {
		if (type == BeachType.NONE || surfaceAlpha <= 0.0F) {
			return BeachMaterial.NONE;
		}

		WorldSettings.MaterialPalette palette = this.getPalette(type);
		float sandWeight = palette.sand;
		float gravelWeight = palette.gravel;
		float stoneWeight = palette.stone;
		float mudWeight = palette.mud;
		float redSandWeight = palette.redSand;
		float varianceStrength = NoiseUtil.clamp(this.settings.variance.noiseStrength, 0.0F, 1.0F);
		float sediment = NoiseUtil.clamp(cell.sediment, 0.0F, 1.0F);
		float arid = NoiseUtil.clamp(cell.regionTemperature * (1.0F - cell.regionMoisture), 0.0F, 1.0F);
		float humid = NoiseUtil.clamp(cell.regionMoisture, 0.0F, 1.0F);
		float cold = NoiseUtil.clamp(1.0F - cell.regionTemperature, 0.0F, 1.0F);
		float energy = this.getEnergy(cell, type);
		float deposition = 1.0F - energy;

		float processSandWeight = 0.0F;
		float processGravelWeight = 0.0F;
		float processStoneWeight = 0.0F;
		float processMudWeight = 0.0F;
		float processRedSandWeight = 0.0F;
		switch (type) {
			case OCEAN -> {
				processSandWeight = deposition * (0.75F + 0.25F * sediment);
				processGravelWeight = energy * (0.50F + 0.25F * cold);
				processStoneWeight = energy * (0.45F + 0.35F * (1.0F - sediment));
				processMudWeight = deposition * humid * (0.35F + 0.25F * (1.0F - sediment));
				processRedSandWeight = deposition * arid * 0.40F;
			}
			case RIVER -> {
				processSandWeight = deposition * (0.35F + 0.35F * sediment);
				processGravelWeight = energy * (0.75F + 0.15F * cold);
				processStoneWeight = energy * (0.35F + 0.25F * (1.0F - sediment));
				processMudWeight = deposition * humid * (0.60F + 0.20F * (1.0F - sediment));
				processRedSandWeight = deposition * arid * 0.18F;
			}
			case LAKE -> {
				processSandWeight = deposition * (0.55F + 0.25F * sediment);
				processGravelWeight = energy * (0.55F + 0.20F * cold);
				processStoneWeight = energy * (0.30F + 0.25F * (1.0F - sediment));
				processMudWeight = deposition * humid * (0.50F + 0.20F * (1.0F - sediment));
				processRedSandWeight = deposition * arid * 0.20F;
			}
			case NONE -> {
				processSandWeight = 1.0F;
			}
		}

		sandWeight = NoiseUtil.lerp(sandWeight, processSandWeight, varianceStrength);
		gravelWeight = NoiseUtil.lerp(gravelWeight, processGravelWeight, varianceStrength);
		stoneWeight = NoiseUtil.lerp(stoneWeight, processStoneWeight, varianceStrength);
		mudWeight = NoiseUtil.lerp(mudWeight, processMudWeight, varianceStrength);
		redSandWeight = NoiseUtil.lerp(redSandWeight, processRedSandWeight, varianceStrength);

		float climateBias = NoiseUtil.clamp(this.settings.variance.climateBias, 0.0F, 1.0F);
		sandWeight += climateBias * (0.08F * arid);
		gravelWeight += climateBias * (0.10F * cold + 0.04F * humid);
		stoneWeight += climateBias * (0.08F * cold + 0.05F * (1.0F - sediment));
		mudWeight += climateBias * (0.18F * humid);
		redSandWeight += climateBias * (0.30F * arid);

		sandWeight = Math.max(0.0F, sandWeight);
		gravelWeight = Math.max(0.0F, gravelWeight);
		stoneWeight = Math.max(0.0F, stoneWeight);
		mudWeight = Math.max(0.0F, mudWeight);
		redSandWeight = Math.max(0.0F, redSandWeight);

		float totalWeight = sandWeight + gravelWeight + stoneWeight + mudWeight + redSandWeight;
		if (totalWeight <= 0.0F) {
			switch (type) {
				case OCEAN -> {
					sandWeight = 0.70F;
					gravelWeight = 0.15F;
					stoneWeight = 0.10F;
					mudWeight = 0.02F;
					redSandWeight = 0.03F;
				}
				case RIVER -> {
					sandWeight = 0.20F;
					gravelWeight = 0.40F;
					stoneWeight = 0.20F;
					mudWeight = 0.18F;
					redSandWeight = 0.02F;
				}
				case LAKE -> {
					sandWeight = 0.35F;
					gravelWeight = 0.30F;
					stoneWeight = 0.15F;
					mudWeight = 0.18F;
					redSandWeight = 0.02F;
				}
				case NONE -> {
					sandWeight = 1.0F;
				}
			}
			totalWeight = sandWeight + gravelWeight + stoneWeight + mudWeight + redSandWeight;
		}

		float target = NoiseUtil.clamp(cell.beachMaterialNoise, 0.0F, 1.0F) * totalWeight;
		float cumulative = sandWeight;
		if (target <= cumulative) {
			return BeachMaterial.SAND;
		}
		cumulative += gravelWeight;
		if (target <= cumulative) {
			return BeachMaterial.GRAVEL;
		}
		cumulative += stoneWeight;
		if (target <= cumulative) {
			return BeachMaterial.STONE;
		}
		cumulative += mudWeight;
		if (target <= cumulative) {
			return BeachMaterial.MUD;
		}
		return BeachMaterial.RED_SAND;
	}

	private WorldSettings.MaterialPalette getPalette(BeachType type) {
		return switch (type) {
			case OCEAN -> this.settings.ocean.materials;
			case RIVER -> this.settings.river.materials;
			case LAKE -> this.settings.lake.materials;
			default -> new WorldSettings.MaterialPalette(1.0F, 0.0F, 0.0F, 0.0F, 0.0F);
		};
	}

	private float getEnergy(Cell cell, BeachType type) {
		return switch (type) {
			case OCEAN -> {
				float coastAlpha = 1.0F - NoiseUtil.clamp(cell.oceanShoreDistance, 0.0F, 1.0F);
				yield NoiseUtil.clamp(0.55F * cell.gradient + 0.45F * coastAlpha, 0.0F, 1.0F);
			}
			case RIVER -> {
				float width = NoiseUtil.clamp(NoiseUtil.map(cell.riverWidth, 0.0F, 1.0F, this.settings.river.minWidth, this.settings.river.maxWidth), 0.0F, 1.0F);
				float depth = NoiseUtil.clamp(NoiseUtil.map(cell.riverDepth, 0.0F, 1.0F, 0.0F, this.settings.river.maxDepth), 0.0F, 1.0F);
				yield NoiseUtil.clamp(0.40F * cell.gradient + 0.35F * width + 0.25F * depth, 0.0F, 1.0F);
			}
			case LAKE -> {
				float depth = NoiseUtil.clamp(NoiseUtil.map(cell.lakeDepth, 0.0F, 1.0F, 0.0F, this.settings.lake.maxDepth), 0.0F, 1.0F);
				yield NoiseUtil.clamp(0.55F * cell.gradient + 0.25F * cell.lakeBankAlpha + 0.20F * depth, 0.0F, 1.0F);
			}
			default -> 0.0F;
		};
	}

	private float getClassificationGradient(Cell north, Cell south, Cell east, Cell west, Cell def) {
		float gx = this.grad(east, west, def);
		float gz = this.grad(north, south, def);
		return gx * gx + gz * gz;
	}

	private float grad(Cell a, Cell b, Cell def) {
		int distance = CLASSIFY_SAMPLE_DISTANCE * 2 + 1;
		if (a.isAbsent()) {
			a = def;
			distance -= CLASSIFY_SAMPLE_DISTANCE;
		}
		if (b.isAbsent()) {
			b = def;
			distance -= CLASSIFY_SAMPLE_DISTANCE;
		}
		return (a.height - b.height) / distance;
	}

	private Support collectSupport(Neighborhood neighborhood) {
		float totalWeight = 0.0F;
		float totalAlpha = 0.0F;
		for (int dz = -1; dz <= 1; dz++) {
			for (int dx = -1; dx <= 1; dx++) {
				if (dx == 0 && dz == 0) {
					continue;
				}
				Cell neighbour = neighborhood.getCell(dx, dz);
				if (neighbour.isAbsent() || neighborhood.getBeachType(dx, dz) != BeachType.OCEAN) {
					continue;
				}
				float weight = dx == 0 || dz == 0 ? CONTINUITY_ORTHOGONAL_WEIGHT : CONTINUITY_DIAGONAL_WEIGHT;
				totalWeight += weight;
				totalAlpha += neighborhood.getBeachSurfaceAlpha(dx, dz) * weight;
			}
		}
		return new Support(totalWeight, totalAlpha);
	}

	private record Support(float weight, float alpha) {
	}

	private boolean isOceanEnvelope(Cell cell) {
		if (cell.terrain.isInlandShore() || cell.terrain.isWetland() || cell.terrain.isRiver() || cell.terrain.isLake()) {
			return false;
		}
		if (cell.terrain.isCoast()) {
			return true;
		}
		return cell.terrain.isOverground() && cell.continentEdge <= this.controlPoints.coastMarker() && cell.oceanShoreAlpha > 0.0F;
	}

	public interface Neighborhood {
		Cell getCell(int dx, int dz);

		BeachType getBeachType(int dx, int dz);

		float getBeachSurfaceAlpha(int dx, int dz);
	}
}
