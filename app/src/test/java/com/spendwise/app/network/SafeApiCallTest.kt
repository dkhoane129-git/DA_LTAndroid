package com.spendwise.app.network

import com.spendwise.app.network.dto.RemoteTransaction
import com.spendwise.app.util.Resource
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Kiểm tra Retrofit + safeApiCall trả đúng 3 trạng thái với các tình huống mạng. */
class SafeApiCallTest {

    private lateinit var server: MockWebServer
    private lateinit var rateApi: ExchangeRateApi
    private lateinit var syncApi: CloudSyncApi

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        rateApi = retrofit.create(ExchangeRateApi::class.java)
        syncApi = retrofit.create(CloudSyncApi::class.java)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun success_parsesJson() = runBlocking {
        server.enqueue(MockResponse().setBody(
            """{"result":"success","base_code":"VND","time_last_update_unix":1,"rates":{"USD":0.000038}}"""
        ))
        val r = safeApiCall { rateApi.latest("VND") }
        assertTrue(r is Resource.Success)
        assertEquals(0.000038, (r as Resource.Success).data!!.rates!!["USD"]!!, 1e-9)
        assertEquals("/v6/latest/VND", server.takeRequest().path)
    }

    @Test
    fun http400_401_500_mapToErrorWithCode() = runBlocking {
        for (code in listOf(400, 401, 500)) {
            server.enqueue(MockResponse().setResponseCode(code).setBody("""{"error":"x"}"""))
            val r = safeApiCall { syncApi.getAll("u1", null) }
            assertTrue(r is Resource.Error)
            assertEquals(code, (r as Resource.Error).code)
            assertTrue(r.message.contains("$code"))
        }
    }

    @Test
    fun noNetwork_mapsToErrorWithoutCode() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        val r = safeApiCall { rateApi.latest("VND") }
        assertTrue(r is Resource.Error)
        assertEquals(null, (r as Resource.Error).code)
    }

    @Test
    fun firebaseRest_requestShape() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"name":"-Nabc"}"""))
        val r = safeApiCall { syncApi.create("u1", "TOKEN", RemoteTransaction(title = "Cơm", amount = 30000)) }
        assertEquals("-Nabc", (r as Resource.Success).data!!.name)
        val req = server.takeRequest()
        assertEquals("POST", req.method)
        assertEquals("/users/u1/transactions.json?auth=TOKEN", req.path)
        assertTrue(req.body.readUtf8().contains("\"title\":\"Cơm\""))

        server.enqueue(MockResponse().setBody("null"))
        val empty = safeApiCall { syncApi.getAll("u1", "TOKEN") }
        assertTrue(empty is Resource.Success && empty.data == null)
    }
}
