data class UserState(
    var weekPostsList: ArrayList<Post>,
    var weekPostsPage: Int,
    var weekPostsMessageId: Long,
    var trendPostsList: ArrayList<Post>,
    var trendPostsPage: Int,
    var trendPostsMessageId: Long,
    var insightPostsList: ArrayList<Post>,
    var insightPostsPage: Int,
    var insightPostsMessageId: Long
)
