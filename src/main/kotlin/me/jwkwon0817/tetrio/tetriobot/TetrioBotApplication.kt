package me.jwkwon0817.tetrio.tetriobot

import ch.qos.logback.classic.Logger
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean

@SpringBootApplication
class TetrioBotApplication

var logger = LoggerFactory.getLogger(TetrioBotApplication::class.java) as Logger

fun main(args: Array<String>) {
	val context = runApplication<TetrioBotApplication>(*args)
	val jdaToken = context.environment.getProperty("jda.token") ?: error("JDA Token is not provided!")
	runBlocking {
		val jda = jdaBuilder(jdaToken, context)

		jda.awaitReady()

		jda.updateCommands().addCommands(
			listOf<CommandData>(
				Commands.slash("등록", "유저를 등록합니다."),
				Commands.slash("등록해제", "등록된 유저를 삭제합니다."),
				Commands.slash("검색", "유저를 검색합니다.")
					.addOptions(
						OptionData(OptionType.USER, "유저", "검색할 유저", false),
						OptionData(OptionType.STRING, "정보형태", "검색할 정보 형태", false)
							.addChoice("상세", "상세")
							.addChoice("간략", "간략")
					),
				Commands.slash("기록", "유저의 기록을 검색합니다.")
					.addOptions(
						OptionData(OptionType.USER, "유저", "검색할 유저", false),
					),
//				Commands.slash("랭크표", "랭크표를 확인합니다."),
//				Commands.slash("순위", "유저의 순위를 검색합니다.")
//					.addOptions(
//						OptionData(OptionType.STRING, "기준", "순위의 기준을 선택합니다.", false)
//							.addChoice("40 Lines", "40l")
//							.addChoice("Blitz", "blitz")
//							.addChoice("Zen", "zen")
//							.addChoice("Tetra League", "rating"),
//						OptionData(OptionType.INTEGER, "페이지", "페이지 번호", false),
//						OptionData(OptionType.USER, "유저", "검색할 유저", false)
//					)
			)
		).queue()

		logger.info("Bot is ready!")
	}
}

@Bean
fun jdaBuilder(
	jdaToken: String, context: ApplicationContext
): JDA {
	return JDABuilder.createDefault(jdaToken)
		.disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE)
		.setBulkDeleteSplittingEnabled(false)
		.setStatus(OnlineStatus.ONLINE)
		.setMemberCachePolicy(MemberCachePolicy.VOICE.or(MemberCachePolicy.OWNER))
		.setChunkingFilter(ChunkingFilter.NONE)
		.disableIntents(GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGE_TYPING)
		.enableIntents(
			GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS,
			GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_EMOJIS_AND_STICKERS,
			GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_MESSAGE_TYPING
		)
		.setLargeThreshold(50)
		.setAutoReconnect(true)
		.addEventListeners(
			context.getBean("userRegister"),
			context.getBean("userSearch"),
			context.getBean("userRecords"),
//			context.getBean("generalRankTable")
		).build()
}