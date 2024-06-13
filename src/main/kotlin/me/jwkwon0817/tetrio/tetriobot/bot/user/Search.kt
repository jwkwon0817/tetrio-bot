package me.jwkwon0817.tetrio.tetriobot.bot.user

import me.jwkwon0817.tetrio.tetriobot.database.UserEntity
import me.jwkwon0817.tetrio.tetriobot.database.UserService
import me.jwkwon0817.tetrio.tetriobot.global.api.TetrioApi
import me.jwkwon0817.tetrio.tetriobot.global.api.UserApi
import me.jwkwon0817.tetrio.tetriobot.global.exceptions.UserNotFoundException
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.utils.TimeFormat
import org.springframework.stereotype.Component
import java.awt.Color
import java.time.Instant
import java.time.format.DateTimeFormatter


@Component("userSearch")
class Search(
	private val userService: UserService
) : ListenerAdapter() {
	override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
		when (event.name) {
			"검색" -> {
				val userToSearch = event.getOption("유저")?.asUser ?: event.user
				val dataForm = event.getOption("정보형태")?.asString ?: "간략"

				val userSearchData = UserApi.getUserSearch(userToSearch.id)
				val data = userSearchData["data"] as? Map<*, *>?

				if (data == null) {
					val embed = EmbedBuilder()
						.setTitle("❌ Tetrio ID를 찾을 수 없음")
						.setDescription("Tetrio ID를 찾을 수 없습니다")
						.addField("등록 방법", "`Config > Account > Connections > Discord`", false)
						.setColor(Color.RED)
						.build()

					event.replyEmbeds(embed).queue()
				}

				var isRegistered = true

				val username = (data?.get("user") as Map<*, *>?)?.get("username") as String

				var foundUser: UserEntity? = null

				try {
					foundUser = userService.getUser(userToSearch.id)
				} catch (e: UserNotFoundException) {
					isRegistered = false
				}

				val embed = searchUser(username, dataForm, true, isRegistered, foundUser, userToSearch)

				event.replyEmbeds(embed).queue()
			}
		}
	}

	override fun onMessageReceived(event: MessageReceivedEvent) {
		if (event.author.isBot) return

		val message = event.message.contentRaw

		if (message.startsWith("!검색")) {
			var username = ""
			var dataForm: String

			if (message.split(" ").size == 1) {
				val embed = EmbedBuilder()
					.setTitle("🔍 검색 명령어 사용법")
					.addField("사용법", "`!검색 [Tetrio ID] [정보 형태]`", false)
					.addField("정보 형태", "**`상세` 또는 `간략`** (기본값: `간략`)", false)
					.setColor(Color.GREEN)
					.build()

				event.message.replyEmbeds(embed).mentionRepliedUser(false).queue()
				return
			}

			try {
				username = message.split(" ")[1].lowercase()
				dataForm = message.split(" ")[2]
			} catch (e: IndexOutOfBoundsException) {
				dataForm = "간략"
			}

			if (dataForm != "상세" && dataForm != "간략") {
				event.channel.sendMessage("정보 형태는 `상세` 또는 `간략`만 가능합니다.").queue()
				return
			}

			val userSearch = UserApi.getUserInfo(username)

			val data = userSearch["data"] as? Map<*, *>?

			if (data == null) {
				val embed = EmbedBuilder()
					.setTitle("❌ Tetrio ID를 찾을 수 없음")
					.setDescription("Tetrio ID를 찾을 수 없습니다")
					.addField("Tetrio ID", username, true)
					.setColor(Color.RED)
					.build()

				event.message.replyEmbeds(embed).mentionRepliedUser(false).queue()
				return
			}

			val embed = searchUser(username, dataForm, false, null, null, null)

			event.message.replyEmbeds(embed).mentionRepliedUser(false).queue()
		}
	}

	fun searchUser(
		username: String,
		dataForm: String,
		isSlashCommand: Boolean,
		isRegistered: Boolean?,
		foundUser: UserEntity?,
		userToSearch: User?
	): MessageEmbed {


		val userData = UserApi.getUserInfo(username)
		val userInfo = (userData["data"] as Map<*, *>?)?.get("user") as Map<*, *>?
		val leagueInfo = userInfo?.get("league") as Map<*, *>

		val gamesWon = leagueInfo["gameswon"] as Int
		val gamesPlayed = leagueInfo["gamesplayed"] as Int

		val ratio: Double = if (gamesPlayed == 0) {
			0.0
		} else {
			Math.round((gamesWon.toDouble() / gamesPlayed.toDouble()) * 100) / 100.0
		}

		var rank = leagueInfo["rank"] as String

		if (rank == "z") {
			rank = "Unranked"
		}

		val rating = (leagueInfo["rating"].toString().toDouble()).toInt()

		var rankText = "**${rank.uppercase()}** (${rating} TR)"

		if (rating == -1) {
			rankText = rank
		}

		var registeredText = ""

		if (isSlashCommand) {
			registeredText = if (isRegistered!!) {
				if (foundUser!!.tetrioId != username) {
					"등록된 계정과 다름"
				} else {
					"등록됨"
				}
			} else {
				"미등록"
			}
		}

		val timeFormat = TimeFormat.DATE_TIME_SHORT

		val ts = userInfo["ts"].toString()

		val formatter = DateTimeFormatter.ISO_INSTANT
		val instant = Instant.from(formatter.parse(ts))
		val timestamp = instant.toEpochMilli()

		val defaultEmbed = EmbedBuilder()
			.setTitle("🔍 Tetrio ID 검색 결과")
			.addField("Tetrio ID", username.uppercase(), true)

		if (isSlashCommand) {
			defaultEmbed.addField("디스코드 ID", userToSearch!!.asMention, true)
		}

		if (isSlashCommand) {
			defaultEmbed.addField("등록 여부", registeredText, true)
		}
		defaultEmbed
			.addField("APM", leagueInfo["apm"].toString(), true)
			.addField("PPS", leagueInfo["pps"].toString(), true)
			.addField("VS", leagueInfo["vs"].toString(), true)
			.setColor(0x008000)
			.setImage(TetrioApi.backgroundUrl)
			.setColor(Color.GREEN)

		if (dataForm == "간략") {
			val embed = defaultEmbed
				.addField("게임수", "$gamesWon / $gamesPlayed (${ratio}%)", true)
				.addField("랭크", rankText, true)
				.setColor(0x008000)
				.setImage(TetrioApi.backgroundUrl)
				.setColor(Color.GREEN)
				.build()

			return embed
		} else {
			val badges = userInfo["badges"] as List<*>

			var badgesText: String

			val badgesTextList = mutableListOf<String>()

			badges.forEach {

				badgesTextList.add("**${(it as Map<*, *>?)?.get("id")}**")
			}

			badgesText = badgesTextList.joinToString(", ")

			if (badgesText == "") {
				badgesText = "없음"
			}

			val xp = userInfo["xp"] as Int

			val gameTime = Math.round((userInfo["gametime"] as Double / 3600) * 100) / 100.0


			val country = userInfo["country"] as String

			val glicko = Math.round((leagueInfo["glicko"] ?: 0.0) as Double * 100) / 100.0

			val bestRank = (leagueInfo["bestrank"] as String?)?.uppercase() ?: "Unranked"

			val friendCount = userInfo["friend_count"] as Int

			val standing = leagueInfo["standing"] as Int
			val standingLocal = leagueInfo["standing_local"] as Int

			val embed = defaultEmbed
				.addField("게임수", "$gamesWon / $gamesPlayed (${ratio}%)", true)
				.addField("랭크", rankText, true)
				.addField("계정 생성일", timeFormat.format(timestamp), true)
				.addField("뱃지", badgesText, true)
				.addField("XP", xp.toString(), true)
				.addField("게임 시간", "$gameTime 시간", true)
				.addField("국가", country, true)
				.addField("Glicko", glicko.toString(), true)
				.addField("최고 랭크", bestRank, true)
				.addField("친구 수", friendCount.toString(), true)
				.addField("전체 순위", standing.toString(), true)
				.addField("지역 순위", standingLocal.toString(), true)
				.setColor(0x008000)
				.setImage(TetrioApi.backgroundUrl)
				.setColor(Color.GREEN)
				.build()

			return embed
		}
	}
}