package by.vshkl.mvp.domain;

import by.vshkl.mvp.model.Quote;
import by.vshkl.repository.Repository;
import io.reactivex.Observable;

public class VoteQuoteUsecase implements Usecase<Boolean> {

    private Repository repository;
    private Quote.VoteState requiredQuoteVoteState;

    public VoteQuoteUsecase(Repository repository) {
        this.repository = repository;
    }

    public void setRequiredQuoteVoteState(Quote.VoteState requiredQuoteVoteState) {
        this.requiredQuoteVoteState = requiredQuoteVoteState;
    }

    @Override
    public Observable<Boolean> execute() {
        return repository.voteQuote(requiredQuoteVoteState);
    }
}
