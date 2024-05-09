package org.nguh.nguhcraft.mixin.server;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.nguh.nguhcraft.server.Discord;
import org.nguh.nguhcraft.server.NetworkHandler;
import org.nguh.nguhcraft.server.ServerUtils;
import org.nguh.nguhcraft.server.accessors.LivingEntityAccessor;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin extends ServerCommonNetworkHandler {
    public ServerPlayNetworkHandlerMixin(MinecraftServer server, ClientConnection connection, ConnectedClientData clientData) {
        super(server, connection, clientData);
    }

    @Unique static private final Text NEEDS_CLIENT_MOD
        = Text.literal("Please install the Nguhcraft client-side mod to play on this server");

    @Shadow public ServerPlayerEntity player;

    @Shadow @Final static Logger LOGGER;

    /** Forward quit message to Discord. */
    @Inject(method = "cleanUp()V", at = @At("HEAD"))
    private void inject$cleanUp(CallbackInfo CI) {
        Discord.BroadcastJoinQuitMessage(player, false);
    }

    /**
    * Prevent players in a hypershot context from using weapons.
    * <p>
    * This should already be prevented client-side, but we check for this
    * here as well just in case.
    */
    @Inject(
        method = "onPlayerInteractItem",
        cancellable = true,
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/server/network/ServerPlayerEntity.updateLastActionTime ()V",
            ordinal = 0,
            shift = At.Shift.AFTER
        )
    )
    private void inject$onPlayerInteractItem(PlayerInteractItemC2SPacket Packet, CallbackInfo CI) {
        if (((LivingEntityAccessor)player).getHypershotContext() != null) {
            LOGGER.warn("Player {} tried to use an item while in hypershot context", player.getDisplayName());
            CI.cancel();
        }
    }

    /**
    * Initial player tick.
    * <p>
    * This is called before doing any other processing on the player. Despite
    * the fact that this is part of the network handler, this is still called
    * on the tick thread.
    */
    @Inject(method = "tick()V", at = @At("HEAD"))
    private void inject$tick(CallbackInfo CI) { ServerUtils.ActOnPlayerTick(player); }

    /**
     * Disconnect on incoming signed chat messages.
     *
     * @author Sirraide
     * @reason The client is patched to never send these.
     */
    @Overwrite
    public void onChatMessage(ChatMessageC2SPacket Packet) {
        disconnect(NEEDS_CLIENT_MOD);
    }

    /**
     * Disconnect on incoming signed commands.
     *
     * @author Sirraide
     * @reason The client is patched to never send these.
     */
    @Overwrite
    public void onChatCommandSigned(ChatCommandSignedC2SPacket Packet) {
        disconnect(NEEDS_CLIENT_MOD);
    }

    /**
     * Handle incoming commands.
     * <p>
     * We need to hijack the command handling logic a bit since an unlinked
     * player should *never* be able to execute commands.
     *
     * @author Sirraide
     * @reason See above.
     */
    @Overwrite
    public void onCommandExecution(@NotNull CommandExecutionC2SPacket Packet) {
        NetworkHandler.HandleCommand((ServerPlayNetworkHandler) (Object) this, Packet.command());
    }

    /**
     * Disconnect if the client tries to establish a session.
     *
     * @author Sirraide
     * @reason The client is patched to never send these.
     */
    @Overwrite
    public void onPlayerSession(PlayerSessionC2SPacket packet) {
        disconnect(NEEDS_CLIENT_MOD);
    }
}
