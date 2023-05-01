package example.basic.dto

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDate

interface IUser {
    val username: String
    val birthDate: LocalDate?
}

@Serializable
data class UserDto(
    val id: Long,
    override val username: String,
    @Contextual
    override val birthDate: LocalDate? = null,
) : IUser
