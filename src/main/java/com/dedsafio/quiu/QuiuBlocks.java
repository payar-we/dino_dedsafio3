package com.dedsafio.quiu;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.MapColor;
import net.minecraft.entity.Entity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/** Todos los bloques de Quiu. */
public final class QuiuBlocks {
	private QuiuBlocks() {}

	private static AbstractBlock.Settings piedra(float dureza, BlockSoundGroup s, MapColor c) {
		return AbstractBlock.Settings.create().mapColor(c).strength(dureza, 6.0f).sounds(s);
	}

	public static final Block TRONCO_QUIU = reg("tronco_quiu", new PillarBlock(
			AbstractBlock.Settings.create().mapColor(MapColor.BROWN).strength(2.0f).sounds(BlockSoundGroup.WOOD)));
	public static final Block TRONCO_PRIMITIVO = reg("tronco_primitivo", new PillarBlock(
			AbstractBlock.Settings.create().mapColor(MapColor.BROWN).strength(2.0f).sounds(BlockSoundGroup.WOOD)));
	public static final Block PIZARRA_QUIU = reg("pizarra_quiu",
			new PillarBlock(piedra(3.0f, BlockSoundGroup.DEEPSLATE, MapColor.DEEPSLATE_GRAY)));
	public static final Block ADOQUIN_QUIU = reg("adoquin_quiu",
			new Block(piedra(3.5f, BlockSoundGroup.DEEPSLATE, MapColor.DEEPSLATE_GRAY)));
	public static final Block LOSAS_QUIU = reg("losas_quiu",
			new Block(piedra(3.5f, BlockSoundGroup.DEEPSLATE, MapColor.DEEPSLATE_GRAY)));
	public static final Block BASALTO_QUIU = reg("basalto_quiu",
			new Block(piedra(1.25f, BlockSoundGroup.STONE, MapColor.BLACK)));
	public static final Block TOBA_QUIU = reg("toba_quiu",
			new Block(piedra(1.5f, BlockSoundGroup.TUFF, MapColor.STONE_GRAY)));
	/** Grieta cerrada: no se rompe ni con explosiones. */
	public static final Block SELLO_QUIU = reg("sello_quiu", new Block(
			AbstractBlock.Settings.create().mapColor(MapColor.BLACK).strength(-1.0f, 3600000.0f).dropsNothing()));
	public static final Block PORTAL_QUIU = reg("portal_quiu", new PortalQuiuBlock(
			AbstractBlock.Settings.create().mapColor(MapColor.BLACK).strength(-1.0f, 3600000.0f).noCollision().dropsNothing()));
	public static final Block CUERDA_QUIU = reg("cuerda_quiu", new CuerdaQuiuBlock(
			AbstractBlock.Settings.create().mapColor(MapColor.BROWN).strength(0.4f).noCollision().nonOpaque().sounds(BlockSoundGroup.WOOL)));
	public static final Block BIOXITE_ORE = reg("bioxite_ore", new Block(
			AbstractBlock.Settings.create().mapColor(MapColor.STONE_GRAY).strength(4.0f, 6.0f).requiresTool().sounds(BlockSoundGroup.STONE)));

	public static final Item BIOXITE = Registry.register(Registries.ITEM, Identifier.of(QuiuMod.ID, "bioxite"), new Item(new Item.Settings()));

	private static <T extends Block> T reg(String name, T block) {
		Identifier id = Identifier.of(QuiuMod.ID, name);
		Registry.register(Registries.BLOCK, id, block);
		Registry.register(Registries.ITEM, id, new BlockItem(block, new Item.Settings()));
		return block;
	}

	public static void init() {}

	/** La oscuridad del fondo del hoyo: al caer en ella entras al Centro de Quiu (y la del techo te saca). */
	public static class PortalQuiuBlock extends Block {
		public PortalQuiuBlock(AbstractBlock.Settings settings) { super(settings); }

		@Override
		public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
			return VoxelShapes.fullCube();
		}

		@Override
		public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
			if (!world.isClient && entity instanceof ServerPlayerEntity player) {
				HoyoQuiu.pedirViaje(player);
			}
		}
	}

	/** Cuerda: se trepa. */
	public static class CuerdaQuiuBlock extends Block {
		private static final VoxelShape FORMA = Block.createCuboidShape(6, 0, 6, 10, 16, 10);
		public CuerdaQuiuBlock(AbstractBlock.Settings settings) { super(settings); }

		@Override
		public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
			return FORMA;
		}
	}
}
