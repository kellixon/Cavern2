package cavern.core;

import org.apache.logging.log4j.Level;

import cavern.api.CavernAPI;
import cavern.block.CaveBlocks;
import cavern.capability.CaveCapabilities;
import cavern.client.CaveKeyBindings;
import cavern.client.CaveRenderingRegistry;
import cavern.client.config.CaveConfigEntries;
import cavern.client.handler.ClientEventHooks;
import cavern.client.handler.MinerStatsHUDEventHooks;
import cavern.config.AquaCavernConfig;
import cavern.config.CavelandConfig;
import cavern.config.CaveniaConfig;
import cavern.config.CavernConfig;
import cavern.config.Config;
import cavern.config.DisplayConfig;
import cavern.config.GeneralConfig;
import cavern.config.HugeCavernConfig;
import cavern.config.MiningAssistConfig;
import cavern.config.MirageWorldsConfig;
import cavern.entity.CaveEntityRegistry;
import cavern.handler.CaveEventHooks;
import cavern.handler.CavebornEventHooks;
import cavern.handler.MiningAssistEventHooks;
import cavern.handler.api.CavernAPIHandler;
import cavern.handler.api.DimensionHandler;
import cavern.handler.api.StatsHandler;
import cavern.item.CaveItems;
import cavern.network.CaveNetworkRegistry;
import cavern.plugin.HaCPlugin;
import cavern.plugin.MCEPlugin;
import cavern.stats.MinerStats;
import cavern.util.CaveLog;
import cavern.util.Version;
import cavern.world.CaveDimensions;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.Mod.Metadata;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;

@Mod
(
	modid = Cavern.MODID,
	guiFactory = "cavern.client.config.CaveGuiFactory",
	updateJSON = "https://raw.githubusercontent.com/kegare/Cavern2/master/cavern2.json"
)
public class Cavern
{
	public static final String MODID = "cavern";

	@Instance(MODID)
	public static Cavern instance;

	@Metadata(MODID)
	public static ModMetadata metadata;

	@SidedProxy(modId = MODID, clientSide = "cavern.client.ClientProxy", serverSide = "cavern.core.CommonProxy")
	public static CommonProxy proxy;

	public static final CreativeTabCavern TAB_CAVERN = new CreativeTabCavern();

	@EventHandler
	public void construct(FMLConstructionEvent event)
	{
		Version.initVersion();

		CavernAPI.apiHandler = new CavernAPIHandler();
		CavernAPI.dimension = new DimensionHandler();
		CavernAPI.stats = new StatsHandler();

		if (event.getSide().isClient())
		{
			clientConstruct();
		}

		Config.updateConfig();

		MinecraftForge.EVENT_BUS.register(this);
	}

	@SideOnly(Side.CLIENT)
	public void clientConstruct()
	{
		Minecraft mc = FMLClientHandler.instance().getClient();

		if (mc.isJava64bit() && Runtime.getRuntime().maxMemory() >= 2000000000L)
		{
			Config.highProfiles = true;
		}

		CaveConfigEntries.initEntries();
	}

	@EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		GeneralConfig.syncConfig();

		CaveNetworkRegistry.registerMessages();

		CaveCapabilities.registerCapabilities();

		CavernAPIHandler.registerItems(CavernAPI.apiHandler);
		CavernAPIHandler.registerEvents(CavernAPI.apiHandler);

		MiningAssistConfig.syncConfig();

		MinerStats.registerMineBonus();

		if (event.getSide().isClient())
		{
			DisplayConfig.syncConfig();

			CaveRenderingRegistry.registerRenderers();
			CaveRenderingRegistry.registerRenderBlocks();

			CaveKeyBindings.registerKeyBindings();

			MinecraftForge.EVENT_BUS.register(new ClientEventHooks());
			MinecraftForge.EVENT_BUS.register(new MinerStatsHUDEventHooks());
		}

