package me.jwkwon0817.tetrio.tetriobot.global.api

object UserApi {
	fun getUserInfo(username: String) =
		TetrioApi.toObject(TetrioApi.get("${TetrioApi.baseUrl}/users/${username}"))

	fun getUserRecords(username: String) =
		TetrioApi.toObject(TetrioApi.get("${TetrioApi.baseUrl}/users/${username}/records"))

	fun getUserSearch(query: String) =
		TetrioApi.toObject(TetrioApi.get("${TetrioApi.baseUrl}/users/search/${query}"))
}