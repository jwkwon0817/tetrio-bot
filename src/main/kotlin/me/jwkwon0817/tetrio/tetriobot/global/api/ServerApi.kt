package me.jwkwon0817.tetrio.tetriobot.global.api

object ServerApi {
	fun getServerStatistics() =
		TetrioApi.toObject(TetrioApi.get("${TetrioApi.baseUrl}/general/stats"))

	fun getServerActivity() =
		TetrioApi.toObject(TetrioApi.get("${TetrioApi.baseUrl}/general/activity"))
}