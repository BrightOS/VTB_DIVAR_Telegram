import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.extensions.filters.Filter
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.ReplyKeyboardRemove
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton

private const val API_TOKEN = "5635376755:AAGnKRpLl_h3Xqr1EeEhy1th4dzsrxmfLjI"

private const val WEEK_NEWS = "Новости недели"
private const val TRENDS = "Тренды"
private const val INSIGHTS = "Инсайты"

private const val POSTS_ON_PAGE = 2

private val commandsList = arrayListOf(WEEK_NEWS, TRENDS, INSIGHTS)
private val usersStateMap = mutableMapOf<Long, UserState>()

private val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
    listOf(InlineKeyboardButton.CallbackData(text = "Следующая страница", callbackData = "nextWeekNew"))
)

fun main() {
    val bot = bot {
        token = "5635376755:AAGnKRpLl_h3Xqr1EeEhy1th4dzsrxmfLjI"
        dispatch {

            message(Filter.Sticker) {
                bot.sendMessage(ChatId.fromId(message.chat.id), text = "Nice sticker bro \\o/")
            }

            command("start") {
                val keyboardMarkup = KeyboardReplyMarkup(keyboard = generateUsersButton(), resizeKeyboard = true)

                if (usersStateMap.keys.contains(message.chat.id))
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = "Вы уже авторизованы.",
                        replyMarkup = keyboardMarkup
                    )
                else {
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = "Авторизация прошла успешно.",
                        replyMarkup = keyboardMarkup
                    )
                    usersStateMap.put(message.chat.id, UserState(arrayListOf(), 0))
                }
            }

            text {
                println("${ChatId.fromId(message.chat.id)} -> $text")

                bot.sendMessage(
                    ChatId.fromId(message.chat.id), when (text) {
                        WEEK_NEWS -> getWeekNews(message.chat.id)
                        TRENDS -> getTrends(message.chat.id)
                        INSIGHTS -> getInsights(message.chat.id)
                        else -> ""
                    },
                    replyMarkup = ReplyKeyboardRemove()
                )

                showNextPosts(bot, message.chat.id)
            }

            audio {
                bot.sendMessage(ChatId.fromId(message.chat.id), text = "Я глухой, але...")
            }

            callbackQuery("nextWeekNew") {
                showNextPosts(bot, callbackQuery.message?.chat?.id ?: return@callbackQuery)
            }

            telegramError {
                println(error.getErrorMessage())
            }
        }
    }
    bot.startPolling()
}

fun showNextPosts(bot: Bot, id: Long) {
    val currentUserState = usersStateMap.get(id)!!

    bot.sendMessage(
        ChatId.fromId(id), currentUserState.let { userState ->
            var result = ""

            repeat(POSTS_ON_PAGE) {
                userState.currentPostsList[POSTS_ON_PAGE * userState.currentPostsPage + it].let {
                    result += "${it.title}\n${it.date}\n${it.sourceUrl}"
                }
                result += "\n"

                if (POSTS_ON_PAGE * userState.currentPostsPage + it == userState.currentPostsList.size)
                    return
            }

            result
        },
        replyMarkup =
        if (currentUserState.currentPostsPage * POSTS_ON_PAGE < currentUserState.currentPostsList.size)
            inlineKeyboardMarkup
        else
            KeyboardReplyMarkup(keyboard = generateUsersButton(), resizeKeyboard = true)
    )
    println("sss")
    currentUserState.currentPostsPage++
}

fun getWeekNews(id: Long): String {

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

    usersStateMap.get(id)?.apply {
        currentPostsList = resultList
        currentPostsPage = 0
    }

    return "Новости недели"
}

fun getTrends(id: Long): String {
    return ""
}

fun getInsights(id: Long): String {
    return ""
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