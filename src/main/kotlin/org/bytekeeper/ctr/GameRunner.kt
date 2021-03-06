package org.bytekeeper.ctr

import org.apache.logging.log4j.LogManager
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import kotlin.concurrent.thread

@Component
class GameRunner(private val gameService: GameService,
                 private val config: Config) : CommandLineRunner {
    private val log = LogManager.getLogger()

    override fun run(vararg args: String?) {
        log.info("Let's play! Starting ${config.parallelGamesCount} concurrent games")

        repeat(config.parallelGamesCount) {
            log.info("Starting worker thread")
            thread {
                try {
                    while (true) {
                        gameService.schedule1on1()
                    }
                } catch (e: NotEnoughBotsException) {
                    log.debug("No bots to schedule, pausing...")
                    Thread.sleep(1000)
                } catch (e: Exception) {
                    log.error("Worker thread DIED!", e)
                    throw e
                }
            }
        }
    }


}

class BotDisabledException(message: String) : RuntimeException(message)
