package raccoonman.reterraforged.data.worldgen.preset.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import raccoonman.reterraforged.world.worldgen.noise.function.DistanceFunction;

import java.util.Optional;

public class WorldSettings {
	public static final Codec<WorldSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Continent.CODEC.fieldOf("continent").forGetter((o) -> o.continent),
		ControlPoints.CODEC.fieldOf("controlPoints").forGetter((o) -> o.controlPoints),
		Properties.CODEC.fieldOf("properties").forGetter((o) -> o.properties),
		Beach.CODEC.optionalFieldOf("beaches", Beach.DEFAULT).forGetter((o) -> o.beaches)
	).apply(instance, WorldSettings::new));

    public Continent continent;
    public ControlPoints controlPoints;
    public Properties properties;
    public Beach beaches;

    public WorldSettings(Continent continent, ControlPoints controlPoints, Properties properties) {
        this(continent, controlPoints, properties, Beach.DEFAULT.copy());
    }

    public WorldSettings(Continent continent, ControlPoints controlPoints, Properties properties, Beach beaches) {
        this.continent = continent;
        this.controlPoints = controlPoints;
        this.properties = properties;
        this.beaches = beaches;
    }

    public WorldSettings copy() {
    	return new WorldSettings(this.continent.copy(), this.controlPoints.copy(), this.properties.copy(), this.beaches.copy());
    }
    
    public static class Continent {
    	public static final Codec<Continent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
    		ContinentType.CODEC.fieldOf("continentType").forGetter((o) -> o.continentType),
    		DistanceFunction.CODEC.optionalFieldOf("continentShape", DistanceFunction.EUCLIDEAN).forGetter((o) -> o.continentShape),
    		Codec.INT.fieldOf("continentScale").forGetter((o) -> o.continentScale),
    		Codec.FLOAT.fieldOf("continentJitter").forGetter((o) -> o.continentJitter),
    		Codec.FLOAT.optionalFieldOf("continentSkipping", 0.25F).forGetter((o) -> o.continentSkipping),
    		Codec.FLOAT.optionalFieldOf("continentSizeVariance", 0.25F).forGetter((o) -> o.continentSizeVariance),
    		Codec.INT.optionalFieldOf("continentNoiseOctaves", 5).forGetter((o) -> o.continentNoiseOctaves),
    		Codec.FLOAT.optionalFieldOf("continentNoiseGain", 0.26F).forGetter((o) -> o.continentNoiseGain),
    		Codec.FLOAT.optionalFieldOf("continentNoiseLacunarity", 4.33F).forGetter((o) -> o.continentNoiseLacunarity)
    	).apply(instance, Continent::new));
    	
        public ContinentType continentType;
        public DistanceFunction continentShape;
        public int continentScale;
        public float continentJitter;
        public float continentSkipping;
        public float continentSizeVariance;
        public int continentNoiseOctaves;
        public float continentNoiseGain;
        public float continentNoiseLacunarity;
        
        public Continent(ContinentType continentType, DistanceFunction continentShape, int continentScale, float continentJitter, float continentSkipping, float continentSizeVariance, int continentNoiseOctaves, float continentNoiseGain, float continentNoiseLacunarity) {
            this.continentType = continentType;
            this.continentShape = continentShape;
            this.continentScale = continentScale;
            this.continentJitter = continentJitter;
            this.continentSkipping = continentSkipping;
            this.continentSizeVariance = continentSizeVariance;
            this.continentNoiseOctaves = continentNoiseOctaves;
            this.continentNoiseGain = continentNoiseGain;
            this.continentNoiseLacunarity = continentNoiseLacunarity;
        }
        
        public Continent copy() {
        	return new Continent(this.continentType, this.continentShape, this.continentScale, this.continentJitter, this.continentSkipping, this.continentSizeVariance, this.continentNoiseOctaves, this.continentNoiseGain, this.continentNoiseLacunarity);
        }
    }
    
    public static class ControlPoints {
    	public static final Codec<ControlPoints> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("islandInland").xmap(opt -> opt.orElse(0.0F), Optional::of).forGetter((o) -> o.islandInland),
            Codec.FLOAT.optionalFieldOf("islandCoast").xmap(opt -> opt.orElse(0.074F), Optional::of).forGetter((o) -> o.islandCoast),
    		Codec.FLOAT.fieldOf("deepOcean").forGetter((o) -> o.deepOcean),
    		Codec.FLOAT.fieldOf("shallowOcean").forGetter((o) -> o.shallowOcean),
    		Codec.FLOAT.fieldOf("beach").forGetter((o) -> o.beach),
    		Codec.FLOAT.fieldOf("coast").forGetter((o) -> o.coast),
    		Codec.FLOAT.fieldOf("inland").forGetter((o) -> o.inland)
        ).apply(instance, ControlPoints::new));

    	public float islandInland;
    	public float islandCoast;
        public float deepOcean;
        public float shallowOcean;
        public float beach;
        public float coast;
        public float inland;
        
        public ControlPoints(float islandInland, float islandCoast, float deepOcean, float shallowOcean, float beach, float coast, float inland) {
        	this.islandInland = islandInland;
        	this.islandCoast = islandCoast;
            this.deepOcean = deepOcean;
            this.shallowOcean = shallowOcean;
            this.beach = beach;
            this.coast = coast;
            this.inland = inland;
        }
        
        public float coastMarker() {
        	return this.coast + (this.inland - this.coast) / 2.0F;
        }
        
        public ControlPoints copy() {
        	return new ControlPoints(this.islandInland, this.islandCoast, this.deepOcean, this.shallowOcean, this.beach, this.coast, this.inland);
        }
    }
    
    public static class Properties {
    	public static final int DEFAULT_OCEAN_DEPTH = 63;

    	public static final Codec<Properties> CODEC = RecordCodecBuilder.create(instance -> instance.group(
    		SpawnType.CODEC.fieldOf("spawnType").forGetter((o) -> o.spawnType),
    		Codec.INT.fieldOf("worldHeight").forGetter((o) -> o.worldHeight),
    		Codec.INT.optionalFieldOf("worldDepth", 64).forGetter((o) -> o.worldDepth),
    		Codec.INT.fieldOf("seaLevel").forGetter((o) -> o.seaLevel),
    		Codec.INT.optionalFieldOf("lavaLevel", -54).forGetter((o) -> o.lavaLevel),
    		Codec.INT.optionalFieldOf("oceanDepth", DEFAULT_OCEAN_DEPTH).forGetter((o) -> o.oceanDepth),
            Codec.INT.optionalFieldOf("spawnX", 0).forGetter((o) -> o.spawnX),
            Codec.INT.optionalFieldOf("spawnZ", 0).forGetter((o) -> o.spawnZ)
    	).apply(instance, Properties::new));

        public static SpawnType spawnType;
        public int worldHeight;
        public int worldDepth;
        public int seaLevel;
        public int lavaLevel;
        public int oceanDepth;
        public static int spawnX;
        public static int spawnZ;

        public Properties(SpawnType spawnType, int worldHeight, int worldDepth, int seaLevel, int lavaLevel, int oceanDepth, int spawnX, int spawnZ) {
        	this.spawnType = spawnType;
        	this.worldHeight = worldHeight;
        	this.worldDepth = worldDepth;
        	this.seaLevel = seaLevel;
        	this.lavaLevel = lavaLevel;
        	this.oceanDepth = Math.max(10, oceanDepth);
            this.spawnX = spawnX;
            this.spawnZ = spawnZ;
        }

        public Properties copy() {
        	return new Properties(this.spawnType, this.worldHeight, this.worldDepth, this.seaLevel, this.lavaLevel, this.oceanDepth, this.spawnX, this.spawnZ);
        }
        
        @Deprecated
        public int terrainScaler() {
        	return Math.min(this.worldHeight, 256);
        }
    }

    public static class Variance {
        public static final Codec<Variance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("noiseStrength", 0.6F).forGetter((o) -> o.noiseStrength),
            Codec.FLOAT.optionalFieldOf("climateBias", 0.35F).forGetter((o) -> o.climateBias)
        ).apply(instance, Variance::new));

        public float noiseStrength;
        public float climateBias;

        public Variance(float noiseStrength, float climateBias) {
            this.noiseStrength = noiseStrength;
            this.climateBias = climateBias;
        }

        public Variance copy() {
            return new Variance(this.noiseStrength, this.climateBias);
        }
    }

    public static class MaterialPalette {
        public static final Codec<MaterialPalette> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("sand", 1.0F).forGetter((o) -> o.sand),
            Codec.FLOAT.optionalFieldOf("gravel", 0.0F).forGetter((o) -> o.gravel),
            Codec.FLOAT.optionalFieldOf("stone", 0.0F).forGetter((o) -> o.stone),
            Codec.FLOAT.optionalFieldOf("mud", 0.0F).forGetter((o) -> o.mud),
            Codec.FLOAT.optionalFieldOf("redSand", 0.0F).forGetter((o) -> o.redSand)
        ).apply(instance, MaterialPalette::new));

        public float sand;
        public float gravel;
        public float stone;
        public float mud;
        public float redSand;

        public MaterialPalette(float sand, float gravel, float stone, float mud, float redSand) {
            this.sand = sand;
            this.gravel = gravel;
            this.stone = stone;
            this.mud = mud;
            this.redSand = redSand;
        }

        public MaterialPalette copy() {
            return new MaterialPalette(this.sand, this.gravel, this.stone, this.mud, this.redSand);
        }
    }

    public static class OceanGeometry {
        public static final OceanGeometry DEFAULT = new OceanGeometry(1.2F, 0.15F, 0.35F);
        public static final Codec<OceanGeometry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("coastBandScale", 1.2F).forGetter((o) -> o.coastBandScale),
            Codec.FLOAT.optionalFieldOf("transitionBias", 0.15F).forGetter((o) -> o.transitionBias),
            Codec.FLOAT.optionalFieldOf("continuityPadding", 0.35F).forGetter((o) -> o.continuityPadding)
        ).apply(instance, OceanGeometry::new));

        public float coastBandScale;
        public float transitionBias;
        public float continuityPadding;

        public OceanGeometry(float coastBandScale, float transitionBias, float continuityPadding) {
            this.coastBandScale = coastBandScale;
            this.transitionBias = transitionBias;
            this.continuityPadding = continuityPadding;
        }

        public OceanGeometry copy() {
            return new OceanGeometry(this.coastBandScale, this.transitionBias, this.continuityPadding);
        }
    }

    public static class Ocean {
        public static final Codec<Ocean> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("coverage", 0.68F).forGetter((o) -> o.coverage),
            Codec.INT.optionalFieldOf("surfaceDepth", 4).forGetter((o) -> o.surfaceDepth),
            Codec.FLOAT.optionalFieldOf("maxSlope", 0.34F).forGetter((o) -> o.maxSlope),
            Codec.INT.optionalFieldOf("minHeight", 0).forGetter((o) -> o.minHeight),
            Codec.INT.optionalFieldOf("maxHeight", 6).forGetter((o) -> o.maxHeight),
            MaterialPalette.CODEC.optionalFieldOf("materials", new MaterialPalette(1.0F, 0.45F, 0.15F, 0.05F, 0.12F)).forGetter((o) -> o.materials),
            OceanGeometry.CODEC.optionalFieldOf("geometry", OceanGeometry.DEFAULT).forGetter((o) -> o.geometry)
        ).apply(instance, Ocean::new));

        public float coverage;
        public int surfaceDepth;
        public float maxSlope;
        public int minHeight;
        public int maxHeight;
        public MaterialPalette materials;
        public OceanGeometry geometry;

        public Ocean(float coverage, int surfaceDepth, float maxSlope, int minHeight, int maxHeight, MaterialPalette materials, OceanGeometry geometry) {
            this.coverage = coverage;
            this.surfaceDepth = surfaceDepth;
            this.maxSlope = maxSlope;
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
            this.materials = materials;
            this.geometry = geometry;
        }

        public Ocean copy() {
            return new Ocean(this.coverage, this.surfaceDepth, this.maxSlope, this.minHeight, this.maxHeight, this.materials.copy(), this.geometry.copy());
        }
    }

    public static class River {
        public static final Codec<River> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("coverage", 0.34F).forGetter((o) -> o.coverage),
            Codec.INT.optionalFieldOf("surfaceDepth", 2).forGetter((o) -> o.surfaceDepth),
            Codec.FLOAT.optionalFieldOf("maxSlope", 0.26F).forGetter((o) -> o.maxSlope),
            Codec.INT.optionalFieldOf("minHeight", 0).forGetter((o) -> o.minHeight),
            Codec.INT.optionalFieldOf("maxHeight", 5).forGetter((o) -> o.maxHeight),
            MaterialPalette.CODEC.optionalFieldOf("materials", new MaterialPalette(0.4F, 1.0F, 0.55F, 0.35F, 0.03F)).forGetter((o) -> o.materials),
            Codec.FLOAT.optionalFieldOf("minWidth", 6.0F).forGetter((o) -> o.minWidth),
            Codec.FLOAT.optionalFieldOf("maxWidth", 24.0F).forGetter((o) -> o.maxWidth),
            Codec.FLOAT.optionalFieldOf("maxDepth", 8.0F).forGetter((o) -> o.maxDepth),
            Codec.FLOAT.optionalFieldOf("minBankHeight", 1.0F).forGetter((o) -> o.minBankHeight),
            Codec.FLOAT.optionalFieldOf("maxBankHeight", 7.0F).forGetter((o) -> o.maxBankHeight)
        ).apply(instance, River::new));

        public float coverage;
        public int surfaceDepth;
        public float maxSlope;
        public int minHeight;
        public int maxHeight;
        public MaterialPalette materials;
        public float minWidth;
        public float maxWidth;
        public float maxDepth;
        public float minBankHeight;
        public float maxBankHeight;

        public River(float coverage, int surfaceDepth, float maxSlope, int minHeight, int maxHeight, MaterialPalette materials, float minWidth, float maxWidth, float maxDepth, float minBankHeight, float maxBankHeight) {
            this.coverage = coverage;
            this.surfaceDepth = surfaceDepth;
            this.maxSlope = maxSlope;
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
            this.materials = materials;
            this.minWidth = minWidth;
            this.maxWidth = maxWidth;
            this.maxDepth = maxDepth;
            this.minBankHeight = minBankHeight;
            this.maxBankHeight = maxBankHeight;
        }

        public River copy() {
            return new River(this.coverage, this.surfaceDepth, this.maxSlope, this.minHeight, this.maxHeight, this.materials.copy(), this.minWidth, this.maxWidth, this.maxDepth, this.minBankHeight, this.maxBankHeight);
        }
    }

    public static class Lake {
        public static final Codec<Lake> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("coverage", 0.42F).forGetter((o) -> o.coverage),
            Codec.INT.optionalFieldOf("surfaceDepth", 3).forGetter((o) -> o.surfaceDepth),
            Codec.FLOAT.optionalFieldOf("maxSlope", 0.24F).forGetter((o) -> o.maxSlope),
            Codec.INT.optionalFieldOf("minHeight", 0).forGetter((o) -> o.minHeight),
            Codec.INT.optionalFieldOf("maxHeight", 6).forGetter((o) -> o.maxHeight),
            MaterialPalette.CODEC.optionalFieldOf("materials", new MaterialPalette(0.75F, 0.85F, 0.35F, 0.45F, 0.05F)).forGetter((o) -> o.materials),
            Codec.FLOAT.optionalFieldOf("maxDepth", 10.0F).forGetter((o) -> o.maxDepth),
            Codec.FLOAT.optionalFieldOf("minBankHeight", 1.0F).forGetter((o) -> o.minBankHeight),
            Codec.FLOAT.optionalFieldOf("maxBankHeight", 8.0F).forGetter((o) -> o.maxBankHeight)
        ).apply(instance, Lake::new));

        public float coverage;
        public int surfaceDepth;
        public float maxSlope;
        public int minHeight;
        public int maxHeight;
        public MaterialPalette materials;
        public float maxDepth;
        public float minBankHeight;
        public float maxBankHeight;

        public Lake(float coverage, int surfaceDepth, float maxSlope, int minHeight, int maxHeight, MaterialPalette materials, float maxDepth, float minBankHeight, float maxBankHeight) {
            this.coverage = coverage;
            this.surfaceDepth = surfaceDepth;
            this.maxSlope = maxSlope;
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
            this.materials = materials;
            this.maxDepth = maxDepth;
            this.minBankHeight = minBankHeight;
            this.maxBankHeight = maxBankHeight;
        }

        public Lake copy() {
            return new Lake(this.coverage, this.surfaceDepth, this.maxSlope, this.minHeight, this.maxHeight, this.materials.copy(), this.maxDepth, this.minBankHeight, this.maxBankHeight);
        }
    }

    public static class Beach {
        public static final Beach DEFAULT = new Beach(
            new Variance(0.6F, 0.35F),
            new Ocean(0.68F, 4, 0.34F, 0, 6, new MaterialPalette(1.0F, 0.45F, 0.15F, 0.05F, 0.12F), OceanGeometry.DEFAULT.copy()),
            new River(0.34F, 2, 0.26F, 0, 5, new MaterialPalette(0.4F, 1.0F, 0.55F, 0.35F, 0.03F), 6, 24, 8, 1, 7),
            new Lake(0.42F, 3, 0.24F, 0, 6, new MaterialPalette(0.75F, 0.85F, 0.35F, 0.45F, 0.05F), 10, 1, 8)
        );

        public static final Codec<Beach> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Variance.CODEC.optionalFieldOf("variance", new Variance(0.6F, 0.35F)).forGetter((o) -> o.variance),
            Ocean.CODEC.optionalFieldOf("ocean", DEFAULT.ocean).forGetter((o) -> o.ocean),
            River.CODEC.optionalFieldOf("river", DEFAULT.river).forGetter((o) -> o.river),
            Lake.CODEC.optionalFieldOf("lake", DEFAULT.lake).forGetter((o) -> o.lake)
        ).apply(instance, Beach::new));

        public Variance variance;
        public Ocean ocean;
        public River river;
        public Lake lake;

        public Beach(Variance variance, Ocean ocean, River river, Lake lake) {
            this.variance = variance;
            this.ocean = ocean;
            this.river = river;
            this.lake = lake;
        }

        public Beach copy() {
            return new Beach(this.variance.copy(), this.ocean.copy(), this.river.copy(), this.lake.copy());
        }
    }
}
