import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.extensions.filters.Filter
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton

private const val API_TOKEN = "5635376755:AAGnKRpLl_h3Xqr1EeEhy1th4dzsrxmfLjI"

private const val WEEK_NEWS = "Новости недели"
private const val TRENDS = "Тренды"
private const val INSIGHTS = "Инсайты"

private const val POSTS_ON_PAGE = 2

private val usersStateMap = mutableMapOf<Long, UserState>()

fun main() {
    val bot = bot {
        token = API_TOKEN
        dispatch {

            message(Filter.Sticker) {
                bot.sendMessage(ChatId.fromId(message.chat.id), text = "Nice sticker bro \\o/")
            }

            text {
                println("${ChatId.fromId(message.chat.id)} -> $text")

                when (text) {
                    WEEK_NEWS -> getWeekNews(bot, message.chat.id)
                    TRENDS -> getTrends(bot, message.chat.id)
                    INSIGHTS -> getInsights(bot, message.chat.id)
                    else -> {
                        val keyboardMarkup =
                            KeyboardReplyMarkup(keyboard = generateUsersButton(), resizeKeyboard = true)

                        if (usersStateMap.keys.contains(message.chat.id).not()) {
                            bot.sendMessage(
                                chatId = ChatId.fromId(message.chat.id),
                                text = "Авторизация прошла успешно.",
                                replyMarkup = keyboardMarkup
                            )

                            usersStateMap[message.chat.id] = UserState(
                                weekPostsList = arrayListOf(), weekPostsPage = 0, weekPostsMessageId = 0,
                                trendPostsList = arrayListOf(), trendPostsPage = 0, trendPostsMessageId = 0,
                                insightPostsList = arrayListOf(), insightPostsPage = 0, insightPostsMessageId = 0
                            )
                        }
                    }
                }
            }

            audio {
                bot.sendMessage(ChatId.fromId(message.chat.id), text = "Я глухой, але...")
            }

            callbackQuery("nextWeekNew") {
                showWeekPosts(bot, callbackQuery.message?.chat?.id ?: return@callbackQuery)
            }

            callbackQuery("prevWeekNew") {
                showWeekPosts(bot, callbackQuery.message?.chat?.id ?: return@callbackQuery, next = false)
            }

            callbackQuery("prevTrend") {
                showTrendPosts(bot, callbackQuery.message?.chat?.id ?: return@callbackQuery, next = false)
            }

            callbackQuery("nextTrend") {
                showTrendPosts(bot, callbackQuery.message?.chat?.id ?: return@callbackQuery, next = true)
            }

            callbackQuery("prevInsights") {
                showInsightPosts(bot, callbackQuery.message?.chat?.id ?: return@callbackQuery, next = false)
            }

            callbackQuery("nextInsights") {
                showInsightPosts(bot, callbackQuery.message?.chat?.id ?: return@callbackQuery, next = true)
            }

            telegramError {
                println(error.getErrorMessage())
            }
        }
    }
    bot.startPolling()
}

fun getWeekNews(bot: Bot, id: Long) {

    // Loading week news
    val resultList = arrayListOf<Post>()
    repeat(11) {
        resultList.add(
            Post(
                title = "Пост №$it. Налоговики не могут отказаться провести сверку расчетов",
                date = "7 октября 2022 года",
                sourceUrl = "http://www.consultant.ru/legalnews/20534/"
            )
        )
    }

    usersStateMap[id]?.apply {
        bot.deleteMessage(ChatId.fromId(id), weekPostsMessageId)

        weekPostsList = resultList
        weekPostsPage = 0
        weekPostsMessageId = 0
    }

    showWeekPosts(bot, id)
}

