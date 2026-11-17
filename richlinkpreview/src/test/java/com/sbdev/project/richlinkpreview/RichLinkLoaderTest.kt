package com.sbdev.project.richlinkpreview

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class RichLinkLoaderTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val fetchCount = AtomicInteger()
    private var gate = CompletableDeferred<Unit>()
    private var nextResult: Result<MetaData> = Result.success(MetaData(title = "Fetched"))

    /** A loader whose "network" counts calls and waits until the test opens [gate]. */
    private val loader = RichLinkLoader(scope = scope) { _, _ ->
        fetchCount.incrementAndGet()
        gate.await()
        nextResult
    }

    @Before
    fun setUp() {
        RichLinkCache.enabled = false
        RichLinkCache.clear()
    }

    @After
    fun tearDown() {
        scope.cancel()
        RichLinkCache.enabled = false
    }

    /** Waits until [condition] holds, so tests do not depend on thread timing. */
    private suspend fun awaitCondition(condition: () -> Boolean) = withTimeout(5_000) {
        while (!condition()) yield()
    }

    @Test
    fun `concurrent loads of the same url share one fetch`() = runBlocking {
        val loads = List(5) { async(Dispatchers.Default) { loader.load("https://a.com") } }
        awaitCondition { fetchCount.get() == 1 }

        gate.complete(Unit)
        val results = loads.awaitAll()

        assertEquals(1, fetchCount.get())
        assertTrue(results.all { it.getOrNull()?.title == "Fetched" })
        awaitCondition { loader.inFlightCount == 0 }
    }

    @Test
    fun `different urls are fetched separately`() = runBlocking {
        gate.complete(Unit)
        listOf("https://a.com", "https://b.com").map { async { loader.load(it) } }.awaitAll()
        assertEquals(2, fetchCount.get())
    }

    @Test
    fun `a caller leaving does not cancel the fetch for the others`() = runBlocking {
        val leaving = launch(Dispatchers.Default) { loader.load("https://a.com") }
        val staying = async(Dispatchers.Default) { loader.load("https://a.com") }
        awaitCondition { fetchCount.get() == 1 }

        leaving.cancelAndJoin()
        gate.complete(Unit)

        assertEquals("Fetched", staying.await().getOrNull()?.title)
        assertEquals(1, fetchCount.get())
    }

    @Test
    fun `cached url is not fetched`() = runBlocking {
        RichLinkCache.enabled = true
        RichLinkCache.put("https://a.com", MetaData(title = "Cached"))

        assertEquals("Cached", loader.load("https://a.com").getOrNull()?.title)
        assertEquals(0, fetchCount.get())
    }

    @Test
    fun `successful fetch is cached so the next load skips the network`() = runBlocking {
        RichLinkCache.enabled = true
        gate.complete(Unit)

        loader.load("https://a.com")
        loader.load("https://a.com")

        assertEquals(1, fetchCount.get())
    }

    @Test
    fun `without the cache, sequential loads fetch again`() = runBlocking {
        gate.complete(Unit)
        loader.load("https://a.com")
        loader.load("https://a.com")
        assertEquals(2, fetchCount.get())
    }

    @Test
    fun `refresh ignores the cache and replaces the entry`() = runBlocking {
        RichLinkCache.enabled = true
        RichLinkCache.put("https://a.com", MetaData(title = "Old"))
        gate.complete(Unit)

        assertEquals("Fetched", loader.refresh("https://a.com").getOrNull()?.title)
        assertEquals(1, fetchCount.get())
        assertEquals("Fetched", RichLinkCache.get("https://a.com")?.title)
    }

    @Test
    fun `failed refresh keeps the cached entry`() = runBlocking {
        RichLinkCache.enabled = true
        RichLinkCache.put("https://a.com", MetaData(title = "Old"))
        nextResult = Result.failure(IOException("offline"))
        gate.complete(Unit)

        assertTrue(loader.refresh("https://a.com").isFailure)
        assertEquals("Old", RichLinkCache.get("https://a.com")?.title)
    }

    @Test
    fun `refresh joins a fetch that is already running`() = runBlocking {
        val load = async(Dispatchers.Default) { loader.load("https://a.com") }
        awaitCondition { fetchCount.get() == 1 }
        // UNDISPATCHED: runs until it suspends on the shared fetch, so it has joined
        // the running fetch before the gate opens.
        val refresh = async(start = CoroutineStart.UNDISPATCHED) { loader.refresh("https://a.com") }

        gate.complete(Unit)
        load.await()
        refresh.await()
        assertEquals(1, fetchCount.get())
    }

    @Test
    fun `previews showing the url receive refreshed data`() = runBlocking {
        val received = CompletableDeferred<MetaData>()
        val listener = launch(Dispatchers.Default) {
            loader.updates("https://a.com").collect { received.complete(it) }
        }
        val other = launch(Dispatchers.Default) {
            loader.updates("https://b.com").collect { error("wrong url notified") }
        }
        delay(200) // let both collectors subscribe; updates are not replayed

        gate.complete(Unit)
        loader.refresh("https://a.com")

        assertEquals("Fetched", withTimeout(5_000) { received.await() }.title)
        listener.cancel()
        other.cancel()
    }

    @Test
    fun `failures are shared but not cached`() = runBlocking {
        RichLinkCache.enabled = true
        nextResult = Result.failure(IOException("offline"))

        val loads = List(3) { async(Dispatchers.Default) { loader.load("https://a.com") } }
        awaitCondition { fetchCount.get() == 1 }
        gate.complete(Unit)

        assertTrue(loads.awaitAll().all { it.isFailure })
        assertEquals(1, fetchCount.get())

        loader.load("https://a.com") // retried, because the failure was not cached
        assertEquals(2, fetchCount.get())
    }
}
