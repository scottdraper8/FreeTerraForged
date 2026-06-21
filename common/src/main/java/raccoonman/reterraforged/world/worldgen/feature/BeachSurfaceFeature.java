package raccoonman.reterraforged.world.worldgen.feature;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.beach.BeachMaterial;
import raccoonman.reterraforged.world.worldgen.cell.beach.BeachType;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.Tile;
import raccoonman.reterraforged.world.worldgen.feature.BeachSurfaceFeature.Config;

public class BeachSurfaceFeature extends Feature<Config> {
	private static final MaterialSet SAND_PALETTE = new MaterialSet(Blocks.SAND.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState());
	private static final MaterialSet GRAVEL_PALETTE = new MaterialSet(Blocks.GRAVEL.defaultBlockState(), Blocks.STONE.defaultBlockState());
	private static final MaterialSet STONE_PALETTE = new MaterialSet(Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState());
	private static final MaterialSet MUD_PALETTE = new MaterialSet(Blocks.MUD.defaultBlockState(), Blocks.DIRT.defaultBlockState());
	private static final MaterialSet RED_SAND_PALETTE = new MaterialSet(Blocks.RED_SAND.defaultBlockState(), Blocks.RED_SANDSTONE.defaultBlockState());

	public BeachSurfaceFeature(Codec<Config> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<Config> placeContext) {
		WorldGenLevel level = placeContext.level();
		RandomState randomState = level.getLevel().getChunkSource().randomState();
		@Nullable
		GeneratorContext generatorContext;
		if (!((Object) randomState instanceof RTFRandomState rtfRandomState) || (generatorContext = rtfRandomState.generatorContext()) == null) {
			throw new IllegalStateException();
		}

		ChunkPos chunkPos = new ChunkPos(placeContext.origin());
		ChunkAccess chunk = level.getChunk(chunkPos.x, chunkPos.z);
		@Nullable
		Tile.Chunk tileChunk = generatorContext.cache != null ? generatorContext.cache.provideAtChunk(chunkPos.x, chunkPos.z).getChunkReader(chunkPos.x, chunkPos.z) : null;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		Cell lookupCell = new Cell();
		Config config = placeContext.config();
		int seaLevel = placeContext.chunkGenerator().getSeaLevel();

		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int worldX = chunkPos.getBlockX(x);
				int worldZ = chunkPos.getBlockZ(z);
				Cell cell = tileChunk != null ? tileChunk.getCell(x, z) : lookupCell.reset();
				if (tileChunk == null) {
					generatorContext.lookup.applyCell(cell, worldX, worldZ, true);
				}
				if (!shouldPaint(cell)) {
					continue;
				}
				int surfaceY = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
				if (surfaceY < seaLevel - 2) {
					continue;
				}
				pos.set(worldX, surfaceY, worldZ);
				paintColumn(chunk, pos, surfaceY, getDepth(cell.beachType, config), cell.beachMaterial);
			}
		}
		return true;
	}

	private static boolean shouldPaint(Cell cell) {
		if (cell.beachType == BeachType.NONE) {
			return false;
		}
		if (cell.beachMaterial == BeachMaterial.NONE) {
			return false;
		}
		return cell.beachSurfaceAlpha > cell.beachSurfaceNoise;
	}

	private static int getDepth(BeachType type, Config config) {
		return switch (type) {
			case OCEAN -> config.oceanDepth();
			case RIVER -> config.riverDepth();
			case LAKE -> config.lakeDepth();
			default -> 0;
		};
	}

	private static void paintColumn(ChunkAccess chunk, BlockPos.MutableBlockPos pos, int surfaceY, int depth, BeachMaterial material) {
		if (depth <= 0) {
			return;
		}
		MaterialSet palette = MaterialSet.of(material);
		for (int dy = 0; dy < depth; dy++) {
			int y = surfaceY - dy;
			BlockState state = dy >= depth - 1 ? palette.filler() : palette.surface();
			ColumnDecorator.replaceSolid(chunk, pos.setY(y), state);
		}
	}

	public record Config(int oceanDepth, int riverDepth, int lakeDepth) implements FeatureConfiguration {
		public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("ocean_depth").forGetter(Config::oceanDepth),
			Codec.INT.fieldOf("river_depth").forGetter(Config::riverDepth),
			Codec.INT.fieldOf("lake_depth").forGetter(Config::lakeDepth)
		).apply(instance, Config::new));
	}

	private record MaterialSet(BlockState surface, BlockState filler) {
		private static MaterialSet of(BeachMaterial material) {
			return switch (material) {
				case GRAVEL -> GRAVEL_PALETTE;
				case STONE -> STONE_PALETTE;
				case MUD -> MUD_PALETTE;
				case RED_SAND -> RED_SAND_PALETTE;
				case SAND, NONE -> SAND_PALETTE;
			};
		}
	}
}