fun showWeekPosts(bot: Bot, id: Long, next: Boolean = true) {
    val currentUserState = usersStateMap[id]!!

    if (next)
        currentUserState.weekPostsPage++
    else
        currentUserState.weekPostsPage--

    val messageText = currentUserState.let { userState ->
        var result = "❖ *Новости в категории -* _${WEEK_NEWS}_\n\n"

        repeat(POSTS_ON_PAGE) { i ->
            if (POSTS_ON_PAGE * (userState.weekPostsPage - 1) + i < userState.weekPostsList.size)
                userState.weekPostsList[POSTS_ON_PAGE * (userState.weekPostsPage - 1) + i].let {
                    result += "❖ _${it.title}_\n» *Дата -* _${it.date}_\n» *Источник -* ${it.sourceUrl}\n\n"
                }
        }

        result += "» *Страница -* _${userState.weekPostsPage}_"

        result
    }

    val replyMarkup = if (currentUserState.weekPostsPage == 1)
        InlineKeyboardMarkup.create(
            listOf(
                InlineKeyboardButton.CallbackData(text = "Следующая страница", callbackData = "nextWeekNew")
            )
        )
    else if (currentUserState.weekPostsPage * POSTS_ON_PAGE > currentUserState.weekPostsList.size)
        InlineKeyboardMarkup.create(
            listOf(
                InlineKeyboardButton.CallbackData(text = "Предыдущая страница", callbackData = "prevWeekNew")
            )
        )
    else
        InlineKeyboardMarkup.create(
            listOf(
                InlineKeyboardButton.CallbackData(text = "Предыдущая страница", callbackData = "prevWeekNew"),
                InlineKeyboardButton.CallbackData(text = "Следующая страница", callbackData = "nextWeekNew")
            )
        )

    if (currentUserState.weekPostsMessageId == 0L)
        currentUserState.weekPostsMessageId = bot.sendMessage(
            ChatId.fromId(id),
            text = messageText,
            replyMarkup = replyMarkup,
            parseMode = ParseMode.MARKDOWN
        ).get().messageId
    else
        bot.editMessageText(
            ChatId.fromId(id),
            currentUserState.weekPostsMessageId,
            text = messageText,
            replyMarkup = replyMarkup,
            parseMode = ParseMode.MARKDOWN
        )
}

fun getTrends(bot: Bot, id: Long) {
    // Trending news
    val resultList = arrayListOf<Post>()
    repeat(11) {
        resultList.add(
            Post(
                title = "Пост №$it. Налоговики не могут отказаться провести сверку расчетов",
                date = "7 октября 2022 года",
                sourceUrl = "http://www.consultant.ru/legalnews/20534/"
            )
        )
    }

    usersStateMap[id]?.apply {
        bot.deleteMessage(ChatId.fromId(id), trendPostsMessageId)

        trendPostsList = resultList
        trendPostsPage = 0
        trendPostsMessageId = 0
    }

    showTrendPosts(bot, id)
}

fun showTrendPosts(bot: Bot, id: Long, next: Boolean = true) {
    val currentUserState = usersStateMap[id]!!

    if (next)
        currentUserState.trendPostsPage++
    else
        currentUserState.trendPostsPage--

    val messageText = currentUserState.let { userState ->
        var result = "❖ *Новости в категории -* _${TRENDS}_\n\n"

        repeat(POSTS_ON_PAGE) { i ->
            if (POSTS_ON_PAGE * (userState.trendPostsPage - 1) + i < userState.trendPostsList.size)
                userState.trendPostsList[POSTS_ON_PAGE * (userState.trendPostsPage - 1) + i].let {
                    result += "❖ _${it.title}_\n» *Дата -* _${it.date}_\n» *Источник -* ${it.sourceUrl}\n\n"
                }
        }

        result += "» *Страница -* _${userState.trendPostsPage}_"

        result
    }

    val replyMarkup = if (currentUserState.trendPostsPage == 1)
        InlineKeyboardMarkup.create(
            listOf(
                InlineKeyboardButton.CallbackData(text = "Следующая страница", callbackData = "nextTrend")
            )
        )
    else if (currentUserState.trendPostsPage * POSTS_ON_PAGE > currentUserState.trendPostsList.size)
        InlineKeyboardMarkup.create(
            listOf(
                InlineKeyboardButton.CallbackData(text = "Предыдущая страница", callbackData = "prevTrend")
            )
        )
    else
        InlineKeyboardMarkup.create(
            listOf(
                InlineKeyboardButton.CallbackData(text = "Предыдущая страница", callbackData = "prevTrend"),
                InlineKeyboardButton.CallbackData(text = "Следующая страница", callbackData = "nextTrend")
            )
        )

    if (currentUserState.trendPostsMessageId == 0L)
        currentUserState.trendPostsMessageId = bot.sendMessage(
            chatId = ChatId.fromId(id),
            text = messageText,
            replyMarkup = replyMarkup,
            parseMode = ParseMode.MARKDOWN
        ).get().messageId
    else
        bot.editMessageText(
            chatId = ChatId.fromId(id),
            messageId = currentUserState.trendPostsMessageId,
            text = messageText,
            replyMarkup = replyMarkup,
            parseMode = ParseMode.MARKDOWN
        )
}

