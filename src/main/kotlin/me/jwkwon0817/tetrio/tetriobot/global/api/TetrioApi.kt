package me.jwkwon0817.tetrio.tetriobot.global.api

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.httpGet

object TetrioApi {
	const val baseUrl = "https://ch.tetr.io/api"

	const val backgroundUrl = "https://tetr.io/res/tetrio_social.png"

	const val imageUrl = "https://static-00.iconduck.com/assets.00/tetrio-desktop-icon-1024x1024-a14596tm.png"

	private val objectMapper: ObjectMapper = ObjectMapper()

	fun get(url: String): String {
		val (request, response, result) = url
			.httpGet()
			.responseString()

		result.fold(
			success = { return it },
			failure = { throw Exception(it.toString()) }
		)
	}

	fun toObject(json: String): Map<String, Any> {
		return objectMapper.readValue(json, object : TypeReference<Map<String, Any>>() {})
	}
}