package me.jwkwon0817.tetrio.tetriobot.database

import me.jwkwon0817.tetrio.tetriobot.global.exceptions.UserNotFoundException
import org.springframework.stereotype.Service

@Service
class UserService(
	private val userRepository: UserRepository
) {
	fun getUser(discordId: String): UserEntity {
		return userRepository.findById(discordId).orElseThrow(::UserNotFoundException)
	}

	fun setUser(discordId: String, tetrioId: String) {
		userRepository.save(UserEntity(discordId, tetrioId))
	}

	fun deleteUser(discordId: String) {
		userRepository.deleteById(discordId)
	}

	fun getUsers(): MutableList<UserEntity> {
		return userRepository.findAll().toMutableList()
	}
}