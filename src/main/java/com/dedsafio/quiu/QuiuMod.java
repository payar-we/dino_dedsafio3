package com.dedsafio.quiu;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuiuMod implements ModInitializer {
	public static final String ID = "quiu";
	public static final Logger LOG = LoggerFactory.getLogger(ID);

	@Override
	public void onInitialize() {
		QuiuBlocks.init();

		ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(e -> {
			e.add(QuiuBlocks.TRONCO_QUIU);
			e.add(QuiuBlocks.TRONCO_PRIMITIVO);
			e.add(QuiuBlocks.PIZARRA_QUIU);
			e.add(QuiuBlocks.ADOQUIN_QUIU);
			e.add(QuiuBlocks.LOSAS_QUIU);
			e.add(QuiuBlocks.BASALTO_QUIU);
			e.add(QuiuBlocks.TOBA_QUIU);
			e.add(QuiuBlocks.SELLO_QUIU);
			e.add(QuiuBlocks.PORTAL_QUIU);
			e.add(QuiuBlocks.CUERDA_QUIU);
			e.add(QuiuBlocks.BIOXITE_ORE);
		});
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(e -> e.add(QuiuBlocks.BIOXITE));

		// El hoyo se construye solo, a 2750 bloques del spawn (como en customgear).
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			try {
				HoyoQuiu.asegurar(server);
			} catch (Exception ex) {
				LOG.error("No se pudo construir el Hoyo de Quiu", ex);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> HoyoQuiu.tick());

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(CommandManager.literal("quiu")
				.requires(src -> src.hasPermissionLevel(2))
				.then(CommandManager.literal("info").executes(ctx -> {
					String t = HoyoQuiu.info(ctx.getSource().getServer());
					ctx.getSource().sendFeedback(() -> Text.literal(t), false);
					return 1;
				}))
				.then(CommandManager.literal("abrir").executes(ctx -> {
					int n = HoyoQuiu.abrir(ctx.getSource().getServer());
					ctx.getSource().sendFeedback(() -> Text.literal(n > 0
						? "La Grieta de Quiu se abrio (" + n + " bloques del sello quitados)."
						: "No encontre el sello (ya esta abierta o el hoyo aun no existe)."), true);
					return n;
				}))
				.then(CommandManager.literal("ir").executes(ctx -> {
					HoyoQuiu.irAlHoyo(ctx.getSource().getPlayerOrThrow());
					return 1;
				}))
				.then(CommandManager.literal("entrar").executes(ctx -> {
					HoyoQuiu.pedirViaje(ctx.getSource().getPlayerOrThrow());
					return 1;
				}))));
	}
}
