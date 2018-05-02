/*
 *
 * RealisticNametags
 * Copyright (C) 2018 Martijn Heil
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.martijn_heil.realisticnametags

import com.comphenix.packetwrapper.WrapperPlayServerScoreboardTeam
import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.ProtocolManager
import com.comphenix.protocol.events.PacketContainer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.Team
import java.util.*

class RealisticNametags : JavaPlugin() {
    private lateinit var protocolManager: ProtocolManager
    private val TEAM_HIDDEN_NAME = "name_hidden"
    private val distance: Double = 10.0 // Distance in metres, or blocks if you will.
    private lateinit var team_hidden: Team
    private val shownPlayers = HashMap<Player, MutableList<Player>>()

    override fun onEnable() {
        this.protocolManager = ProtocolLibrary.getProtocolManager()
        server.scoreboardManager.mainScoreboard.getTeam(TEAM_HIDDEN_NAME)?.unregister()
        team_hidden = server.scoreboardManager.mainScoreboard.registerNewTeam(TEAM_HIDDEN_NAME)
        team_hidden.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OWN_TEAM)
        server.onlinePlayers.forEach { team_hidden.addEntry(it.name); this.shownPlayers[it] = ArrayList() }

        server.scheduler.scheduleSyncRepeatingTask(this, {
            server.onlinePlayers.forEach { observer ->
                val toBeShown = ArrayList<Player>()
                val toBeHidden = ArrayList<Player>()
                server.onlinePlayers.forEach { target ->
                    if(observer != target &&
                            observer.location.distance(target.location) <= distance &&
                            observer.hasLineOfSight(target)) { // make sure it is shown
                        if(!shownPlayers[observer]!!.contains(target)) toBeShown.add(target)
                    } else { // make sure it is hidden
                        if(shownPlayers[observer]!!.contains(target)) toBeHidden.add(target)
                    }
                }
                hideNamesFor(observer, toBeHidden)
                toBeHidden.forEach { shownPlayers[observer]!!.remove(it) }
                showNamesFor(observer, toBeShown)
                toBeShown.forEach { shownPlayers[observer]!!.add(it) }
            }
        }, 0, 1)

        server.pluginManager.registerEvents(object : Listener {
            @EventHandler(ignoreCancelled = true)
            fun onPlayerJoin(e: PlayerJoinEvent) {
                team_hidden.addEntry(e.player.name)
                shownPlayers[e.player] = ArrayList()
            }
        }, this)
    }

    override fun onDisable() {
        shownPlayers.clear()
    }

    private fun showNamesFor(observer: Player, targets: List<Player>) {
        if(targets.isEmpty()) return
        val packet = WrapperPlayServerScoreboardTeam(PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM))
        packet.name = TEAM_HIDDEN_NAME
        packet.mode = 4 // mode 4: remove players from team
        packet.players = targets.map { it.name }
        packet.sendPacket(observer)
    }

    private fun hideNamesFor(observer: Player, targets: List<Player>) {
        if(targets.isEmpty()) return
        val packet = WrapperPlayServerScoreboardTeam(PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM))
        packet.name = TEAM_HIDDEN_NAME
        packet.mode = 3  // mode 3: add players to team
        packet.players = targets.map { it.name }
        packet.sendPacket(observer)
    }
}