package by.vshkl.bashq.view

interface QuoteActionListener {

    fun share(content: String)

    fun vote(actionLink: String, action: String)

    fun onVoteSuccess(message: String)

    fun onVoteError(errorMessage: String)
}