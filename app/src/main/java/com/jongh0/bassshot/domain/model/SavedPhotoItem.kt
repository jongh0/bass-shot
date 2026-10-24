package com.jongh0.bassshot.domain.model

import android.net.Uri

data class SavedPhotoItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateAdded: Long,
    val isAlreadyMeasured: Boolean = false
)
