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
import khttp.structures.authorization.Authorization

private const val API_TOKEN = "5635376755:AAGnKRpLl_h3Xqr1EeEhy1th4dzsrxmfLjI"

private const val WEEK_NEWS = "Новости недели"
private const val TRENDS = "Тренды"
private const val INSIGHTS = "Инсайты"

private const val POSTS_ON_PAGE = 2
private const val HOST = "https://divarteam.ru/v1"

private val usersStateMap = mutableMapOf<Long, UserState>()

enum class Status {
    NOT_AUTHORIZED,
    WAITING_FOR_EMAIL,
    WAITING_FOR_CODE,
    AUTHORIZED
}

enum class AuthRegStatus {
    AUTH,
    REG,
    NONE
}

fun main() {
    val bot = bot {
        token = API_TOKEN
        dispatch {

            message(Filter.Sticker) {
                bot.sendMessage(ChatId.fromId(message.chat.id), text = "Nice sticker bro \\o/")
            }

            text {
                if (usersStateMap[message.chat.id] == null)
                    usersStateMap[message.chat.id] = UserState()

                val currentUser = usersStateMap[message.chat.id]!!

                when (currentUser.status) {


                    Status.NOT_AUTHORIZED -> {
                        bot.sendMessage(
                            ChatId.fromId(message.chat.id),
                            "Отправьте следующим сообщением почту для аутентификации"
                        )
                        currentUser.status = Status.WAITING_FOR_EMAIL
                    }


                    Status.WAITING_FOR_EMAIL -> {
                        currentUser.email = text
                        // Отправить письмо на почту
                        // headers = mapOf("Authorization" to "Bearer ${currentUser.token}")
                        val r = khttp.get(url = "$HOST/reg.send_code", params = mapOf("email" to text))
                        when (r.statusCode) {
                            200 -> {
                                bot.sendMessage(
                                    ChatId.fromId(message.chat.id),
                                    "Вам на почту был отправлен четырёхзначный код. Отправьте его следующим сообщением."
                                )
                                currentUser.email = text
                                currentUser.authStatus = AuthRegStatus.REG
                                currentUser.status = Status.WAITING_FOR_CODE
                            }

                            400 -> {
                                val r2 = khttp.get(url = "$HOST/auth.send_code", params = mapOf("email" to text))
                                when (r2.statusCode) {
                                    200 -> {
                                        bot.sendMessage(
                                            ChatId.fromId(message.chat.id),
                                            "Вам на почту был отправлен четырёхзначный код. Отправьте его следующим сообщением."
                                        )
                                        currentUser.email = text
                                        currentUser.status = Status.WAITING_FOR_CODE
                                        currentUser.authStatus = AuthRegStatus.AUTH
                                    }

                                    else -> {
                                        println(r.statusCode)
                                        bot.sendMessage(
                                            ChatId.fromId(message.chat.id),
                                            "Произошла ошибка. Повторите попытку снова."
                                        )
                                    }
                                }
                            }

                            else -> {
                                println(r.statusCode)
                                bot.sendMessage(
                                    ChatId.fromId(message.chat.id),
                                    "Произошла ошибка. Повторите попытку снова."
                                )
                            }
                        }
                    }


                    Status.WAITING_FOR_CODE -> {
                        // Отправка регистрации
                        val r = if (currentUser.authStatus == AuthRegStatus.REG)
                            khttp.post(url = "$HOST/reg", params = mapOf("email" to currentUser.email, "code" to text))
                        else
                            khttp.get(url = "$HOST/auth", params = mapOf("email" to currentUser.email, "code" to text))

                        println(r.text)

                        when (r.statusCode) {
                            200, 201 -> {
                                val keyboardMarkup =
                                    KeyboardReplyMarkup(keyboard = generateUsersButton(), resizeKeyboard = true)

                                bot.sendMessage(
                                    chatId = ChatId.fromId(message.chat.id),
                                    text = "Авторизация прошла успешно!",
                                    replyMarkup = keyboardMarkup
                                )
                                currentUser.status = Status.AUTHORIZED
                            }

                            else -> {
                                bot.sendMessage(
                                    ChatId.fromId(message.chat.id),
                                    "Произошла ошибка. Повторите попытку снова."
                                )
                                println(r.statusCode)
                            }
                        }
                    }


                    Status.AUTHORIZED -> {
                        println("${ChatId.fromId(message.chat.id)} -> $text")

                        when (text) {
                            WEEK_NEWS ->
                                getWeekNews(bot, message.chat.id)

                            TRENDS ->
                                getTrends(bot, message.chat.id)

                            INSIGHTS ->
                                getInsights(bot, message.chat.id)
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