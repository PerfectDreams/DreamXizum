package net.perfectdreams.dreamxizum.utils

import com.okkero.skedule.schedule
import net.perfectdreams.dreamcore.DreamCore
import net.perfectdreams.dreamcore.utils.InstantFirework
import net.perfectdreams.dreamcore.utils.broadcast
import net.perfectdreams.dreamcore.utils.scheduler
import net.perfectdreams.dreamxizum.DreamXizum
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class ArenaXizum(val name: String) {
	var location1: Location? = null
	var location2: Location? = null
	var isReady: Boolean = false

	@Transient
	var player1: Player? = null
	@Transient
	var player2: Player? = null

	fun startArena(player1: Player, player2: Player) {
		broadcast(DreamXizum.PREFIX + " §b${player1.displayName}§e §4§lVS §b${player2.displayName}")

		this.player1 = player1
		this.player2 = player2

		player1.health = 20.0
		player1.foodLevel = 20
		player2.health = 20.0
		player2.foodLevel = 20
		player1.gameMode = GameMode.SURVIVAL
		player2.gameMode = GameMode.SURVIVAL

		// Teletransportar os players para os starting points
		player1.teleport(location1)
		player2.teleport(location2)

		player1.walkSpeed = 0f
		player2.walkSpeed = 0f
		player1.addPotionEffect(PotionEffect(PotionEffectType.JUMP, 100, -5))
		player2.addPotionEffect(PotionEffect(PotionEffectType.JUMP, 100, -5))

		scheduler().schedule(DreamXizum.INSTANCE) {
			for (idx in 5 downTo 1) {
				player1.sendTitle("§a§l$idx", "", 0, 15, 5)
				player2.sendTitle("§a§l$idx", "", 0, 15, 5)
				waitFor(20)
			}
			player1.walkSpeed = 0.2f
			player2.walkSpeed = 0.2f
			player1.sendTitle("§c§lLutem!", "", 0, 15, 5)
			player2.sendTitle("§c§lLutem!", "", 0, 15, 5)

			scheduler().schedule(DreamXizum.INSTANCE) {
				var seconds = 180
				while (true) {
					if (this@ArenaXizum.player1 == null || this@ArenaXizum.player2 == null) {
						return@schedule
					}

					if (0 >= seconds) {
						finishArena(this@ArenaXizum.player2!!, WinType.TIMEOUT)
						return@schedule
					}

					player1.sendMessage(DreamXizum.PREFIX + " §3Faltam §d${seconds} segundos§3 para acabar a partida!")
					player2.sendMessage(DreamXizum.PREFIX + " §3Faltam §d${seconds} segundos§3 para acabar a partida!")

					waitFor(20 * 15)
					seconds -= 15
				}
			}
		}
	}

	fun finishArena(loser: Player, type: WinType) {
		// Okay, ele estava... fazer o que né
		val winner = if (loser == player1) {
			player2
		} else {
			player1
		}!!

		winner.health = 20.0
		winner.foodLevel = 20
		loser.health = 20.0
		loser.foodLevel = 20

		if (type == WinType.DISCONNECTED) {
			broadcast(DreamXizum.PREFIX + " §b${loser.displayName}§e arregou o Xizum contra o §b${winner.displayName}§e!")
		} else if (type == WinType.KILLED) {
			broadcast(DreamXizum.PREFIX + " §b${winner.displayName}§e venceu o Xizum contra o §b${loser.displayName}§e!")
		} else if (type == WinType.TIMEOUT) {
			broadcast(DreamXizum.PREFIX + " §b${winner.displayName}§e e §b${loser.displayName}§e demoraram tanto que a partida do Xizum acabou...")
		}

		winner.sendTitle("§a§lVocê venceu!", "", 10, 80, 10)


		val loserLocation = loser.location

		val head = ItemStack(Material.PLAYER_HEAD, 1)
		val meta = head.itemMeta as SkullMeta
		meta.owner = loser.name
		head.itemMeta = meta
		val item = loserLocation.world.dropItemNaturally(loserLocation, head)

		loser.teleport(DreamCore.dreamConfig.spawn)
		winner.playSound(winner.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)

		InstantFirework.spawn(loserLocation, FireworkEffect.builder()
				.with(FireworkEffect.Type.STAR)
				.withColor(Color.RED)
				.withFade(Color.BLACK)
				.withFlicker()
				.withTrail()
				.build())

		InstantFirework.spawn(winner.location, FireworkEffect.builder()
				.with(FireworkEffect.Type.STAR)
				.withColor(Color.GREEN)
				.withFade(Color.BLACK)
				.withFlicker()
				.withTrail()
				.build())

		scheduler().schedule(DreamXizum.INSTANCE) {
			waitFor(100)
			item.remove()
			winner.teleport(DreamCore.dreamConfig.spawn)
			player1 = null
			player2 = null
		}
	}
}