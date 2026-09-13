package client;

import javax.smartcardio.CardException;

public class CardSWException extends CardException {
    private final int sw;

    public CardSWException(int sw) {
        super(String.format("Card returned status word: %04X", sw));
        this.sw = sw;
    }

    public CardSWException(int sw, String message) {
        super("Card returned status word: " + String.format("%04X", sw) + ". " + message);
        this.sw = sw;
    }

    public int getSW() {
        return sw;
    }
}
