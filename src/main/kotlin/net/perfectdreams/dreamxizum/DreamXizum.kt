package net.perfectdreams.dreamxizum

import com.github.salomonbrys.kotson.fromJson
import com.okkero.skedule.schedule
import net.perfectdreams.dreamcore.utils.*
import net.perfectdreams.dreamcore.utils.extensions.getStoredMetadata
import net.perfectdreams.dreamxizum.commands.DreamXizumCommand
import net.perfectdreams.dreamxizum.commands.XizumCommand
import net.perfectdreams.dreamxizum.utils.*
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.io.File

class DreamXizum : KotlinPlugin(), Listener {
	companion object {
		const val PREFIX = "§8[§4§lXiz§c§lum§8]§e"
		const val XIZUM_DATA_KEY = "XizumData"
		val INSTANCE get() = Bukkit.getPluginManager().getPlugin("DreamXizum") as DreamXizum
	}

	val arenas = mutableListOf<ArenaXizum>()
	val requestQueue = mutableListOf<RequestQueueEntry>()
	val requests = mutableListOf<XizumRequest>()
	val queue = mutableListOf<Player>()

	override fun softEnable() {
		super.softEnable()

		registerCommand(XizumCommand(this))
		registerCommand(DreamXizumCommand(this))

		registerEvents(this)

		loadArenas()
	}

	override fun softDisable() {
		super.softDisable()
	}

	fun addToQueue(player: Player) {
		queue.add(player)
		checkQueue()
	}

	fun addToRequestQueue(player1: Player, player2: Player) {
		requestQueue.add(RequestQueueEntry(player1, player2))
		checkQueue()
	}

	fun checkQueue() {
		while (requestQueue.isNotEmpty()) { // Enquanto existem entries na lista de requests...
			val request = requestQueue[0]
			val queued1 = request.player1
			val queued2 = request.player2

			if (!queued1.isValid || !queued1.isOnline) {
				requestQueue.remove(request)
				continue
			}

			if (!queued2.isValid || !queued2.isOnline) {
				requestQueue.remove(request)
				continue
			}

			// Vamos pegar a primeira arena disponível para o nosso X1
			// Caso retorne null, cancele a verificação de queue já que não existem mais arenas disponíveis
			val arena = arenas.firstOrNull { it.isReady && it.player1 == null } ?: return

			requestQueue.remove(request)

			// omg todos são válidos??? yay??? não sei :X
			arena.startArena(queued1, queued2)
		}
		while (queue.size >= 2) { // Enquanto existe mais de duas pessoas na fila...
			val queued1 = queue[0]
			val queued2 = queue[1]

			if (!queued1.isValid || !queued1.isOnline) {
				queue.remove(queued1)
				continue
			}

			if (!queued2.isValid || !queued2.isOnline) {
				queue.remove(queued2)
				continue
			}

			// Vamos pegar a primeira arena disponível para o nosso X1
			// Caso retorne null, cancele a verificação de queue já que não existem mais arenas disponíveis
			val arena = arenas.firstOrNull { it.isReady && it.player1 == null } ?: return

			queue.remove(queued1)
			queue.remove(queued2)

			// omg todos são válidos??? yay??? não sei :X
			arena.startArena(queued1, queued2)
		}
	}

	@EventHandler
	fun onClick(e: InventoryClickEvent) {
		val holder = e.inventory?.holder ?: return

		if (holder !is XizumInventoryHolder)
			return

		e.isCancelled = true
		val player = e.whoClicked as Player
		player.closeInventory()
		val data = e.currentItem?.getStoredMetadata(XIZUM_DATA_KEY) ?: return

		if (data == "joinQueue") {
			player.performCommand("xizum fila")
		} else if (data == "invitePlayer") {
			SignGUIUtils.openGUIFor(player, arrayOf("§lEscreva o", "§lnome do", "§lplayer!", "Fulano"), object: SignGUIUtils.SignGUIListener() {
				override fun onSignDone(player: Player, lines: Array<String>) {
					val playerName = lines[3]

					scheduler().schedule(INSTANCE) {
						player.performCommand("xizum convidar ${playerName}")
					}
				}
			})
		}
	}

	@EventHandler
	fun onDeath(e: EntityDamageEvent) {
		if (e.entity is Player) {
			val player = e.entity as Player

			// Vamos verificar se o tal usuário estava em uma arena
			val arena = arenas.firstOrNull { it.player1 == player || it.player2 == player } ?: return

			val killed = 0 >= player.health - e.finalDamage

			if (killed) {
				e.isCancelled = true

				arena.finishArena(player, WinType.KILLED)
				checkQueue()
			}
		}
	}

	@EventHandler
	fun onDisconnect(e: PlayerQuitEvent) {
		// Vamos verificar se o tal usuário estava em uma arena
		val arena = arenas.firstOrNull { it.player1 == e.player || it.player2 == e.player } ?: return

		arena.finishArena(e.player, WinType.DISCONNECTED)
		checkQueue()
	}

	fun saveArenas() {
		val arenasFolder = File(dataFolder, "arenas")

		arenasFolder.deleteRecursively()
		arenasFolder.mkdirs()

		arenas.forEach {
			File(arenasFolder, "${it.name}.json").writeText(DreamUtils.gson.toJson(it))
		}
	}

	fun loadArenas() {
		val arenasFolder = File(dataFolder, "arenas")

		arenasFolder.listFiles().forEach {
			arenas.add(DreamUtils.gson.fromJson(it.readText()))
		}
	}

	fun getArenaByName(name: String): ArenaXizum? {
		return arenas.firstOrNull { it.name == name }
	}
}