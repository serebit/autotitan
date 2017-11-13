package com.serebit.autotitan.modules.extra

import com.serebit.autotitan.api.meta.Access
import com.serebit.autotitan.api.meta.Locale
import com.serebit.autotitan.api.meta.annotations.CommandFunction
import com.serebit.autotitan.api.meta.annotations.ExtensionClass
import com.serebit.autotitan.data.DataManager
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder
import java.util.*
import kotlin.concurrent.timer

@ExtensionClass(name = "Twitter")
class TwitterExtension {
    private val twitterManagers = mutableMapOf<Long, TwitterManager>()
    private val dataManager = DataManager(this::class.java)

    init {
        val storedConfig = dataManager.read("config.json", TwitterExtensionConfiguration::class.java)
        if (storedConfig != null) restore(storedConfig)
    }

    private fun restore(config: TwitterExtensionConfiguration) {
        config.map.forEach {
            twitterManagers.put(it.key, TwitterManager(it.value))
        }
    }

    @CommandFunction(
            locale = Locale.GUILD,
            access = Access.BOT_OWNER
    )
    fun initTwitter(
            evt: MessageReceivedEvent,
            oAuthConsumerKey: String,
            oAuthConsumerSecret: String,
            oAuthAccessToken: String,
            oAuthAccessTokenSecret: String
    ) {
        twitterManagers.put(evt.guild.idLong, TwitterManager(TwitterConfiguration(
                oAuthConsumerKey,
                oAuthConsumerSecret,
                oAuthAccessToken,
                oAuthAccessTokenSecret
        )))

        dataManager.write("config.json", TwitterExtensionConfiguration(
                twitterManagers.map { it.key to it.value.config }.toMap()
        ))

        evt.message.delete().complete()
        evt.channel.sendMessage("Twitter has been initialized.").complete()
    }

    @CommandFunction(locale = Locale.GUILD, delimitFinalParameter = false)
    fun tweet(evt: MessageReceivedEvent, message: String) {
        twitterManagers[evt.guild.idLong]?.tweet(evt, message)
    }

    @CommandFunction(locale = Locale.GUILD)
    fun tweetQueue(evt: MessageReceivedEvent): Unit = evt.run {
        val manager = twitterManagers[evt.guild.idLong]
        if (manager == null) {
            channel.sendMessage("Twitter is not initialized for this guild.").complete()
            return
        }
        channel.sendMessage(manager.queue).complete()
    }
}

private class TwitterManager(val config: TwitterConfiguration) {
    private var twitter: Twitter? = null
    private var scheduler: Timer? = null
    private val tweetQueue = mutableListOf<Tweet>()
    val queue: MessageEmbed
        get() = EmbedBuilder().apply {
            setTitle("Tweet Queue")
            var description = ""
            tweetQueue.forEach {
                description += "${it.user.asMention}: \"${it.message}\"\n"
            }
            setDescription(description)
        }.build()

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
            val immutableTwitter = twitter
            if (immutableTwitter != null && tweetQueue.isNotEmpty()) {
                val tweetEvent = tweetQueue.removeAt(0)
                val status = immutableTwitter.updateStatus(tweetEvent.message)
                val url = "https://twitter.com/${status.user.screenName}/status/${status.id}"

                val embed = EmbedBuilder().apply {
                    setAuthor(
                            "Tweet by ${tweetEvent.user.name}#${tweetEvent.user.discriminator}",
                            url,
                            tweetEvent.user.effectiveAvatarUrl
                    )
                    setDescription(tweetEvent.message)
                }.build()

                tweetEvent.channel.sendMessage(embed).queue()
            }
        }
    }

    fun tweet(evt: MessageReceivedEvent, message: String) {
        if (message.length <= 140) {
            val tweet = Tweet(evt.textChannel, evt.author, message)
            tweetQueue.add(tweet)
            evt.channel.sendMessage(
                    """${evt.author.asMention}, your tweet has been queued."""
            ).complete()
        } else {
            evt.channel.sendMessage(
                    """${evt.author.asMention}, your tweet is ${message.length - 140} characters
                    too long!"""
            ).complete()
        }
    }
}

private data class Tweet(
        val channel: TextChannel,
        val user: User,
        val message: String
)

private data class TwitterConfiguration(
        val oAuthConsumerKey: String,
        val oAuthConsumerSecret: String,
        val oAuthAccessToken: String,
        val oAuthAccessTokenSecret: String
)

private data class TwitterExtensionConfiguration(
        val map: Map<Long, TwitterConfiguration>
)