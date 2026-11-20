package com.dedsafio.quiu;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PillarBlock;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * El Hoyo de Quiu (la entrada) y los viajes entre el Overworld y el Centro de Quiu.
 *
 * Como en customgear: a 2750 bloques del spawn hay una grieta sellada. Debajo hay un pozo con una
 * rampa en espiral y, en el fondo, la Oscuridad de Quiu: caer en ella te lleva al Centro de Quiu.
 * Alla, una cuerda enorme sube hasta el techo, donde otra Oscuridad te devuelve al hoyo.
 */
public final class HoyoQuiu {
	private HoyoQuiu() {}

	public static final int DISTANCIA = 2750;
	public static final int FONDO = -58;      // y del fondo del pozo
	public static final int R_POZO = 10;      // radio del pozo
	public static final int R_PARED = 13;     // radio exterior de la pared
	public static final int TECHO = 312;      // y donde termina la cuerda del Centro de Quiu

	private static final RegistryKey<World> DINO =
			RegistryKey.of(RegistryKeys.WORLD, Identifier.of("dinodedsafio", "dino_world"));

	private static final ConcurrentLinkedQueue<Runnable> COLA = new ConcurrentLinkedQueue<>();
	private static final Set<UUID> PENDIENTES = new HashSet<>();

	private static final int F = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;

	// ------------------------------------------------------------------ ubicacion

	public static BlockPos centro(ServerWorld ow) {
		Random r = new Random(ow.getSeed() ^ 1364543829L);
		double ang = r.nextDouble() * Math.PI * 2.0;
		BlockPos sp = ow.getSpawnPos();
		int x = (int) Math.round(sp.getX() + Math.cos(ang) * DISTANCIA);
		int z = (int) Math.round(sp.getZ() + Math.sin(ang) * DISTANCIA);
		return new BlockPos(x, 0, z);
	}

	public static String info(MinecraftServer server) {
		BlockPos c = centro(server.getOverworld());
		boolean abierto = ySello(server.getOverworld(), c) == Integer.MIN_VALUE;
		return "Hoyo de Quiu en X=" + c.getX() + " Z=" + c.getZ() + " (fondo y=" + FONDO + "). Grieta: "
				+ (abierto ? "ABIERTA" : "SELLADA") + ". Usa /quiu abrir para abrirla.";
	}

	// ------------------------------------------------------------------ construccion

	private static void poner(ServerWorld w, int x, int y, int z, BlockState s) {
		w.setBlockState(new BlockPos(x, y, z), s, F);
	}

	private static BlockState paredBloque(Random rnd, int y, int ySup) {
		double prof = (ySup - y) / (double) Math.max(1, ySup - FONDO);       // 0 arriba, 1 abajo
		int i = (int) Math.floor((prof * 0.8 + rnd.nextDouble() * 0.4) * 5.0);
		i = Math.max(0, Math.min(4, i));
		switch (i) {
			case 0: return QuiuBlocks.BASALTO_QUIU.getDefaultState();
			case 1: return QuiuBlocks.TOBA_QUIU.getDefaultState();
			case 2: return QuiuBlocks.ADOQUIN_QUIU.getDefaultState();
			case 3: return QuiuBlocks.PIZARRA_QUIU.getDefaultState().with(PillarBlock.AXIS, net.minecraft.util.math.Direction.Axis.Y);
			default: return QuiuBlocks.LOSAS_QUIU.getDefaultState();
		}
	}

	/** Construye el hoyo si aun no existe. */
	public static void asegurar(MinecraftServer server) {
		ServerWorld ow = server.getOverworld();
		BlockPos c = centro(ow);
		int cx = c.getX(), cz = c.getZ();
		ow.getChunk(cx >> 4, cz >> 4);
		if (ow.getBlockState(new BlockPos(cx, FONDO - 1, cz)).isOf(QuiuBlocks.PORTAL_QUIU)) return; // ya existe

		QuiuMod.LOG.info("Construyendo el Hoyo de Quiu en {} {}...", cx, cz);
		int ySup = ow.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, cx, cz);
		Random rnd = new Random(ow.getSeed() ^ 1213159759L);
		BlockState aire = Blocks.AIR.getDefaultState();
		BlockState losas = QuiuBlocks.LOSAS_QUIU.getDefaultState();
		BlockState portal = QuiuBlocks.PORTAL_QUIU.getDefaultState();
		BlockState sello = QuiuBlocks.SELLO_QUIU.getDefaultState();

