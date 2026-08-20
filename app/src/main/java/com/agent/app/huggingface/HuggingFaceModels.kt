package com.agent.app.huggingface

import com.google.gson.annotations.SerializedName

data class HuggingFaceModel(
    val id: String,
    val downloads: Long,
    val likes: Long,
    val tags: List<String>?,
    val siblings: List<ModelSibling>?
)

data class ModelSibling(
    @SerializedName("rfilename") val rfilename: String
)

data class HuggingFaceModelDetail(
    val id: String,
    val downloads: Long,
    val likes: Long,
    val tags: List<String>?,
    val siblings: List<ModelSibling>?,
    val pipeline_tag: String?
)
