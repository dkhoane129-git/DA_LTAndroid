package com.spendwise.app.network

import com.spendwise.app.network.dto.PushResponse
import com.spendwise.app.network.dto.RemoteTransaction
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * REST API của Firebase Realtime Database.
 * Rule trên server chỉ cho user đọc/ghi /users/{uid của chính mình}, cần ID token qua ?auth=
 */
interface CloudSyncApi {

    @GET("users/{uid}/transactions.json")
    suspend fun getAll(
        @Path("uid") uid: String,
        @Query("auth") token: String?
    ): Response<Map<String, RemoteTransaction>?>

    @POST("users/{uid}/transactions.json")
    suspend fun create(
        @Path("uid") uid: String,
        @Query("auth") token: String?,
        @Body body: RemoteTransaction
    ): Response<PushResponse>

    @PUT("users/{uid}/transactions/{key}.json")
    suspend fun update(
        @Path("uid") uid: String,
        @Path("key") key: String,
        @Query("auth") token: String?,
        @Body body: RemoteTransaction
    ): Response<RemoteTransaction>

    @DELETE("users/{uid}/transactions/{key}.json")
    suspend fun delete(
        @Path("uid") uid: String,
        @Path("key") key: String,
        @Query("auth") token: String?
    ): Response<Unit>
}
