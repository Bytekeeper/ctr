package org.bytekeeper.ctr.publish

import org.assertj.core.api.Assertions
import org.bytekeeper.ctr.*
import org.bytekeeper.ctr.repository.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.io.BufferedWriter
import java.io.StringWriter
import java.time.Instant
import java.util.*

@ExtendWith(MockitoExtension::class)
class GameResultsPublisherTest {
    private lateinit var sut: GameResultsPublisher

    @Mock
    private lateinit var gameResultRepository: GameResultRepository

    @Mock
    private lateinit var unitEventsRepository: UnitEventsRepository

    @Mock
    private lateinit var publisher: Publisher

    private val writer: StringWriter = StringWriter()

    @BeforeEach
    fun setup() {
        sut = GameResultsPublisher(gameResultRepository, unitEventsRepository, publisher, Maps(), Config())

        given(publisher.globalStatsWriter(ArgumentMatchers.anyString()))
                .willReturn(BufferedWriter(writer))
    }

    @Test
    fun shouldRenderGamesWithWinner() {
        // GIVEN
        val botA = Bot(-1, true, null, "botA", Race.PROTOSS, "", rank = Ranking.Rank.B)
        val botB = Bot(-1, true, null, "botB", Race.TERRAN, "", rank = Ranking.Rank.S)

        given(gameResultRepository.findByTimeGreaterThan(any())).willReturn(mutableListOf(
                GameResult(UUID.randomUUID(), Instant.MIN, 1.0, false, false, "", "map", botA, Race.ZERG, botB, Race.TERRAN, botA, botB, false, false, "", 0)))

        // WHEN
        sut.handle(PreparePublish())

        // THEN
        Assertions.assertThat(writer.toString())
                .isEqualTo("""{"bots":[{"name":"botA","race":"P","rank":"B","rating":2000},{"name":"botB","race":"T","rank":"S","rating":2000}],"maps":["map"],"results":[{"botA":{"botIndex":0,"race":"Z","winner":true,"loser":false,"crashed":false},"botB":{"botIndex":1,"race":"T","winner":false,"loser":true,"crashed":false},"invalidGame":false,"realTimeout":false,"frameTimeout":false,"endedAt":-31557014167219200,"mapIndex":0,"gameHash":"","frameCount":0,"gameEvents":null}]}""")
    }

    @Test
    fun shouldRenderGamesWithCrash() {
        // GIVEN
        val botA = Bot(-1, true, null, "botA", Race.RANDOM, "")
        val botB = Bot(-1, true, null, "botB", Race.ZERG, "")

        given(gameResultRepository.findByTimeGreaterThan(any())).willReturn(mutableListOf(
                GameResult(UUID.randomUUID(), Instant.MIN, 1.0, false, false, "", "map", botA, Race.TERRAN, botB, Race.PROTOSS, botA, botB, true, false, "", 0)))

        // WHEN
        sut.handle(PreparePublish())

        // THEN
        Assertions.assertThat(writer.toString())
                .isEqualTo("""{"bots":[{"name":"botA","race":"R","rank":"U","rating":2000},{"name":"botB","race":"Z","rank":"U","rating":2000}],"maps":["map"],"results":[{"botA":{"botIndex":0,"race":"T","winner":true,"loser":false,"crashed":true},"botB":{"botIndex":1,"race":"P","winner":false,"loser":true,"crashed":false},"invalidGame":false,"realTimeout":false,"frameTimeout":false,"endedAt":-31557014167219200,"mapIndex":0,"gameHash":"","frameCount":0,"gameEvents":null}]}""")
    }

    @Test
    fun shouldRenderGamesWithRealtimeout() {
        // GIVEN
        val botA = Bot(-1, true, null, "botA", Race.RANDOM, "")
        val botB = Bot(-1, true, null, "botB", Race.ZERG, "")

        given(gameResultRepository.findByTimeGreaterThan(any())).willReturn(mutableListOf(
                GameResult(UUID.randomUUID(), Instant.MIN, 1.0, true, false, "", "map", botA, Race.PROTOSS, botB, Race.ZERG, null, null, false, false, "", 0)))

        // WHEN
        sut.handle(PreparePublish())

        // THEN
        Assertions.assertThat(writer.toString())
                .isEqualTo("""{"bots":[{"name":"botA","race":"R","rank":"U","rating":2000},{"name":"botB","race":"Z","rank":"U","rating":2000}],"maps":["map"],"results":[{"botA":{"botIndex":0,"race":"P","winner":false,"loser":false,"crashed":false},"botB":{"botIndex":1,"race":"Z","winner":false,"loser":false,"crashed":false},"invalidGame":true,"realTimeout":true,"frameTimeout":false,"endedAt":-31557014167219200,"mapIndex":0,"gameHash":"","frameCount":0,"gameEvents":null}]}""")
    }

    @Test
    fun shouldRenderGamesWithFrametimeout() {
        // GIVEN
        val botA = Bot(-1, true, null, "botA", Race.RANDOM, "")
        val botB = Bot(-1, true, null, "botB", Race.ZERG, "")

        given(gameResultRepository.findByTimeGreaterThan(any())).willReturn(mutableListOf(
                GameResult(UUID.randomUUID(), Instant.MIN, 1.0, false, true, "", "map", botA, Race.TERRAN, botB, Race.PROTOSS, botA, botB, false, false, "", 0)))

        // WHEN
        sut.handle(PreparePublish())

        // THEN
        Assertions.assertThat(writer.toString())
                .isEqualTo("""{"bots":[{"name":"botA","race":"R","rank":"U","rating":2000},{"name":"botB","race":"Z","rank":"U","rating":2000}],"maps":["map"],"results":[{"botA":{"botIndex":0,"race":"T","winner":true,"loser":false,"crashed":false},"botB":{"botIndex":1,"race":"P","winner":false,"loser":true,"crashed":false},"invalidGame":false,"realTimeout":false,"frameTimeout":true,"endedAt":-31557014167219200,"mapIndex":0,"gameHash":"","frameCount":0,"gameEvents":null}]}""")
    }

    @Test
    fun `should render game events`() {
        // GIVEN
        val botA = Bot(-1, true, null, "botA", Race.RANDOM, "")
        val botB = Bot(-1, true, null, "botB", Race.ZERG, "")

        val gameId = UUID.randomUUID()
        given(gameResultRepository.findByTimeGreaterThan(any())).willReturn(mutableListOf(
                GameResult(gameId, Instant.MIN, 1.0, false, true, "", "map", botA, Race.TERRAN, botB, Race.PROTOSS, botA, botB, false, false, "", 0)))
        given(unitEventsRepository.aggregateGameEventsWith8OrMoreEvents(any())).willReturn(
                listOf(GameEvent(gameId, UnitType.PROTOSS_CARRIER, UnitEventType.UNIT_CREATE, 10L))
        )

        // WHEN
        sut.handle(PreparePublish())

        // THEN
        Assertions.assertThat(writer.toString())
                .isEqualTo("""{"bots":[{"name":"botA","race":"R","rank":"U","rating":2000},{"name":"botB","race":"Z","rank":"U","rating":2000}],"maps":["map"],"results":[{"botA":{"botIndex":0,"race":"T","winner":true,"loser":false,"crashed":false},"botB":{"botIndex":1,"race":"P","winner":false,"loser":true,"crashed":false},"invalidGame":false,"realTimeout":false,"frameTimeout":true,"endedAt":-31557014167219200,"mapIndex":0,"gameHash":"","frameCount":0,"gameEvents":[{"unit":72,"event":1,"amount":10}]}]}""")
    }
}