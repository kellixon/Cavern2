package cavern.item;

import javax.annotation.Nullable;

import cavern.api.CavernAPI;
import cavern.api.IPortalCache;
import cavern.core.Cavern;
import cavern.handler.CaveEventHooks;
import cavern.stats.PlayerData;
import cavern.stats.PortalCache;
import cavern.util.CaveUtils;
import cavern.world.CaveDimensions;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ITeleporter;

public class ItemMirageBook extends Item implements ITeleporter
{
	public ItemMirageBook()
	{
		super();
		this.setTranslationKey("mirageBook");
		this.setMaxStackSize(1);
		this.setHasSubtypes(true);
		this.setCreativeTab(Cavern.TAB_CAVERN);
	}

	@Override
	public String getTranslationKey(ItemStack stack)
	{
		return getTranslationKey() + "." + EnumType.byItemStack(stack).getTranslationKey();
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack)
	{
		return Cavern.proxy.translateFormat(getTranslationKey() + ".name", super.getItemStackDisplayName(stack));
	}

	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> subItems)
	{
		if (!isInCreativeTab(tab))
		{
			return;
		}

		for (EnumType type : EnumType.VALUES)
		{
			if (type.getDimension() != null)
			{
				subItems.add(type.getItemStack());
			}
		}
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand)
	{
		ItemStack stack = player.getHeldItem(hand);
		DimensionType type = EnumType.byItemStack(stack).getDimension();

		if (type == null)
		{
			return new ActionResult<>(EnumActionResult.PASS, stack);
		}

		if (world.provider.getDimensionType() == type)
		{
			if (!world.isRemote)
			{
				PlayerData.get(player).setLastTeleportTime(type, world.getTotalWorldTime());

				transferTo(null, player);
			}

			return new ActionResult<>(EnumActionResult.SUCCESS, stack);
		}

		if (world.isRemote)
		{
			if (CavernAPI.dimension.isInMirageWorlds(player))
			{
				player.sendStatusMessage(new TextComponentTranslation(getTranslationKey() + ".fail"), true);
			}
			else
			{
				player.sendStatusMessage(new TextComponentTranslation(getTranslationKey() + ".portal"), true);
			}
		}

		return new ActionResult<>(EnumActionResult.PASS, stack);
	}

	public boolean transferTo(@Nullable DimensionType dimNew, EntityPlayer entityPlayer)
	{
		if (entityPlayer == null || !(entityPlayer instanceof EntityPlayerMP))
		{
			return false;
		}

		EntityPlayerMP player = (EntityPlayerMP)entityPlayer;
		ResourceLocation key = CaveUtils.getKey("mirage_worlds");
		IPortalCache cache = PortalCache.get(player);
		DimensionType dimOld = player.world.provider.getDimensionType();

		if (dimNew == null)
		{
			dimNew = cache.getLastDim(key, null);
		}

		if (dimNew == null || dimOld == dimNew)
		{
			return false;
		}

		if (CavernAPI.dimension.isMirageWorlds(dimNew))
		{
			cache.setLastDim(key, dimOld);
		}

		cache.setLastPos(key, dimOld, player.getPosition());

		player.timeUntilPortal = player.getPortalCooldown();

		player.changeDimension(dimNew.getId(), this);

		if (player.getBedLocation(dimNew.getId()) == null)
		{
			player.setSpawnChunk(player.getPosition(), true, dimNew.getId());
		}

		return true;
	}

	@Override
	public void placeEntity(World world, Entity entity, float rotationYaw)
	{
		if (attemptToLastPos(world, entity))
		{
			return;
		}

		if (attemptRandomly(world, entity))
		{
			return;
		}

		attemptToVoid(world, entity);
	}

	protected boolean attemptToLastPos(World world, Entity entity)
	{
		IPortalCache cache = PortalCache.get(entity);
		ResourceLocation key = CaveUtils.getKey("mirage_worlds");
		DimensionType type = world.provider.getDimensionType();

		if (cache.hasLastPos(key, type))
		{
			BlockPos pos = cache.getLastPos(key, type);

			if (world.getBlockState(pos.down()).getMaterial().isSolid() && world.getBlockState(pos).getBlock().canSpawnInBlock() && world.getBlockState(pos.up()).getBlock().canSpawnInBlock())
			{
				entity.moveToBlockPosAndAngles(pos, entity.rotationYaw, entity.rotationPitch);

				return true;
			}

			cache.setLastPos(key, type, null);
		}

		return false;
	}

	protected boolean attemptRandomly(World world, Entity entity)
	{
		int count = 0;

		outside: while (++count < 50)
		{
			int x = MathHelper.floor(entity.posX) + itemRand.nextInt(64) - 32;
			int z = MathHelper.floor(entity.posZ) + itemRand.nextInt(64) - 32;
			int y = CavernAPI.dimension.isInCaves(entity) ? itemRand.nextInt(30) + 20 : itemRand.nextInt(20) + 60;
			BlockPos pos = new BlockPos(x, y, z);

			while (pos.getY() > 1 && world.isAirBlock(pos))
			{
				pos = pos.down();
			}

			while (pos.getY() < world.getActualHeight() - 3 && !world.isAirBlock(pos))
			{
				pos = pos.up();
			}

			if (world.getBlockState(pos.down()).getMaterial().isSolid() && world.getBlockState(pos).getBlock().canSpawnInBlock() && world.getBlockState(pos.up()).getBlock().canSpawnInBlock())
			{
				for (BlockPos around : BlockPos.getAllInBoxMutable(pos.add(-4, 0, -4), pos.add(4, 0, 4)))
				{
					if (world.getBlockState(around).getMaterial().isLiquid())
					{
						continue outside;
					}
				}

				entity.moveToBlockPosAndAngles(pos, entity.rotationYaw, entity.rotationPitch);

				return true;
			}
		}

		return false;
	}

	protected boolean attemptToVoid(World world, Entity entity)
	{
		if (!CavernAPI.dimension.isInTheVoid(entity))
		{
			return false;
		}

		BlockPos pos = new BlockPos(entity.posX, 0.0D, entity.posZ);
		BlockPos from = pos.add(-1, 0, -1);
		BlockPos to = pos.add(1, 0, 1);

		BlockPos.getAllInBoxMutable(from, to).forEach(blockPos -> world.setBlockState(blockPos, Blocks.MOSSY_COBBLESTONE.getDefaultState(), 2));

		entity.moveToBlockPosAndAngles(pos.up(), entity.rotationYaw, entity.rotationPitch);

		return true;
	}

	public static ItemStack getRandomBook()
	{
		int i = MathHelper.floor(CaveEventHooks.RANDOM.nextDouble() * EnumType.VALUES.length);
		EnumType type = EnumType.VALUES[i];

		if (type.getDimension() != null)
		{
			return type.getItemStack();
		}

		return ItemStack.EMPTY;
	}

	public enum EnumType
	{
		CAVELAND(0, "caveland"),
		CAVENIA(1, "cavenia"),
		FROST_MOUNTAINS(2, "frostMountains"),
		WIDE_DESERT(3, "wideDesert"),
		THE_VOID(4, "theVoid"),
		DARK_FOREST(5, "darkForest"),
		CROWN_CLIFFS(6, "crownCliffs");

		public static final EnumType[] VALUES = new EnumType[values().length];

		private final int meta;
		private final String translationKey;

		private EnumType(int meta, String name)
		{
			this.meta = meta;
			this.translationKey = name;
		}

		public int getMetadata()
		{
			return meta;
		}

		public String getTranslationKey()
		{
			return translationKey;
		}

		@Nullable
		public DimensionType getDimension()
		{
			switch (this)
			{
				case CAVELAND:
					return CaveDimensions.CAVELAND;
				case CAVENIA:
					return CaveDimensions.CAVENIA;
				case FROST_MOUNTAINS:
					return CaveDimensions.FROST_MOUNTAINS;
				case WIDE_DESERT:
					return CaveDimensions.WIDE_DESERT;
				case THE_VOID:
					return CaveDimensions.THE_VOID;
				case DARK_FOREST:
					return CaveDimensions.DARK_FOREST;
				case CROWN_CLIFFS:
					return CaveDimensions.CROWN_CLIFFS;
			}

			return null;
		}

		public ItemStack getItemStack()
		{
			return new ItemStack(CaveItems.MIRAGE_BOOK, 1, getMetadata());
		}

		public static EnumType byMetadata(int meta)
		{
			if (meta < 0 || meta >= VALUES.length)
			{
				meta = 0;
			}

			return VALUES[meta];
		}

		public static EnumType byItemStack(ItemStack stack)
		{
			return byMetadata(stack.isEmpty() ? 0 : stack.getMetadata());
		}

		static
		{
			for (EnumType type : values())
			{
				VALUES[type.getMetadata()] = type;
			}
		}
	}
}