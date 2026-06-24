package raccoonman.reterraforged.client.gui.screen.presetconfig;

import java.util.Optional;

import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;
import raccoonman.reterraforged.client.data.RTFTranslationKeys;
import raccoonman.reterraforged.client.gui.screen.page.LinkedPageScreen.Page;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetListPage.PresetEntry;
import raccoonman.reterraforged.client.gui.widget.Slider;
import raccoonman.reterraforged.data.worldgen.preset.settings.MiscellaneousSettings;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;

public class MiscellaneousPage extends PresetEditorPage {
	private CycleButton<Boolean> smoothLayerDecorator;
	private Slider strataRegionSize;
	private CycleButton<Boolean> strataDecorator;
	private Slider strataRockMinLayers;
	private Slider strataRockMaxLayers;
	private Slider strataRockMinDepth;
	private Slider strataRockMaxDepth;
	private Slider strataStoneWeight;
	private Slider strataGraniteWeight;
	private Slider strataAndesiteWeight;
	private Slider strataDioriteWeight;
	private CycleButton<Boolean> oreCompatibleStoneOnly;
	private CycleButton<Boolean> plainStoneErosion;
	private CycleButton<Boolean> naturalSnowDecorator;
	private CycleButton<Boolean> customBiomeFeatures;
	private CycleButton<Boolean> vanillaSprings;
	private CycleButton<Boolean> vanillaLavaLakes;
	private CycleButton<Boolean> vanillaLavaSprings;
	private Slider mountainBiomeUsage;
	private Slider volcanoBiomeUsage;
	
	public MiscellaneousPage(PresetConfigScreen screen, PresetEntry preset) {
		super(screen, preset);
	}

	@Override
	public Component title() {
		return Component.translatable(RTFTranslationKeys.GUI_MISCELLANEOUS_SETTINGS_TITLE);
	}