		MinecraftForge.EVENT_BUS.register(new CaveEventHooks());
		MinecraftForge.EVENT_BUS.register(new CavebornEventHooks());
		MinecraftForge.EVENT_BUS.register(new MiningAssistEventHooks());
	}

	@SubscribeEvent
	public void registerBlocks(RegistryEvent.Register<Block> event)
	{
		IForgeRegistry<Block> registry = event.getRegistry();

		CaveBlocks.registerBlocks(registry);
	}

	@SubscribeEvent
	public void registerItems(RegistryEvent.Register<Item> event)
	{
		IForgeRegistry<Item> registry = event.getRegistry();

		CaveBlocks.registerItemBlocks(registry);
		CaveItems.registerItems(registry);
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void registerModels(ModelRegistryEvent event)
	{
		CaveBlocks.registerModels();
		CaveItems.registerModels();
	}

	@SubscribeEvent
	public void registerSounds(RegistryEvent.Register<SoundEvent> event)
	{
		IForgeRegistry<SoundEvent> registry = event.getRegistry();

		CaveSounds.registerSounds(registry);
	}

	@SubscribeEvent
	public void registerEntityEntries(RegistryEvent.Register<EntityEntry> event)
	{
		CaveEntityRegistry.registerEntities();
	}

	@SubscribeEvent
	public void registerRecipes(RegistryEvent.Register<IRecipe> event)
	{
		IForgeRegistry<IRecipe> registry = event.getRegistry();

		CaveItems.registerRecipes(registry);
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void registerBlockColors(ColorHandlerEvent.Block event)
	{
		BlockColors colors = event.getBlockColors();

		CaveBlocks.registerBlockColors(colors);
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void registerItemColors(ColorHandlerEvent.Item event)
	{
		ItemColors itemColors = event.getItemColors();
		BlockColors blockColors = event.getBlockColors();

		CaveBlocks.registerItemBlockColors(blockColors, itemColors);
	}

	@EventHandler
	public void init(FMLInitializationEvent event)
	{
		CaveBlocks.registerOreDicts();
		CaveItems.registerOreDicts();

		CaveItems.registerEquipments();

		CaveBlocks.registerSmeltingRecipes();

		CaveEntityRegistry.addSpawns();

		CavernConfig.syncConfig();
		CavernConfig.syncBiomesConfig();
		CavernConfig.syncVeinsConfig();

		HugeCavernConfig.syncConfig();
		HugeCavernConfig.syncBiomesConfig();
		HugeCavernConfig.syncVeinsConfig();

		AquaCavernConfig.syncConfig();
		AquaCavernConfig.syncBiomesConfig();
		AquaCavernConfig.syncVeinsConfig();

		CavelandConfig.syncConfig();
		CavelandConfig.syncVeinsConfig();

		CaveniaConfig.syncConfig();
		CaveniaConfig.syncBiomesConfig();
		CaveniaConfig.syncVeinsConfig();

		MirageWorldsConfig.syncConfig();

		CaveDimensions.registerDimensions();
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event)
	{
		loadPlugins();
	}

	public void loadPlugins()
	{
		if (Loader.isModLoaded(HaCPlugin.LIB_MODID))
		{
			try
			{
				HaCPlugin.load();
			}
			catch (Exception e)
			{
				CaveLog.log(Level.WARN, e, "Failed to load the Heat&Climate mod plugin.");
			}
		}

		if (Loader.isModLoaded(MCEPlugin.MODID))
		{
			try
			{
				MCEPlugin.load();
			}
			catch (Exception e)
			{
				CaveLog.log(Level.WARN, e, "Failed to load the MCEconomy mod plugin.");
			}
		}
	}

	@EventHandler
	public void loaded(FMLLoadCompleteEvent event)
	{
		if (GeneralConfig.miningPoints.shouldInit())
		{
			GeneralConfig.miningPoints.init();
		}

		Config.saveConfig(GeneralConfig.config);
	}

	@EventHandler
	public void onServerStarting(FMLServerStartingEvent event)
	{
		if (event.getSide().isServer() && Config.configChecker.isUpdated())
		{
			event.getServer().sendMessage(new TextComponentTranslation("cavern.config.message.update"));
		}

		event.registerServerCommand(new CommandCavern());

		GeneralConfig.refreshMiningPointItems();
		GeneralConfig.refreshMiningPoints();
		GeneralConfig.refreshCavebornBonusItems();

		MiningAssistConfig.refreshEffectiveItems();
		MiningAssistConfig.refreshTargetBlocks();

		CavernConfig.refreshTriggerItems();
		CavernConfig.refreshDungeonMobs();

		HugeCavernConfig.refreshTriggerItems();

		AquaCavernConfig.refreshTriggerItems();
		AquaCavernConfig.refreshDungeonMobs();
	}
}