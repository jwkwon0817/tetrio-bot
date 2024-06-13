package me.jwkwon0817.tetrio.tetriobot.bot.user

import me.jwkwon0817.tetrio.tetriobot.database.UserService
import me.jwkwon0817.tetrio.tetriobot.global.api.TetrioApi
import me.jwkwon0817.tetrio.tetriobot.global.api.UserApi
import me.jwkwon0817.tetrio.tetriobot.global.exceptions.UserNotFoundException
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.buttons.Button
import org.springframework.stereotype.Component
import java.awt.Color


@Component("userRegister")
class Register(
	private val userService: UserService
) : ListenerAdapter() {
	override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
		when (event.name) {
			"등록" -> {
				val (embed, buttonList) = register(event.user)

				if (buttonList.isEmpty()) {
					event.replyEmbeds(embed).queue()
					return
				}

				event.deferReply().queue {
					it.sendMessageEmbeds(embed)
						.addActionRow(buttonList)
						.queue()
				}
			}

			"등록해제" -> {
				val embed = unRegister(event.user)

				event.replyEmbeds(embed).queue()
			}
		}
	}

	override fun onMessageReceived(event: MessageReceivedEvent) {
		if (event.author.isBot) return

		val message = event.message.contentRaw

		if (message.startsWith("!등록해제")) {
			val embed = unRegister(event.author)

			event.message.replyEmbeds(embed).mentionRepliedUser(false).queue()
		} else if (message.startsWith("!등록")) {
			val (embed, buttonList) = register(event.author)

			if (buttonList.isEmpty()) {
				event.message.replyEmbeds(embed).mentionRepliedUser(false).queue()
				return
			}


			event.message.replyEmbeds(embed).setActionRow(buttonList).mentionRepliedUser(false).queue()
		} else if (message.startsWith("!등록해제")) {
			val embed = unRegister(event.author)

			event.message.replyEmbeds(embed).mentionRepliedUser(false).queue()
		}
	}

	override fun onButtonInteraction(event: ButtonInteractionEvent) {
		val componentId = event.componentId

		if (componentId.startsWith("register-")) {
			val id = event.componentId.split("-")[2]
			val username = event.componentId.split("-").last()

			if (event.user.id != id) {
				val embed = EmbedBuilder()
					.setTitle("❌ 잘못된 사용자")
					.setDescription("잘못된 사용자입니다")
					.setColor(Color.RED)
					.build()

				event.replyEmbeds(embed).setEphemeral(true).queue()
				return
			}

			if (event.componentId.startsWith("register-yes-")) {
				val embed = EmbedBuilder()
					.setTitle("✅ 등록 완료")
					.setDescription("등록이 완료되었습니다!")
					.addField("Tetrio ID", username, true)
					.addField("디스코드 ID", event.user.asMention, true)
					.setColor(Color.GREEN)
					.build()

				event.channel.editMessageEmbedsById(event.messageId, embed).setComponents(emptyList()).queue {
					userService.setUser(id, username)
				}
			} else if (event.componentId.startsWith("register-no-")) {
				val embed = EmbedBuilder()
					.setTitle("❌ 등록 취소")
					.setDescription("등록이 취소되었습니다")
					.addField("Tetrio ID", username, true)
					.addField("디스코드 ID", event.user.asMention, true)
					.setColor(Color.RED)
					.build()

				event.channel.editMessageEmbedsById(event.messageId, embed).setComponents(emptyList()).queue()
			}
		}
	}

	fun register(user: User): Pair<MessageEmbed, List<Button>> {
		val discordId = user.id

		val userSearchData = UserApi.getUserSearch(discordId)
		val userData = (userSearchData["data"] as? Map<*, *>)?.get("user") as? Map<*, *>

		val username = userData?.get("username") as? String

		if (username == null) {
			val embed = EmbedBuilder()
				.setTitle("❌ Tetrio ID를 찾을 수 없음")
				.setDescription("Tetrio ID를 찾을 수 없습니다")
				.addField("등록 방법", "`Config > Account > Connections > Discord`", false)
				.setColor(Color.RED)
				.build()

			return Pair(embed, emptyList())
		}

		val userInfoData = UserApi.getUserInfo(username)
		val userInfo = (userInfoData["data"] as? Map<*, *>)?.get("user") as? Map<*, *>
		val leagueInfo = userInfo?.get("league") as? Map<*, *>

		val rank = leagueInfo?.get("rank") as? String

		var tetrioId: String
		var title: String

		try {
			val foundUser = userService.getUser(discordId)

			if (foundUser.tetrioId == username) {
				val embed = EmbedBuilder()
					.setTitle("❌ 이미 등록된 계정")
					.setDescription("이미 등록된 계정입니다")
					.addField("Tetrio ID", username.uppercase(), true)
					.addField("디스코드 ID", user.asMention, true)
					.setColor(Color.RED)
					.build()

				return Pair(embed, emptyList())
			}

			tetrioId = "${foundUser.tetrioId.uppercase()} -> ${username.uppercase()}"
			title = "✅ 이 계정으로 바꾸는 것이 맞나요?"
		} catch (e: UserNotFoundException) {
			tetrioId = username.uppercase()
			title = "✅ 이 계정이 맞나요?"
		}

		val embed = EmbedBuilder()
			.setTitle(title)
			.addField("Tetrio ID", tetrioId, true)
			.addField("랭크", if (rank != "z") rank!!.uppercase() else "Unranked", false)
			.setThumbnail(TetrioApi.imageUrl)
			.setImage(TetrioApi.backgroundUrl)
			.setColor(0x008000)
			.build()

		val yesButton = Button.success("register-yes-${discordId}-${username}", "✅ 맞아요")
		val noButton = Button.danger("register-no-${discordId}-${username}", "❌ 아니에요")

		return Pair(embed, listOf(yesButton, noButton))
	}

	fun unRegister(user: User): MessageEmbed {
		val discordId = user.id

		try {
			userService.getUser(discordId)

			userService.deleteUser(discordId)

			val embed = EmbedBuilder()
				.setTitle("✅ 등록 해제 완료")
				.setDescription("등록 해제가 완료되었습니다")
				.addField("디스코드 ID", user.asMention, true)
				.setColor(Color.GREEN)
				.build()
			return embed

		} catch (e: UserNotFoundException) {
			val embed = EmbedBuilder()
				.setTitle("❌ 등록된 계정 없음")
				.setDescription("등록된 계정이 없습니다")
				.addField("디스코드 ID", user.asMention, true)
				.setColor(Color.RED)
				.build()

			return embed
		}
	}
}