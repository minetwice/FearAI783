package com.agent.app.huggingface

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface HuggingFaceApi {

    @GET("api/models")
    suspend fun searchModels(
        @Query("search") search: String = "gguf",
        @Query("filter") filter: String = "gguf",
        @Query("sort") sort: String = "downloads",
        @Query("direction") direction: Int = -1,
        @Query("limit") limit: Int = 20
    ): Response<List<HuggingFaceModel>>

    @GET("api/models/{model_id}")
    suspend fun getModelDetail(
        @Path("model_id", encoded = true) modelId: String
    ): Response<HuggingFaceModelDetail>
}
