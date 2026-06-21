package raccoonman.reterraforged.world.worldgen.cell.heightmap;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.concurrent.Resource;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.beach.BeachEvaluator;
import raccoonman.reterraforged.world.worldgen.cell.beach.BeachType;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.Tile;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.TileCache;

public class WorldLookup {
	private static final long FALLBACK_LOG_INTERVAL = 65536L;
	private static final AtomicLong FALLBACK_LOOKUPS = new AtomicLong();
	private static final AtomicLong FALLBACK_SAMPLES = new AtomicLong();
	private TileCache cache;
	private Heightmap heightmap;
	private Levels levels;
	private BeachEvaluator beachEvaluator;

	public WorldLookup(GeneratorContext context) {
		this.cache = context.cache;
		this.heightmap = context.generator.getHeightmap();
		this.levels = context.levels;
		ControlPoints controlPoints = ControlPoints.make(context.preset.world().controlPoints);
		this.beachEvaluator = new BeachEvaluator(context.levels, controlPoints, context.preset.world().beaches);
	}

	public Heightmap getHeightmap() {
		return this.heightmap;
	}

	public boolean applyCell(Cell cell, int x, int z, boolean applyClimate) {
		return this.applyCell(cell, x, z, false, applyClimate);
	}

	public boolean applyCell(Cell cell, int x, int z, boolean load, boolean applyClimate) {
		if (load && this.computeAccurate(cell, x, z)) {
			return true;
		}
		if (this.computeCached(cell, x, z)) {
			return true;
		}
		return this.compute(cell, x, z, applyClimate);
	}

	private boolean computeAccurate(Cell cell, int x, int z) {
		int rx = this.cache.chunkToTile(x >> 4);
		int rz = this.cache.chunkToTile(z >> 4);
		Tile tile = this.cache.provide(rx, rz);
		Cell c = tile.lookup(x, z);
		if (c != null) {
			cell.copyFrom(c);
		}
		return cell.terrain != null;
	}

	private boolean computeCached(Cell cell, int x, int z) {
		int rx = this.cache.chunkToTile(x >> 4);
		int rz = this.cache.chunkToTile(z >> 4);
		Tile tile = this.cache.provideIfPresent(rx, rz);
		if (tile != null) {
			Cell c = tile.lookup(x, z);
			if (c != null) {
				cell.copyFrom(c);
			}
			return cell.terrain != null;
		}
		return false;
	}

	private boolean compute(Cell cell, int x, int z, boolean applyClimate) {
		this.recordFallbackLookup();
		try (SamplingCache cache = new SamplingCache()) {
			this.sampleCell(cell, x, z, applyClimate, true, cache);
			if (cell.terrain.isCoast() && (cell.beachType == BeachType.NONE || cell.beachType == BeachType.OCEAN)) {
				try (SampledNeighborhood neighborhood = new SampledNeighborhood(cell, x, z, applyClimate, cache)) {
					this.beachEvaluator.applyContinuity(cell, neighborhood);
				}
			}
		}
		return false;
	}

	private void recordFallbackLookup() {
		long count = FALLBACK_LOOKUPS.incrementAndGet();
		if (count == 1L || count % FALLBACK_LOG_INTERVAL == 0L) {
			RTFCommon.LOGGER.info("WorldLookup fallback used {} times ({} sampled cells)", count, FALLBACK_SAMPLES.get());
		}
	}

	private void sampleCell(Cell cell, int x, int z, boolean applyClimate) {
		try (SamplingCache cache = new SamplingCache()) {
			this.sampleCell(cell, x, z, applyClimate, true, cache);
		}
	}

	private void sampleCell(Cell cell, int x, int z, boolean applyClimate, boolean resolveMaterial) {
		try (SamplingCache cache = new SamplingCache()) {
			this.sampleCell(cell, x, z, applyClimate, resolveMaterial, cache);
		}
	}

	private void sampleCell(Cell cell, int x, int z, boolean applyClimate, boolean resolveMaterial, SamplingCache cache) {
		this.heightmap.apply(cell, x, z, applyClimate);
		cell.gradient = this.computeGradient(cell, x, z, cache);
		Cell north = cache.sampleTerrain(x, z - 8);
		Cell south = cache.sampleTerrain(x, z + 8);
		Cell east = cache.sampleTerrain(x + 8, z);
		Cell west = cache.sampleTerrain(x - 8, z);
		this.beachEvaluator.evaluate(cell, north, south, east, west, resolveMaterial);
	}

