package org.nguh.nguhcraft.server

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.Entity
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.packet.CustomPayload
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.TeleportTarget
import org.nguh.nguhcraft.server.accessors.ServerPlayerAccessor
import java.util.*

@JvmStatic
fun BlockEntity.CreateUpdate(Update: (Tag: NbtCompound) -> Unit) : NbtCompound {
    val Tag = NbtCompound()
    Update(Tag)

    // An empty tag prevents deserialisation on the client, so
    // ensure that this is never empty.
    if (Tag.isEmpty) Tag.putBoolean("nguhcraft_ensure_deserialised", true)
    return Tag
}

fun Entity.Teleport(
    ToWorld: ServerWorld,
    OnTopOf: BlockPos,
    SaveLastPos: Boolean = false
) {
    val Vec = Vec3d(OnTopOf.x.toDouble(), OnTopOf.y.toDouble() + 1, OnTopOf.z.toDouble())
    Teleport(TeleportTarget(ToWorld, Vec, Vec3d.ZERO, 0F, 0F, TeleportTarget.NO_OP), SaveLastPos)
}

fun Entity.Teleport(
    ToWorld: ServerWorld,
    To: Vec3d,
    Yaw: Float,
    Pitch: Float,
    SaveLastPos: Boolean = false
) {
    Teleport(TeleportTarget(ToWorld, To, Vec3d.ZERO, Yaw, Pitch, TeleportTarget.NO_OP), SaveLastPos)
}

fun Entity.Teleport(Target: TeleportTarget, SaveLastPos: Boolean) {
    if (SaveLastPos && this is ServerPlayerEntity) SavePositionBeforeTeleport()
    teleportTo(Target)
}

/** Send a packet to every client except one. */
fun MinecraftServer.Broadcast(Except: ServerPlayerEntity, P: CustomPayload) {
    for (Player in playerManager.playerList)
        if (Player != Except)
            ServerPlayNetworking.send(Player, P)
}

/** Send a packet to every client in a world. */
fun MinecraftServer.Broadcast(W: ServerWorld, P: CustomPayload) {
    for (Player in W.players)
        ServerPlayNetworking.send(Player, P)
}

/** Send a packet to every client. */
fun MinecraftServer.Broadcast(P: CustomPayload) {
    for (Player in playerManager.playerList)
        ServerPlayNetworking.send(Player, P)
}

/** Get a player by Name. */
fun MinecraftServer.PlayerByName(Name: String): ServerPlayerEntity? {
    return playerManager.getPlayer(Name)
}

/** Get a player by UUID. */
fun MinecraftServer.PlayerByUUID(ID: String?): ServerPlayerEntity? {
    return try { playerManager.getPlayer(UUID.fromString(ID)) }
    catch (E: RuntimeException) { null }
}

/** Check if a player is a moderator. */
var ServerPlayerEntity.IsModerator
    get() = (this as ServerPlayerAccessor).isModerator
    set(value) { (this as ServerPlayerAccessor).setIsModerator(value) }

/** Save the player’s current position as a teleport target. */
@JvmStatic
fun ServerPlayerEntity.SavePositionBeforeTeleport() {
    (this as ServerPlayerAccessor).lastPositionBeforeTeleport = TeleportTarget(
        this.serverWorld,
        this.pos,
        Vec3d.ZERO,
        this.yaw,
        this.pitch,
        TeleportTarget.NO_OP
    )
}