package com.serebit.autotitan.extensions.extra

import com.serebit.autotitan.api.Access
import com.serebit.autotitan.api.Locale
import com.serebit.autotitan.api.annotations.CommandFunction
import com.serebit.autotitan.api.annotations.ExtensionClass
import com.serebit.autotitan.data.DataManager
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder
import java.util.*
import kotlin.concurrent.timer


@ExtensionClass
class TwitterExtension {
    private var twitter: Twitter? = null
    private var scheduler: Timer? = null
    private val tweetQueue = mutableListOf<Tweet>()
    private val dataManager = DataManager(TwitterExtension::class.java)

    init {
        val storedConfig = dataManager.read("config.json", TwitterConfiguration::class.java)
        if (storedConfig != null) init(storedConfig)
    }

    private fun init(config: TwitterConfiguration) {
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
                val tweet = tweetQueue.first()
                tweetQueue.removeAt(0)
                val status = immutableTwitter.updateStatus(tweet.message)
                val url = "https://twitter.com/${status.user.screenName}/status/${status.id}"
                val builder = EmbedBuilder().apply {
                    setAuthor(
                            "Tweet by ${tweet.user.name}#${tweet.user.discriminator}",
                            url,
                            tweet.user.effectiveAvatarUrl
                    )
                    setDescription(tweet.message)
                }
                tweet.channel.sendMessage(builder.build()).queue()
            }
        }
        dataManager.write("config.json", config)
    }

    @CommandFunction(
            access = Access.BOT_OWNER
    )
    fun initTwitter(
            evt: MessageReceivedEvent,
            oAuthConsumerKey: String,
            oAuthConsumerSecret: String,
            oAuthAccessToken: String,
            oAuthAccessTokenSecret: String
    ) {
        evt.message.delete().complete()
        init(TwitterConfiguration(
                oAuthConsumerKey,
                oAuthConsumerSecret,
                oAuthAccessToken,
                oAuthAccessTokenSecret
        ))
        evt.channel.sendMessage("Twitter has been initialized.").complete()
    }

    @CommandFunction(locale = Locale.GUILD, delimitFinalParameter = false)
    fun tweet(evt: MessageReceivedEvent, message: String) {
        if (message.length <= 140) {
            val tweet = Tweet(evt.textChannel, evt.author, message)
            tweetQueue.add(tweet)
            evt.channel.sendMessage(
                    """${evt.author.asMention}, your tweet has been queued."""
            ).queue()
        } else {
            evt.channel.sendMessage(
                    """${evt.author.asMention}, your tweet is ${message.length - 140} characters
                    too long!"""
            ).queue()
        }
    }

    @CommandFunction(locale = Locale.GUILD)
    fun tweetQueue(evt: MessageReceivedEvent) {
        val builder = EmbedBuilder().apply {
            setTitle("Tweet Queue")
            var description = ""
            tweetQueue.forEach {
                description += "${it.user.asMention}: \"${it.message}\"\n"
            }
            setDescription(description)
        }
        evt.channel.sendMessage(builder.build()).queue()
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