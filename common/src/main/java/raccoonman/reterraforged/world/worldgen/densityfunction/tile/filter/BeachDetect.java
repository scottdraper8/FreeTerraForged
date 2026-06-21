package raccoonman.reterraforged.world.worldgen.densityfunction.tile.filter;

import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.beach.BeachEvaluator;
import raccoonman.reterraforged.world.worldgen.cell.beach.BeachType;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.ControlPoints;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.Size;

public record BeachDetect(BeachEvaluator evaluator, ThreadLocal<SnapshotBuffers> buffers) implements Filter {

    @Override
    public void apply(Filterable map, int seedX, int seedZ, int iterations) {
        Size size = map.getBlockSize();
        int total = size.total();
        Cell[] backing = map.getBacking();

        for (int x = 0; x < total; x++) {
            for (int z = 0; z < total; z++) {
                Cell cell = map.getCellRaw(x, z);
                Cell n = map.getCellRaw(x, z - 8);
                Cell s = map.getCellRaw(x, z + 8);
                Cell e = map.getCellRaw(x + 8, z);
                Cell w = map.getCellRaw(x - 8, z);
                this.evaluator.evaluate(cell, n, s, e, w);
            }
        }

        SnapshotBuffers snapshotBuffers = this.buffers.get();
        snapshotBuffers.snapshot(backing);

        for (int x = 0; x < total; x++) {
            for (int z = 0; z < total; z++) {
                Cell cell = map.getCellRaw(x, z);
                SnapshotNeighborhood neighborhood = snapshotBuffers.neighborhood;
                neighborhood.init(x, z, total, snapshotBuffers.beachTypes, snapshotBuffers.beachAlphas);
                this.evaluator.applyContinuity(cell, neighborhood);
            }
        }
    }

    public static BeachDetect make(GeneratorContext ctx) {
        Levels levels = ctx.levels;
        ControlPoints controlPoints = ControlPoints.make(ctx.preset.world().controlPoints);
        WorldSettings.Beach beachSettings = ctx.preset.world().beaches;
        return new BeachDetect(
            new BeachEvaluator(levels, controlPoints, beachSettings),
            ThreadLocal.withInitial(SnapshotBuffers::new)
        );
    }

    private static class SnapshotBuffers {
        BeachType[] beachTypes = new BeachType[0];
        float[] beachAlphas = new float[0];
        final SnapshotNeighborhood neighborhood = new SnapshotNeighborhood();

        void snapshot(Cell[] backing) {
            if (this.beachTypes.length != backing.length) {
                this.beachTypes = new BeachType[backing.length];
                this.beachAlphas = new float[backing.length];
            }
            for (int i = 0; i < backing.length; i++) {
                this.beachTypes[i] = backing[i].beachType;
                this.beachAlphas[i] = backing[i].beachSurfaceAlpha;
            }
        }
    }

    private static class SnapshotNeighborhood implements BeachEvaluator.Neighborhood {
        private int cx, cz, total;
        private BeachType[] beachTypes;
        private float[] beachAlphas;

        void init(int cx, int cz, int total, BeachType[] beachTypes, float[] beachAlphas) {
            this.cx = cx;
            this.cz = cz;
            this.total = total;
            this.beachTypes = beachTypes;
            this.beachAlphas = beachAlphas;
        }

        @Override
        public Cell getCell(int dx, int dz) {
            return Cell.empty();
        }

        @Override
        public BeachType getBeachType(int dx, int dz) {
            int x = this.cx + dx;
            int z = this.cz + dz;
            if (x < 0 || x >= this.total || z < 0 || z >= this.total) {
                return BeachType.NONE;
            }
            return this.beachTypes[z * this.total + x];
        }

        @Override
        public float getBeachSurfaceAlpha(int dx, int dz) {
            int x = this.cx + dx;
            int z = this.cz + dz;
            if (x < 0 || x >= this.total || z < 0 || z >= this.total) {
                return 0.0F;
            }
            return this.beachAlphas[z * this.total + x];
        }
    }
}