fun getInsights(bot: Bot, id: Long) {
    // Insights
    val resultList = arrayListOf<Post>()
    repeat(11) {
        resultList.add(
            Post(
                title = "Пост №$it. Налоговики не могут отказаться провести сверку расчетов",
                date = "7 октября 2022 года",
                sourceUrl = "http://www.consultant.ru/legalnews/20534/"
            )
        )
    }

    usersStateMap[id]?.apply {
        bot.deleteMessage(ChatId.fromId(id), insightPostsMessageId)

        insightPostsList = resultList
        insightPostsPage = 0
        insightPostsMessageId = 0
    }

    showInsightPosts(bot, id)
}

fun showInsightPosts(bot: Bot, id: Long, next: Boolean = true) {
    val currentUserState = usersStateMap[id]!!

    if (next)
        currentUserState.insightPostsPage++
    else
        currentUserState.insightPostsPage--

    val messageText = currentUserState.let { userState ->
        var result = "❖ *Новости в категории -* _${INSIGHTS}_\n\n"

        repeat(POSTS_ON_PAGE) { i ->
            if (POSTS_ON_PAGE * (userState.insightPostsPage - 1) + i < userState.insightPostsList.size)
                userState.insightPostsList[POSTS_ON_PAGE * (userState.insightPostsPage - 1) + i].let {
                    result += "❖ _${it.title}_\n» *Дата -* _${it.date}_\n» *Источник -* ${it.sourceUrl}\n\n"
                }
        }

        result += "» *Страница -* _${userState.insightPostsPage}_"

        result
    }

    val replyMarkup = if (currentUserState.insightPostsPage == 1)
        InlineKeyboardMarkup.create(
            listOf(
                InlineKeyboardButton.CallbackData(text = "Следующая страница", callbackData = "nextInsights")
            )
        )
    else if (currentUserState.insightPostsPage * POSTS_ON_PAGE > currentUserState.insightPostsList.size)
        InlineKeyboardMarkup.create(
            listOf(
                InlineKeyboardButton.CallbackData(text = "Предыдущая страница", callbackData = "prevInsights")
            )
        )
    else
        InlineKeyboardMarkup.create(
            listOf(
                InlineKeyboardButton.CallbackData(text = "Предыдущая страница", callbackData = "prevInsights"),
                InlineKeyboardButton.CallbackData(text = "Следующая страница", callbackData = "nextInsights")
            )
        )

    if (currentUserState.insightPostsMessageId == 0L)
        currentUserState.insightPostsMessageId = bot.sendMessage(
            ChatId.fromId(id),
            messageText,
            replyMarkup = replyMarkup,
            parseMode = ParseMode.MARKDOWN
        ).get().messageId
    else
        bot.editMessageText(
            ChatId.fromId(id),
            currentUserState.insightPostsMessageId,
            text = messageText,
            replyMarkup = replyMarkup,
            parseMode = ParseMode.MARKDOWN
        )
}

fun generateUsersButton(): List<List<KeyboardButton>> {
    return listOf(
        listOf(KeyboardButton(WEEK_NEWS)),
        listOf(
            KeyboardButton(TRENDS),
            KeyboardButton(INSIGHTS)
        )
    )
}