		for (int dx = -R_PARED; dx <= R_PARED; dx++) {
			for (int dz = -R_PARED; dz <= R_PARED; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d > R_PARED + 0.5) continue;
				int x = cx + dx, z = cz + dz;
				if (d <= R_POZO + 0.5) {
					for (int y = FONDO; y <= ySup + 1; y++) poner(ow, x, y, z, aire);
					poner(ow, x, FONDO - 1, z, d <= R_POZO - 1.5 ? portal : losas);
					poner(ow, x, FONDO - 2, z, losas);
					poner(ow, x, ySup, z, sello);                       // la grieta, cerrada
				} else {
					for (int y = FONDO - 2; y <= ySup + 1; y++) poner(ow, x, y, z, paredBloque(rnd, y, ySup));
				}
			}
		}

		// rampa en espiral (16 bloques de bajada por vuelta, 3.2 de ancho)
		double r0 = 6.4, ancho = 3.2;
		int yIni = ySup - 5;
		double total = (yIni - FONDO) / 16.0 * 2.0 * Math.PI;
		for (double t = 0; t < total; t += 0.025) {
			int y = (int) Math.floor(yIni - t / (2.0 * Math.PI) * 16.0);
			for (double rr = r0; rr <= r0 + ancho; rr += 0.4) {
				int x = (int) Math.floor(cx + 0.5 + rr * Math.cos(t));
				int z = (int) Math.floor(cz + 0.5 + rr * Math.sin(t));
				poner(ow, x, y, z, QuiuBlocks.ADOQUIN_QUIU.getDefaultState());
				for (int h = 1; h <= 3; h++) poner(ow, x, y + h, z, aire);
			}
		}
		QuiuMod.LOG.info("Hoyo de Quiu listo (superficie y={}).", ySup);
	}

	/** y del sello en la columna central, o MIN_VALUE si no hay. */
	private static int ySello(ServerWorld ow, BlockPos c) {
		for (int y = ow.getTopY() - 1; y >= ow.getBottomY(); y--) {
			if (ow.getBlockState(new BlockPos(c.getX(), y, c.getZ())).isOf(QuiuBlocks.SELLO_QUIU)) return y;
		}
		return Integer.MIN_VALUE;
	}

	/** Quita el sello. Devuelve cuantos bloques quito. */
	public static int abrir(MinecraftServer server) {
		ServerWorld ow = server.getOverworld();
		BlockPos c = centro(ow);
		ow.getChunk(c.getX() >> 4, c.getZ() >> 4);
		int y = ySello(ow, c);
		if (y == Integer.MIN_VALUE) return 0;
		int n = 0;
		for (int dx = -R_POZO - 1; dx <= R_POZO + 1; dx++) {
			for (int dz = -R_POZO - 1; dz <= R_POZO + 1; dz++) {
				BlockPos p = new BlockPos(c.getX() + dx, y, c.getZ() + dz);
				if (ow.getBlockState(p).isOf(QuiuBlocks.SELLO_QUIU)) {
					ow.setBlockState(p, Blocks.AIR.getDefaultState(), F);
					n++;
				}
			}
		}
		return n;
	}

	// ------------------------------------------------------------------ viajes

	private static String f(double v) { return String.format(Locale.ROOT, "%.2f", v); }

	private static void tp(ServerPlayerEntity p, String dim, double x, double y, double z) {
		MinecraftServer s = p.getServer();
		if (s == null) return;
		p.fallDistance = 0;
		String u = p.getUuidAsString();
		ServerCommandSource src = s.getCommandSource().withSilent();
		s.getCommandManager().executeWithPrefix(src,
				"execute as " + u + " in " + dim + " run tp @s " + f(x) + " " + f(y) + " " + f(z));
		s.getCommandManager().executeWithPrefix(src, "effect give " + u + " minecraft:slow_falling 8 0 true");
	}

	/** Llamado desde la Oscuridad de Quiu (o /quiu entrar): decide el sentido segun la dimension. */
	public static void pedirViaje(ServerPlayerEntity p) {
		if (!PENDIENTES.add(p.getUuid())) return;
		COLA.add(() -> {
			PENDIENTES.remove(p.getUuid());
			try {
				MinecraftServer s = p.getServer();
				if (s == null) return;
				if (p.getServerWorld().getRegistryKey().equals(DINO)) salirDeQuiu(p, s);
				else entrarAQuiu(p, s);
			} catch (Exception ex) {
				QuiuMod.LOG.error("Error en el viaje a Quiu", ex);
			}
		});
	}

	public static void tick() {
		Runnable r;
		while ((r = COLA.poll()) != null) r.run();
	}

	private static void entrarAQuiu(ServerPlayerEntity p, MinecraftServer s) {
		ServerWorld dino = s.getWorld(DINO);
		if (dino == null) {
			p.sendMessage(Text.literal("La dimension de los dinosaurios no esta cargada."), false);
			return;
		}
		int yg = prepararSalida(dino);
		tp(p, "dinodedsafio:dino_world", 3.5, yg + 1.0, 3.5);
		p.sendMessage(Text.literal("Centro de Quiu").formatted(Formatting.GOLD, Formatting.BOLD), true);
	}

	private static void salirDeQuiu(ServerPlayerEntity p, MinecraftServer s) {
		ServerWorld ow = s.getOverworld();
		BlockPos c = centro(ow);
		int x = c.getX() + R_PARED + 3, z = c.getZ();
		ow.getChunk(x >> 4, z >> 4);
		int y = ow.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
		tp(p, "minecraft:overworld", x + 0.5, y, z + 0.5);
	}

	public static void irAlHoyo(ServerPlayerEntity p) {
		MinecraftServer s = p.getServer();
		if (s == null) return;
		ServerWorld ow = s.getOverworld();
		BlockPos c = centro(ow);
		ow.getChunk(c.getX() >> 4, c.getZ() >> 4);
		int x = c.getX() + R_PARED + 3, z = c.getZ();
		ow.getChunk(x >> 4, z >> 4);
		int y = ow.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
		tp(p, "minecraft:overworld", x + 0.5, y, z + 0.5);
	}

	/**
	 * Centro de Quiu: busca el suelo en (3,3) y deja una cuerda gigante en (0,0) que sube hasta el techo,
	 * donde esta la Oscuridad de Quiu que te devuelve al Overworld. Devuelve la y del suelo.
	 */
	private static int prepararSalida(ServerWorld w) {
		w.getChunk(0, 0);
		int yg = 100;
		for (int y = 195; y >= -60; y--) {
			BlockPos p = new BlockPos(3, y, 3);
			BlockState st = w.getBlockState(p);
			if (!st.isAir() && !st.isIn(BlockTags.LEAVES) && st.getFluidState().isEmpty()
					&& w.getBlockState(p.up()).isAir() && w.getBlockState(p.up(2)).isAir()) {
				yg = y;
				break;
			}
		}
		if (!w.getBlockState(new BlockPos(0, TECHO, 0)).isOf(QuiuBlocks.CUERDA_QUIU)) {
			BlockState cuerda = QuiuBlocks.CUERDA_QUIU.getDefaultState();
			for (int y = yg + 1; y <= TECHO; y++) w.setBlockState(new BlockPos(0, y, 0), cuerda, F);
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					w.setBlockState(new BlockPos(dx, TECHO + 1, dz), QuiuBlocks.PORTAL_QUIU.getDefaultState(), F);
				}
			}
			// base de piedra de pozo para que se vea desde lejos
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					w.setBlockState(new BlockPos(dx, yg, dz), QuiuBlocks.ADOQUIN_QUIU.getDefaultState(), F);
				}
			}
		}
		return yg;
	}
}
