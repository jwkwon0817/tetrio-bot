package me.jwkwon0817.tetrio.tetriobot.database

import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
class UserEntity(
	@Id
	var discordId: String,

	var tetrioId: String,
)