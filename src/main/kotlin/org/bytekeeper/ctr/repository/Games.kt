package org.bytekeeper.ctr.repository

import io.micrometer.core.annotation.Timed
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.time.Instant
import java.util.*
import javax.persistence.*

@Entity
class GameResult(@Id val id: UUID,
                 val time: Instant,
                 val gameRealtime: Double,
                 val realtimeTimeout: Boolean = false,
                 val frameTimeout: Boolean? = false,
                 val map: String,
                 @ManyToOne val botA: Bot,
                 @Enumerated(EnumType.STRING) val raceA: Race,
                 @ManyToOne val botB: Bot,
                 @Enumerated(EnumType.STRING) val raceB: Race,
                 @ManyToOne val winner: Bot? = null,
                 @ManyToOne val loser: Bot? = null,
                 val botACrashed: Boolean = false,
                 val botBCrashed: Boolean = false,
                 val gameHash: String,
                 val frameCount: Int? = null)

class BotVsBotWonGames(val botA: Bot,
                       val botB: Bot,
                       val won: Long)

class BotStat(val bot: Bot, val won: Long, val lost: Long)
class MapStat(val map: String, val won: Long, val lost: Long)
class BotRaceVsRace(val bot: Bot, val race: Race, val enemyRace: Race, val won: Long, val lost: Long)

interface GameResultRepository : CrudRepository<GameResult, Long> {
    @EntityGraph(attributePaths = ["botA", "botB", "winner", "loser"])
    @Timed
    fun findByTimeGreaterThan(time: Instant): MutableIterable<GameResult>

    @Timed
    fun countByWinnerRaceAndLoserRace(winner: Race, loser: Race): Int

    @Timed
    fun countByBotACrashedIsTrueOrBotBCrashedIsTrue(): Int

    @Query("SELECT AVG(g.gameRealtime) FROM GameResult g WHERE g.winner <> NULL")
    @Timed
    fun averageGameRealtime(): Double

    @Query("""SELECT new org.bytekeeper.ctr.repository.BotVsBotWonGames(r.winner, r.loser, COUNT(r))
        FROM GameResult r WHERE r.time >= ?1
        GROUP BY r.winner, r.loser""")
    @Timed
    fun listBotVsBotWonGames(after: Instant): List<BotVsBotWonGames>

    @Query("SELECT new org.bytekeeper.ctr.repository.BotStat(bot, SUM(CASE WHEN (r.winner = bot) THEN 1 ELSE 0 END)," +
            " SUM(CASE WHEN (r.loser = bot) THEN 1 else 0 END)) FROM GameResult r, Bot bot WHERE r.time > bot.lastUpdated AND (r.winner = bot OR r.loser = bot) AND bot.enabled = TRUE" +
            " GROUP BY bot")
    @Timed
    fun gamesSinceLastUpdate(): List<BotStat>

    @Query("SELECT new org.bytekeeper.ctr.repository.MapStat(r.map, SUM(CASE WHEN (r.winner = ?1) THEN 1 ELSE 0 END)," +
            " SUM(CASE WHEN (r.loser = ?1) THEN 1 else 0 END)) FROM GameResult r WHERE r.winner = ?1 OR r.loser = ?1" +
            " GROUP BY map")
    @Timed
    fun botStatsPerMap(bot: Bot): List<MapStat>

    @Query("SELECT new org.bytekeeper.ctr.repository.BotRaceVsRace(bot, " +
            "CASE WHEN (r.botA = bot) THEN r.raceA else r.raceB END," +
            "CASE WHEN (r.botA = bot) THEN r.raceB else r.raceA END," +
            " SUM(CASE WHEN (r.winner = bot) THEN 1 ELSE 0 END)," +
            " SUM(CASE WHEN (r.loser = bot) THEN 1 else 0 END))" +
            " FROM GameResult r, Bot bot where (r.botA = bot OR r.botB = bot) AND bot.enabled = TRUE" +
            " GROUP by bot, CASE WHEN (r.botA = bot) THEN r.raceA else r.raceB END, CASE WHEN (r.botA = bot) THEN r.raceB else r.raceA END")
    @Timed
    fun listBotRaceVsRace(): List<BotRaceVsRace>
}