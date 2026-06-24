package raccoonman.reterraforged.data.worldgen.preset.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class MiscellaneousSettings {
	public static final Codec<MiscellaneousSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.BOOL.fieldOf("smoothLayerDecorator").forGetter((s) -> s.smoothLayerDecorator),
		Codec.INT.fieldOf("strataRegionSize").forGetter((s) -> s.strataRegionSize),
		Codec.BOOL.fieldOf("strataDecorator").forGetter((s) -> s.strataDecorator),
		Codec.BOOL.fieldOf("oreCompatibleStoneOnly").forGetter((s) -> s.oreCompatibleStoneOnly),
		Codec.BOOL.fieldOf("erosionDecorator").forGetter((s) -> s.erosionDecorator),
		Codec.BOOL.fieldOf("plainStoneErosion").forGetter((s) -> s.plainStoneErosion),
		Codec.BOOL.fieldOf("naturalSnowDecorator").forGetter((s) -> s.naturalSnowDecorator),
		Codec.BOOL.fieldOf("customBiomeFeatures").forGetter((s) -> s.customBiomeFeatures),
		Codec.BOOL.fieldOf("vanillaSprings").forGetter((s) -> s.vanillaSprings),
		Codec.BOOL.fieldOf("vanillaLavaLakes").forGetter((s) -> s.vanillaLavaLakes),
		Codec.BOOL.fieldOf("vanillaLavaSprings").forGetter((s) -> s.vanillaLavaSprings),
		Codec.FLOAT.fieldOf("mountainBiomeUsage").forGetter((s) -> s.mountainBiomeUsage),
		Codec.FLOAT.fieldOf("volcanoBiomeUsage").forGetter((s) -> s.volcanoBiomeUsage),
		StrataSettings.CODEC.optionalFieldOf("strata", StrataSettings.DEFAULT).forGetter((s) -> s.strata)
	).apply(instance, MiscellaneousSettings::new));
	
	public boolean smoothLayerDecorator;
	public int strataRegionSize;
	public boolean strataDecorator;
	public boolean oreCompatibleStoneOnly;
	public boolean erosionDecorator;
	public boolean plainStoneErosion;
	public boolean naturalSnowDecorator;
	public boolean customBiomeFeatures;
	public boolean vanillaSprings;
	public boolean vanillaLavaLakes;
	public boolean vanillaLavaSprings;
    public float mountainBiomeUsage;
    public float volcanoBiomeUsage;
    public StrataSettings strata;
	
	public MiscellaneousSettings(
		boolean smoothLayerDecorator,
		int strataRegionSize,
		boolean strataDecorator,
		boolean oreCompatibleStoneOnly,
		boolean erosionDecorator,
		boolean plainStoneErosion,
		boolean naturalSnowDecorator,
		boolean customBiomeFeatures,
		boolean vanillaSprings,
		boolean vanillaLavaLakes,
	    boolean vanillaLavaSprings,
	    float mountainBiomeUsage,
	    float volcanoBiomeUsage
	) {
		this(smoothLayerDecorator, strataRegionSize, strataDecorator, oreCompatibleStoneOnly, erosionDecorator, plainStoneErosion, naturalSnowDecorator, customBiomeFeatures, vanillaSprings, vanillaLavaLakes, vanillaLavaSprings, mountainBiomeUsage, volcanoBiomeUsage, StrataSettings.DEFAULT.copy());
	}
	
	public MiscellaneousSettings(
		boolean smoothLayerDecorator,
		int strataRegionSize,
		boolean strataDecorator,
		boolean oreCompatibleStoneOnly,
		boolean erosionDecorator,
		boolean plainStoneErosion,
		boolean naturalSnowDecorator,
		boolean customBiomeFeatures,
		boolean vanillaSprings,
		boolean vanillaLavaLakes,
		boolean vanillaLavaSprings,
	    float mountainBiomeUsage,
	    float volcanoBiomeUsage,
	    StrataSettings strata
	) {
		this.smoothLayerDecorator = smoothLayerDecorator;
		this.strataRegionSize = strataRegionSize;
		this.strataDecorator = strataDecorator;
		this.oreCompatibleStoneOnly = oreCompatibleStoneOnly;
		this.erosionDecorator = erosionDecorator;
		this.plainStoneErosion = plainStoneErosion;
		this.naturalSnowDecorator = naturalSnowDecorator;
		this.customBiomeFeatures = customBiomeFeatures;
		this.vanillaSprings = vanillaSprings;
		this.vanillaLavaLakes = vanillaLavaLakes;
		this.vanillaLavaSprings = vanillaLavaSprings;
		this.mountainBiomeUsage = mountainBiomeUsage;
		this.volcanoBiomeUsage = volcanoBiomeUsage;
		this.strata = strata.copy();
	}
	
	public MiscellaneousSettings copy() {
		return new MiscellaneousSettings(this.smoothLayerDecorator, this.strataRegionSize, this.strataDecorator, this.oreCompatibleStoneOnly, this.erosionDecorator, this.plainStoneErosion, this.naturalSnowDecorator, this.customBiomeFeatures, this.vanillaSprings, this.vanillaLavaLakes, this.vanillaLavaSprings, this.mountainBiomeUsage, this.volcanoBiomeUsage, this.strata.copy());
	}
	
	public static class StrataSettings {
		public static final StrataSettings DEFAULT = new StrataSettings(4, 10, 0.1F, 1.5F, 1.5F, 1.0F, 1.0F, 1.0F);
		public static final Codec<StrataSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.optionalFieldOf("rockMinLayers", 4).forGetter((s) -> s.rockMinLayers),
			Codec.INT.optionalFieldOf("rockMaxLayers", 10).forGetter((s) -> s.rockMaxLayers),
			Codec.FLOAT.optionalFieldOf("rockMinDepth", 0.1F).forGetter((s) -> s.rockMinDepth),
			Codec.FLOAT.optionalFieldOf("rockMaxDepth", 1.5F).forGetter((s) -> s.rockMaxDepth),
			Codec.FLOAT.optionalFieldOf("stoneWeight", 1.5F).forGetter((s) -> s.stoneWeight),
			Codec.FLOAT.optionalFieldOf("graniteWeight", 1.0F).forGetter((s) -> s.graniteWeight),
			Codec.FLOAT.optionalFieldOf("andesiteWeight", 1.0F).forGetter((s) -> s.andesiteWeight),
			Codec.FLOAT.optionalFieldOf("dioriteWeight", 1.0F).forGetter((s) -> s.dioriteWeight)
		).apply(instance, StrataSettings::new));
		
		public int rockMinLayers;
		public int rockMaxLayers;
		public float rockMinDepth;
		public float rockMaxDepth;
		public float stoneWeight;
		public float graniteWeight;
		public float andesiteWeight;
		public float dioriteWeight;
		
		public StrataSettings(int rockMinLayers, int rockMaxLayers, float rockMinDepth, float rockMaxDepth, float stoneWeight, float graniteWeight, float andesiteWeight, float dioriteWeight) {
			this.rockMinLayers = rockMinLayers;
			this.rockMaxLayers = rockMaxLayers;
			this.rockMinDepth = rockMinDepth;
			this.rockMaxDepth = rockMaxDepth;
			this.stoneWeight = stoneWeight;
			this.graniteWeight = graniteWeight;
			this.andesiteWeight = andesiteWeight;
			this.dioriteWeight = dioriteWeight;
		}
		
		public StrataSettings copy() {
			return new StrataSettings(this.rockMinLayers, this.rockMaxLayers, this.rockMinDepth, this.rockMaxDepth, this.stoneWeight, this.graniteWeight, this.andesiteWeight, this.dioriteWeight);
		}
	}
}
