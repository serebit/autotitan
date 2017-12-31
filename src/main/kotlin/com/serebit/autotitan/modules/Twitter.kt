package com.serebit.autotitan.modules

import com.serebit.autotitan.api.Module
import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.meta.Locale
import com.serebit.autotitan.api.meta.annotations.Command
import com.serebit.autotitan.data.DataManager
import com.serebit.extensions.jda.sendEmbed
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder
import java.util.*
import kotlin.concurrent.timer
import twitter4j.Twitter as TwitterApi

class Twitter : Module(isOptional = true) {
    private val twitterManagers = mutableMapOf<Long, TwitterManager>()
    private val dataManager = DataManager(this::class.java)


    init {
        val storedConfig = dataManager.read("config.json", ModuleConfiguration::class.java)
        if (storedConfig != null) restore(storedConfig)
    }

    private fun restore(config: ModuleConfiguration) {
        config.map.forEach {
            twitterManagers.put(it.key, TwitterManager(it.value))
        }
    }

    @Command(locale = Locale.GUILD, access = Access.BOT_OWNER)
    fun initTwitter(
            evt: MessageReceivedEvent,
            consumerKey: String,
            consumerSecret: String,
            accessToken: String,
            accessTokenSecret: String
    ) {
        twitterManagers.put(
                evt.guild.idLong,
                TwitterManager(ApiConfiguration(consumerKey, consumerSecret, accessToken, accessTokenSecret))
        )

        dataManager.write("config.json", twitterManagers.configuration)

        evt.message.delete().complete()
        evt.channel.sendMessage("Twitter has been initialized.").complete()
    }

    @Command(locale = Locale.GUILD, splitLastParameter = false)
    fun tweet(evt: MessageReceivedEvent, message: String) {
        twitterManagers[evt.guild.idLong]?.tweet(evt, message)
    }

    @Command(locale = Locale.GUILD)
    fun tweetQueue(evt: MessageReceivedEvent): Unit = evt.run {
        val manager = twitterManagers[evt.guild.idLong]
        if (manager == null) {
            channel.sendMessage("Twitter is not initialized for this guild.").complete()
            return
        }
        channel.sendMessage(manager.queue).complete()
    }

    private val MutableMap<Long, TwitterManager>.configuration
        get() = ModuleConfiguration(
                map { it.key to it.value.config }.toMap()
        )

    private data class Tweet(
            val channel: TextChannel,
            val user: User,
            val message: String
    )

    private data class ApiConfiguration(
            val oAuthConsumerKey: String,
            val oAuthConsumerSecret: String,
            val oAuthAccessToken: String,
            val oAuthAccessTokenSecret: String
    )

    private data class ModuleConfiguration(
            val map: Map<Long, ApiConfiguration>
    )

    private class TwitterManager(val config: ApiConfiguration) {
        private val charLimit = 280
        private val twitter: TwitterApi
        private var scheduler: Timer? = null
        private val tweetQueue = mutableListOf<Tweet>()
        val queue
            get() = tweetQueue.joinToString("\n") {
                "${it.user.asMention}: `${it.message}`"
            }

        init {
            val cb = ConfigurationBuilder().apply {
                setDebugEnabled(true)
                setOAuthConsumerKey(config.oAuthConsumerKey)
                setOAuthConsumerSecret(config.oAuthConsumerSecret)
                setOAuthAccessToken(config.oAuthAccessToken)
                setOAuthAccessTokenSecret(config.oAuthAccessTokenSecret)
            }

            twitter = TwitterFactory(cb.build()).instance
            scheduler = timer(daemon = true, period = 60000, initialDelay = 60000) {
                if (tweetQueue.isNotEmpty()) {
                    val tweetEvent = tweetQueue.removeAt(0)
                    val status = twitter.updateStatus(tweetEvent.message)

                    tweetEvent.channel.sendEmbed {
                        setAuthor(
                                "Tweet by ${tweetEvent.user.name}#${tweetEvent.user.discriminator}",
                                "https://twitter.com/${status.user.screenName}/status/${status.id}",
                                tweetEvent.user.effectiveAvatarUrl
                        )
                        setDescription(tweetEvent.message)
                    }.queue()
                }
            }
        }

        fun tweet(evt: MessageReceivedEvent, status: String) {
            evt.run {
                if (status.length > charLimit) {
                    channel.sendMessage(
                            "${author.asMention}, your tweet is ${status.length - charLimit} characters too long!"
                    ).complete()
                    return
                }
                tweetQueue.add(Tweet(textChannel, author, status))
                channel.sendMessage("${author.asMention}, your tweet has been queued.").complete()
            }
        }
    }
}