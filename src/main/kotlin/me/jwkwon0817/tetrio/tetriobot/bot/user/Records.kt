package me.jwkwon0817.tetrio.tetriobot.bot.user

import me.jwkwon0817.tetrio.tetriobot.database.UserEntity
import me.jwkwon0817.tetrio.tetriobot.database.UserService
import me.jwkwon0817.tetrio.tetriobot.global.api.UserApi
import me.jwkwon0817.tetrio.tetriobot.global.exceptions.UserNotFoundException
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.utils.TimeFormat
import org.springframework.stereotype.Component
import java.awt.Color
import java.time.Instant
import java.time.format.DateTimeFormatter


@Component("userRecords")
class Records(
	private val userService: UserService
) : ListenerAdapter() {
	override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
		when (event.name) {
			"기록" -> {
				val userToSearch = event.getOption("유저")?.asUser ?: event.user

				val foundUser: UserEntity?

				try {
					foundUser = userService.getUser(userToSearch.id)
				} catch (e: UserNotFoundException) {
					val embed = EmbedBuilder()
						.setTitle("🔍 기록 명령어 사용법")
						.addField("사용법", "!기록 [Tetrio ID]", false)
						.setColor(Color.GREEN)
						.build()

					event.replyEmbeds(embed).queue()
					return
				}

				val (embed, buttonList) = getRecords(foundUser.tetrioId)

				if (buttonList.isEmpty()) {
					event.replyEmbeds(embed).queue()
				} else {
					event.replyEmbeds(embed).addActionRow(buttonList).queue()
				}
			}
		}
	}

	override fun onMessageReceived(event: MessageReceivedEvent) {
		if (event.author.isBot) return

		val message = event.message.contentRaw

		if (message.startsWith("!기록")) {
			if (message.split(" ").size == 1) {
				// 사용법
				val embed = EmbedBuilder()
					.setTitle("❌ 사용법")
					.setDescription("`!기록 [Tetrio ID]`")
					.setColor(Color.RED)
					.build()

				event.message.replyEmbeds(embed).mentionRepliedUser(false).queue()
				return
			}

			val userToSearch = message.split(" ")[1].lowercase()

			val (embed, buttonList) = getRecords(userToSearch)

			if (buttonList.isEmpty()) {
				event.message.replyEmbeds(embed).mentionRepliedUser(false).queue()
			} else {
				event.message.replyEmbeds(embed).setActionRow(buttonList).mentionRepliedUser(false).queue()
			}
		}
	}

	fun toTimestamp(ts: String): String {
		val timeFormat = TimeFormat.DATE_TIME_SHORT

		val formatter = DateTimeFormatter.ISO_INSTANT
		val instant = Instant.from(formatter.parse(ts))
		val timestamp = instant.toEpochMilli()

		return timeFormat.format(timestamp)
	}

	fun convertSecondsToMinutesAndSeconds(totalSeconds: Long): Pair<Long, Long> {
		val minutes = totalSeconds / 60
		val seconds = totalSeconds % 60
		return Pair(minutes, seconds)
	}

	fun getRecords(tetrioId: String): Pair<MessageEmbed, MutableList<Button>> {
		val userRecordsData = UserApi.getUserRecords(tetrioId)

		if (userRecordsData["success"] == false) {
			val embed = EmbedBuilder()
				.setTitle("❌ Tetrio ID를 찾을 수 없음")
				.setDescription("Tetrio ID를 찾을 수 없습니다")
				.addField("Tetrio ID", tetrioId.uppercase(), true)
				.setColor(Color.RED)
				.build()

			return Pair(embed, mutableListOf())
		}

		val recordsData = (userRecordsData["data"] as Map<*, *>?)?.get("records") as Map<*, *>

		val records40l = (recordsData["40l"] as Map<*, *>?)?.get("record") as Map<*, *>?
		val recordsBlitz = (recordsData["blitz"] as Map<*, *>?)?.get("record") as Map<*, *>?
		val recordsZen = (userRecordsData["data"] as Map<*, *>?)?.get("zen") as Map<*, *>?

		val replayLink = "https://tetr.io/#r"

		val defaultEmbed = EmbedBuilder()
			.setTitle("📜 **${tetrioId.uppercase()}**의 기록")
			.setColor(Color.GREEN)

		var count = 0

		val buttonList = mutableListOf<Button>()

		if (records40l != null) {
			val finalTime =
				Math.round(
					((records40l["endcontext"] as Map<*, *>?)?.get("finalTime").toString()).toDouble() / 1000 * 100
				) / 100.0

			val (minutes, seconds) = convertSecondsToMinutesAndSeconds(finalTime.toLong())

			defaultEmbed
				.addField("", "**40 Lines**", false)
				.addField("기록 갱신일", toTimestamp(records40l["ts"] as String), true)
				.addField("기록", "${minutes}분 ${seconds}초", true)

			val button = Button.link("${replayLink}:${records40l["replayid"]}", "40 Lines 리플레이")

			buttonList.add(button)

			count += 1
		}

		if (recordsBlitz != null) {
			defaultEmbed
				.addField("", "**Blitz**", false)
				.addField("기록 갱신일", toTimestamp(recordsBlitz["ts"] as String), true)
				.addField("기록", (recordsBlitz["endcontext"] as Map<*, *>?)?.get("finalTime").toString(), true)

			val button = Button.link("${replayLink}:${recordsBlitz["replayid"]}", "Blitz 리플레이")

			buttonList.add(button)

			count += 1

		}

		if (recordsZen != null) {
			defaultEmbed
				.addField("", "**Zen**", false)
				.addField("레벨", "${recordsZen["level"]?.toString()} 레벨", true)
				.addField("점수", "${recordsZen["score"]?.toString()} 점", true)

			count += 1
		}

		if (count == 0) {
			defaultEmbed
				.addField("", "기록이 없습니다", false)
				.setColor(Color.RED)
		}

		return Pair(defaultEmbed.build(), buttonList)
	}
}