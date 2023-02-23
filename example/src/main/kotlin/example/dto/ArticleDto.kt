package example.dto

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ArticleTitleDto(
    @Contextual
    val id: UUID,
    val title: String,
)

@Serializable
data class ArticleTitleWithAuthorDto(
    @Contextual
    val id: UUID,
    val title: String,
    val authorId: Long?,
)
