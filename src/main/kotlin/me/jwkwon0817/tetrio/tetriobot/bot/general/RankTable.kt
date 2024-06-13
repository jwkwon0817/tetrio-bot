package me.jwkwon0817.tetrio.tetriobot.bot.general

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component

@Component("generalRankTable")
class RankTable : ListenerAdapter() {
	override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
		if (event.name == "랭크표") {
			val embed = EmbedBuilder()
				.setColor(0x00FF00)
				.setImage("https://i.postimg.cc/wjqhZ8tf/image.webp")
				.build()

			event.replyEmbeds(embed).queue()
		}
	}

	override fun onMessageReceived(event: MessageReceivedEvent) {
		if (event.author.isBot) return

		val message = event.message.contentRaw

		if (message.startsWith("!랭크표")) {
			val embed = EmbedBuilder()
				.setColor(0x00FF00)
				.setImage("https://i.postimg.cc/wjqhZ8tf/image.webp")
				.build()

			event.message.replyEmbeds(embed).mentionRepliedUser(false).queue()
		}
	}
}