	private float computeGradient(Cell center, int x, int z, SamplingCache cache) {
		float totalHeightDif = 0.0F;
		for (int dz = -1; dz <= 2; ++dz) {
			for (int dx = -1; dx <= 2; ++dx) {
				if (dx == 0 && dz == 0) {
					continue;
				}
				Cell neighbour = cache.sampleTerrain(x + dx, z + dz);
				float height = Math.max(neighbour.height, this.levels.water);
				totalHeightDif += Math.abs(center.height - height);
			}
		}
		return Math.min(1.0F, totalHeightDif * 10.0F);
	}

	private Cell sampleTerrain(Cell cell, int x, int z) {
		cell.reset();
		this.heightmap.apply(cell, x, z, false);
		return cell;
	}

	private final class SampledNeighborhood implements BeachEvaluator.Neighborhood, AutoCloseable {
		private final Cell[] cells = new Cell[9];
		private final BeachType[] types = new BeachType[9];
		private final float[] alphas = new float[9];
		@SuppressWarnings("unchecked")
		private final Resource<Cell>[] resources = new Resource[9];

		private SampledNeighborhood(Cell center, int centerX, int centerZ, boolean applyClimate, SamplingCache cache) {
			for (int dz = -1; dz <= 1; dz++) {
				for (int dx = -1; dx <= 1; dx++) {
					int index = this.index(dx, dz);
					if (dx == 0 && dz == 0) {
						this.cells[index] = center;
						this.types[index] = center.beachType;
						this.alphas[index] = center.beachSurfaceAlpha;
						continue;
					}
					Resource<Cell> resource = Cell.getResource();
					Cell cell = resource.get().reset();
					WorldLookup.this.sampleCell(cell, centerX + dx, centerZ + dz, applyClimate, false, cache);
					this.resources[index] = resource;
					this.cells[index] = cell;
					this.types[index] = cell.beachType;
					this.alphas[index] = cell.beachSurfaceAlpha;
				}
			}
		}

		@Override
		public Cell getCell(int dx, int dz) {
			return this.cells[this.index(dx, dz)];
		}

		@Override
		public BeachType getBeachType(int dx, int dz) {
			return this.types[this.index(dx, dz)];
		}

		@Override
		public float getBeachSurfaceAlpha(int dx, int dz) {
			return this.alphas[this.index(dx, dz)];
		}

		@Override
		public void close() {
			for (Resource<Cell> resource : this.resources) {
				if (resource != null) {
					resource.close();
				}
			}
		}

		private int index(int dx, int dz) {
			return (dz + 1) * 3 + (dx + 1);
		}
	}

	private final class SamplingCache implements AutoCloseable {
		private int[] xs = new int[32];
		private int[] zs = new int[32];
		private Cell[] cells = new Cell[32];
		@SuppressWarnings("unchecked")
		private Resource<Cell>[] resources = new Resource[32];
		private int size;

		private Cell sampleTerrain(int x, int z) {
			for (int i = 0; i < this.size; i++) {
				if (this.xs[i] == x && this.zs[i] == z) {
					return this.cells[i];
				}
			}
			this.ensureCapacity(this.size + 1);
			Resource<Cell> resource = Cell.getResource();
			Cell cell = WorldLookup.this.sampleTerrain(resource.get(), x, z);
			int index = this.size++;
			this.xs[index] = x;
			this.zs[index] = z;
			this.cells[index] = cell;
			this.resources[index] = resource;
			FALLBACK_SAMPLES.incrementAndGet();
			return cell;
		}

		@Override
		public void close() {
			for (int i = 0; i < this.size; i++) {
				Resource<Cell> resource = this.resources[i];
				if (resource != null) {
					resource.close();
					this.resources[i] = null;
				}
				this.cells[i] = null;
			}
			this.size = 0;
		}

		private void ensureCapacity(int targetSize) {
			if (targetSize <= this.cells.length) {
				return;
			}
			int capacity = Math.max(this.cells.length * 2, targetSize);
			this.xs = Arrays.copyOf(this.xs, capacity);
			this.zs = Arrays.copyOf(this.zs, capacity);
			this.cells = Arrays.copyOf(this.cells, capacity);
			this.resources = Arrays.copyOf(this.resources, capacity);
		}
	}
}
