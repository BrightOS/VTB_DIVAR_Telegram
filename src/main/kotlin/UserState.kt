data class UserState(
    var token: String = "",
    var status: Status = Status.NOT_AUTHORIZED,
    var authStatus: AuthRegStatus = AuthRegStatus.NONE,
    var email: String = "",
    var weekPostsList: ArrayList<Post> = arrayListOf(),
    var weekPostsPage: Int = 0,
    var weekPostsMessageId: Long = 0L,
    var trendPostsList: ArrayList<Post> = arrayListOf(),
    var trendPostsPage: Int = 0,
    var trendPostsMessageId: Long = 0L,
    var insightPostsList: ArrayList<Post>  = arrayListOf(),
    var insightPostsPage: Int = 0,
    var insightPostsMessageId: Long = 0L
)
