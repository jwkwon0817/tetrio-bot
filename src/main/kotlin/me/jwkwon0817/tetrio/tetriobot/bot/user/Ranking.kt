package me.jwkwon0817.tetrio.tetriobot.bot.user

import me.jwkwon0817.tetrio.tetriobot.database.UserService
import me.jwkwon0817.tetrio.tetriobot.global.api.UserApi
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.buttons.Button
import org.springframework.stereotype.Component

@Component("userRanking")
class Ranking(
	private val userService: UserService
) : ListenerAdapter() {
	override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
		if (event.name == "순위") {
			val standard = event.getOption("기준")?.asString ?: "rating"

			val userSize = userService.getUsers().size

			val embed = EmbedBuilder()
				.setTitle("🏆 순위")
				.setDescription("Tetrio ID를 등록한 유저들의 순위입니다")
				.setColor(0x00FF00)
				.setFooter("현재 페이지: 1 / ${userSize / 5 + 1}")

			val nextButton = Button.primary("ranking-next-${event.user.id}", "다음")

			event.replyEmbeds(embed.build()).setActionRow(nextButton).queue {
				val users = parseUsers(standard)

				paginate(users, 5, 1).forEachIndexed { index, user ->
					embed.addField(
						"data: ${user["data"]}",
						"${index + 1}. ${user["tetrioId"]} (<@${user["discordId"]}>)",
						false
					)

					println("${index + 1}. ${user["tetrioId"]} (<@${user["discordId"]}>), data: ${user["data"]}")
				}

				event.channel.editMessageEmbedsById(it.id, embed.build()).setActionRow(nextButton).queue(
					{
						println("Success")
					},
					{
						println(it.stackTraceToString())
					}
				)
			}
		}
	}

	override fun onButtonInteraction(event: ButtonInteractionEvent) {
		val componentId = event.componentId

		if (componentId.startsWith("ranking")) {
			val discordId = componentId.split("-")[2]

			if (discordId != event.user.id) {
				val embed = EmbedBuilder()
					.setTitle("❌ 오류")
					.setDescription("다른 유저의 명령어는 사용할 수 없습니다")
					.setColor(0xFF0000)

				event.replyEmbeds(embed.build()).setEphemeral(true).queue()

				return
			}

			val pageInfo = event.message.embeds[0].footer?.text?.split(": ")?.get(1)?.split(" / ")

			var currentPage = pageInfo?.get(0)?.toInt() ?: 1
			val maxPage = pageInfo?.get(1)?.toInt() ?: 1

			if (componentId.startsWith("ranking-next-")) {
				currentPage += 1

				val users = parseUsers("40l")

				val embed = EmbedBuilder()
					.setTitle("🏆 순위")
					.setDescription("Tetrio ID를 등록한 유저들의 순위입니다")
					.setColor(0x00FF00)
					.setFooter("현재 페이지: $currentPage / ${users.size / 5 + 1}")

				paginate(users, 5, currentPage).forEachIndexed { index, user ->
					embed.addField(
						"data: ${user["data"]}",
						"${index + 1 + (currentPage - 1) * 5}. ${user["tetrioId"]} (<@${user["discordId"]}>)",
						false
					)
				}

				val nextButton = Button.primary("ranking-next-${event.user.id}", "다음")
				val prevButton = Button.primary("ranking-prev-${event.user.id}", "이전")

				if (currentPage == maxPage) {
					event.channel.editMessageEmbedsById(event.messageId, embed.build()).setActionRow(prevButton).queue()
				} else {
					event.channel.editMessageEmbedsById(event.messageId, embed.build())
						.setActionRow(prevButton, nextButton).queue()
				}

			} else if (componentId.startsWith("ranking-prev-")) {
				currentPage -= 1

				val users = parseUsers("40l")

				val embed = EmbedBuilder()
					.setTitle("🏆 순위")
					.setDescription("Tetrio ID를 등록한 유저들의 순위입니다")
					.setColor(0x00FF00)
					.setFooter("현재 페이지: ${currentPage} / ${users.size / 5 + 1}")

				paginate(users, 5, currentPage).forEachIndexed { index, user ->
					embed.addField(
						"data: ${user["data"]}",
						"${index + 1 + (currentPage - 1) * 5}. ${user["tetrioId"]} (<@${user["discordId"]}>)",
						false
					)
				}

				val nextButton = Button.primary("ranking-next-${event.user.id}", "다음")
				val prevButton = Button.primary("ranking-prev-${event.user.id}", "이전")

				if (currentPage == 1) {
					event.channel.editMessageEmbedsById(event.messageId, embed.build()).setActionRow(nextButton).queue()
				} else {
					event.channel.editMessageEmbedsById(event.messageId, embed.build())
						.setActionRow(prevButton, nextButton).queue()
				}
			}

			event.deferEdit().queue()
		}
	}

	fun parseUsers(standard: String): MutableList<Map<String, *>> {
		val users = mutableListOf<Map<String, *>>()

		userService.getUsers().forEach {
			var standardData: Any? = null
			var data: Any? = null

			val userData = UserApi.getUserRecords(it.tetrioId)

			val info = (userData["data"] as Map<*, *>?)?.get("records") as Map<*, *>?

			when (standard) {
				"40l" -> {
					if ((info?.get("40l") as Map<*, *>?)?.get(
							"record"
						) as Map<*, *>? == null
					) {
						standardData = 0.0
						data = 0.0
					} else {
						val info40l =
							((info?.get("40l") as Map<*, *>?)?.get(
								"record"
							) as Map<*, *>?)?.get("endcontext") as Map<*, *>?

						standardData = info40l?.get("finalTime") as Double
						data = Math.round(standardData * 100) / 100.0
					}
				}

				"blitz" -> {
					if ((info?.get("blitz") as Map<*, *>?)?.get(
							"record"
						) as Map<*, *>? == null
					) {
						standardData = 0
						data = 0
					} else {
						val infoBlitz =
							((info?.get("blitz") as Map<*, *>?)?.get(
								"record"
							) as Map<*, *>?)?.get("endcontext") as Map<*, *>?

						standardData = infoBlitz?.get("finalTime") as Int
						data = standardData
					}
				}

				"zen" -> {
					val infoZen = (userData["data"] as Map<*, *>?)?.get("zen") as Map<*, *>?

					if (infoZen == null) {
						standardData = 0
						data = 0
					} else {
						standardData = infoZen["score"] as Int
						data = standardData
					}
				}

				"rating" -> {
					val userInfo = UserApi.getUserInfo(it.tetrioId)

					val leagueInfo =
						((userInfo["data"] as Map<*, *>?)?.get("user") as Map<*, *>?)?.get("league") as Map<*, *>?

					if (leagueInfo?.get("rating") == -1) {
						standardData = 0.0
						data = 0.0
					} else {
						standardData = leagueInfo?.get("rating") as Double
						data = Math.round(standardData * 100) / 100.0
					}
				}
			}

			users.add(
				mapOf(
					"discordId" to it.discordId,
					"tetrioId" to it.tetrioId,
					"standardData" to standardData,
					"data" to data
				)
			)
		}

		users.sortByDescending { it["standardData"] as Double }


		return users
	}

	fun <T> paginate(list: List<T>, pageSize: Int, pageNumber: Int): List<T> {
		val startIndex = (pageNumber - 1) * pageSize
		if (startIndex >= list.size) {
			return emptyList() // 페이지 범위를 벗어나면 빈 리스트 반환
		}
		val endIndex = kotlin.math.min(startIndex + pageSize, list.size)
		return list.subList(startIndex, endIndex)
	}
}