	@Override
	public void init() {
		super.init();
		
		Preset preset = this.preset.getPreset();
		MiscellaneousSettings miscellaneous = preset.miscellaneous();
		MiscellaneousSettings.StrataSettings strata = miscellaneous.strata;
		
		this.smoothLayerDecorator = PresetWidgets.createToggle(miscellaneous.smoothLayerDecorator, RTFTranslationKeys.GUI_BUTTON_SMOOTH_LAYER_DECORATOR, (button, value) -> {
			miscellaneous.smoothLayerDecorator = value;
		});
		this.strataRegionSize = PresetWidgets.createIntSlider(miscellaneous.strataRegionSize, 50, 1000, RTFTranslationKeys.GUI_SLIDER_STRATA_REGION_SIZE, (slider, value) -> {
			miscellaneous.strataRegionSize = (int) slider.scaleValue(value);
			return value;
		});
		this.strataDecorator = PresetWidgets.createToggle(miscellaneous.strataDecorator, RTFTranslationKeys.GUI_BUTTON_STRATA_DECORATOR, (button, value) -> {
			miscellaneous.strataDecorator = value;
		});
		this.strataRockMinLayers = PresetWidgets.createIntSlider(strata.rockMinLayers, 1, 64, RTFTranslationKeys.GUI_SLIDER_STRATA_ROCK_MIN_LAYERS, (slider, value) -> {
			int layers = (int) slider.scaleValue(value);
			layers = Math.min(layers, strata.rockMaxLayers);
			strata.rockMinLayers = layers;
			return slider.getSliderValue(layers);
		});
		this.strataRockMaxLayers = PresetWidgets.createIntSlider(strata.rockMaxLayers, 1, 64, RTFTranslationKeys.GUI_SLIDER_STRATA_ROCK_MAX_LAYERS, (slider, value) -> {
			int layers = (int) slider.scaleValue(value);
			layers = Math.max(layers, strata.rockMinLayers);
			strata.rockMaxLayers = layers;
			return slider.getSliderValue(layers);
		});
		this.strataRockMinDepth = PresetWidgets.createFloatSlider(strata.rockMinDepth, 0.01F, 5.0F, RTFTranslationKeys.GUI_SLIDER_STRATA_ROCK_MIN_DEPTH, (slider, value) -> {
			float depth = (float) slider.scaleValue(value);
			depth = Math.min(depth, strata.rockMaxDepth);
			strata.rockMinDepth = depth;
			return slider.getSliderValue(depth);
		});
		this.strataRockMaxDepth = PresetWidgets.createFloatSlider(strata.rockMaxDepth, 0.01F, 5.0F, RTFTranslationKeys.GUI_SLIDER_STRATA_ROCK_MAX_DEPTH, (slider, value) -> {
			float depth = (float) slider.scaleValue(value);
			depth = Math.max(depth, strata.rockMinDepth);
			strata.rockMaxDepth = depth;
			return slider.getSliderValue(depth);
		});
		this.strataStoneWeight = PresetWidgets.createFloatSlider(strata.stoneWeight, 0.0F, 10.0F, RTFTranslationKeys.GUI_SLIDER_STRATA_STONE_WEIGHT, (slider, value) -> {
			strata.stoneWeight = (float) slider.scaleValue(value);
			return value;
		});
		this.strataGraniteWeight = PresetWidgets.createFloatSlider(strata.graniteWeight, 0.0F, 10.0F, RTFTranslationKeys.GUI_SLIDER_STRATA_GRANITE_WEIGHT, (slider, value) -> {
			strata.graniteWeight = (float) slider.scaleValue(value);
			return value;
		});
		this.strataAndesiteWeight = PresetWidgets.createFloatSlider(strata.andesiteWeight, 0.0F, 10.0F, RTFTranslationKeys.GUI_SLIDER_STRATA_ANDESITE_WEIGHT, (slider, value) -> {
			strata.andesiteWeight = (float) slider.scaleValue(value);
			return value;
		});
		this.strataDioriteWeight = PresetWidgets.createFloatSlider(strata.dioriteWeight, 0.0F, 10.0F, RTFTranslationKeys.GUI_SLIDER_STRATA_DIORITE_WEIGHT, (slider, value) -> {
			strata.dioriteWeight = (float) slider.scaleValue(value);
			return value;
		});
		this.oreCompatibleStoneOnly = PresetWidgets.createToggle(miscellaneous.oreCompatibleStoneOnly, RTFTranslationKeys.GUI_BUTTON_ORE_COMPATIBLE_STONE_ONLY, (button, value) -> {
			miscellaneous.oreCompatibleStoneOnly = value;
		});
		this.plainStoneErosion = PresetWidgets.createToggle(miscellaneous.plainStoneErosion, RTFTranslationKeys.GUI_BUTTON_PLAIN_STONE_EROSION, (button, value) -> {
			miscellaneous.plainStoneErosion = value;
		});
		this.naturalSnowDecorator = PresetWidgets.createToggle(miscellaneous.naturalSnowDecorator, RTFTranslationKeys.GUI_BUTTON_NATURAL_SNOW_DECORATOR, (button, value) -> {
			miscellaneous.naturalSnowDecorator = value;
		});
		this.customBiomeFeatures = PresetWidgets.createToggle(miscellaneous.customBiomeFeatures, RTFTranslationKeys.GUI_BUTTON_CUSTOM_BIOME_FEATURES, (button, value) -> {
			miscellaneous.customBiomeFeatures = value;
		});
		this.vanillaSprings = PresetWidgets.createToggle(miscellaneous.vanillaSprings, RTFTranslationKeys.GUI_BUTTON_VANILLA_SPRINGS, (button, value) -> {
			miscellaneous.vanillaSprings = value;
		});
		this.vanillaLavaLakes = PresetWidgets.createToggle(miscellaneous.vanillaLavaLakes, RTFTranslationKeys.GUI_BUTTON_VANILLA_LAVA_LAKES, (button, value) -> {
			miscellaneous.vanillaLavaLakes = value;
		});
		this.vanillaLavaSprings = PresetWidgets.createToggle(miscellaneous.vanillaLavaSprings, RTFTranslationKeys.GUI_BUTTON_VANILLA_LAVA_SPRINGS, (button, value) -> {
			miscellaneous.vanillaLavaSprings = value;
		});
		this.mountainBiomeUsage = PresetWidgets.createFloatSlider(miscellaneous.mountainBiomeUsage, 0.0F, 1.0F, RTFTranslationKeys.GUI_SLIDER_MOUNTAIN_BIOME_USAGE, (slider, value) -> {
			miscellaneous.mountainBiomeUsage = (float) slider.scaleValue(value);
			return value;
		});
		this.volcanoBiomeUsage = PresetWidgets.createFloatSlider(miscellaneous.volcanoBiomeUsage, 0.0F, 1.0F, RTFTranslationKeys.GUI_SLIDER_VOLCANO_BIOME_USAGE, (slider, value) -> {
			miscellaneous.volcanoBiomeUsage = (float) slider.scaleValue(value);
			return value;
		});
		
		this.left.addWidget(this.smoothLayerDecorator);
		this.left.addWidget(this.strataRegionSize);
		this.left.addWidget(this.strataDecorator);
		this.left.addWidget(this.strataRockMinLayers);
		this.left.addWidget(this.strataRockMaxLayers);
		this.left.addWidget(this.strataRockMinDepth);
		this.left.addWidget(this.strataRockMaxDepth);
		this.left.addWidget(this.strataStoneWeight);
		this.left.addWidget(this.strataGraniteWeight);
		this.left.addWidget(this.strataAndesiteWeight);
		this.left.addWidget(this.strataDioriteWeight);
		this.left.addWidget(this.oreCompatibleStoneOnly);
		this.left.addWidget(this.plainStoneErosion);
		this.left.addWidget(this.naturalSnowDecorator);
		this.left.addWidget(this.customBiomeFeatures);
		this.left.addWidget(this.vanillaSprings);
		this.left.addWidget(this.vanillaLavaLakes);
		this.left.addWidget(this.vanillaLavaSprings);
		this.left.addWidget(this.mountainBiomeUsage);
		this.left.addWidget(this.volcanoBiomeUsage);
	}
	
	@Override
	public Optional<Page> previous() {
		return Optional.of(new FilterSettingsPage(this.screen, this.preset));
	}

	@Override
	public Optional<Page> next() {
		return Optional.empty();
	}

}
