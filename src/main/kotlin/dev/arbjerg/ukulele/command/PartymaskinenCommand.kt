package dev.arbjerg.ukulele.command

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.arbjerg.ukulele.audio.Player
import dev.arbjerg.ukulele.audio.PlayerRegistry
import dev.arbjerg.ukulele.features.HelpContext
import dev.arbjerg.ukulele.jda.Command
import dev.arbjerg.ukulele.jda.CommandContext
import net.dv8tion.jda.api.Permission
import org.springframework.stereotype.Component

@Component
class PartymaskinenCommand(
        val players: PlayerRegistry,
        val apm: AudioPlayerManager
) : Command("pm", "partymaskinen") {
    override suspend fun CommandContext.invoke() {
        if (!ensureVoiceChannel()) return
        val identifier = "http://cast.partymaskinen.se/" + argumentText;
        apm.loadItem(identifier, Loader(this, player, identifier))
    }

    fun CommandContext.ensureVoiceChannel(): Boolean {
        val ourVc = guild.selfMember.voiceState?.channel
        val theirVc = invoker.voiceState?.channel

        if (ourVc == null && theirVc == null) {
            reply("You need to be in a voice channel")
            return false
        }

        if (ourVc != theirVc && theirVc != null)  {
            val canTalk = selfMember.hasPermission(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK)
            if (!canTalk) {
                reply("I need permission to connect and speak in ${theirVc.name}")
                return false
            }

            guild.audioManager.openAudioConnection(theirVc)
            guild.audioManager.sendingHandler = player
            return true
        }

        return ourVc != null
    }

    class Loader(
            private val ctx: CommandContext,
            private val player: Player,
            private val identifier: String
    ) : AudioLoadResultHandler {
        override fun trackLoaded(track: AudioTrack) {
            val started = player.add(track)
            val dj = track.identifier.replace("http://cast.partymaskinen.se/", "")
            if (started) {
                ctx.reply("Started playing http://cast.partymaskinen.se/ ${dj}")
            } else {
                ctx.reply("Added http://cast.partymaskinen.se/ ${dj}")
            }
        }

        override fun playlistLoaded(playlist: AudioPlaylist) {
            player.add(*playlist.tracks.toTypedArray())
            ctx.reply("Added `${playlist.tracks.size}` tracks from `${playlist.name}`")
        }

        override fun noMatches() {
            val dj = identifier.replace("http://cast.partymaskinen.se/", "")
            ctx.reply("http://cast.partymaskinen.se/ ${dj} is not currently broadcasting")
        }

        override fun loadFailed(exception: FriendlyException) {
            ctx.handleException(exception)
        }
    }

    override fun HelpContext.provideHelp() {
        addUsage("<dj>")
        addDescription("Add the given http://cast.partymaskinen.se/ DJ to the queue")
    }
}
