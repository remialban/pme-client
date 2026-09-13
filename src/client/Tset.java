package client;

import javax.smartcardio.*;
import java.util.List;

public class Tset {

    private static final byte[] WALLET_AID = {
            (byte) 0xA0,
            (byte) 0x00,
            (byte) 0x00,
            (byte) 0x00,
            (byte) 0x62,
            (byte) 0x03,
            (byte) 0x01,
            (byte) 0x0C,
            (byte) 0x06,
            (byte) 0x01
    };

    private static boolean isMyWallet(CardTerminal terminal) throws CardException {

        if (!terminal.isCardPresent()) {
            return false;
        }

        try {
            Card card = terminal.connect("*");

            CardChannel channel = card.getBasicChannel();

            CommandAPDU select = new CommandAPDU(
                    0x00,
                    0xA4,
                    0x04,
                    0x00,
                    WALLET_AID
            );

            ResponseAPDU response = channel.transmit(select);

            boolean walletDetected = response.getSW() == 0x9000;

            card.disconnect(false);

            return walletDetected;

        } catch (CardException e) {
            return false;
        }
    }
    public static void main(String[] args) {

        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            CardTerminals terminals = factory.terminals();

            while (true) {

                // État initial
                List<CardTerminal> list = terminals.list();

                for (CardTerminal terminal : list) {
                    System.out.println("Lecteur : " + terminal.getName());

                    if (terminal.isCardPresent()) {
                        if (isMyWallet(terminal)) {
                            System.out.println(
                                    "✓ Wallet détecté sur : "
                                            + terminal.getName()
                            );
                        } else {
                            System.out.println(
                                    "✗ Autre carte détectée"
                            );
                        }
                    } else {
                        System.out.println("  → Pas de carte");
                    }
                }

                // Attend un changement de lecteur OU de carte
                terminals.waitForChange(0);
            }

        } catch (CardException e) {
            e.printStackTrace();
        }


    }
}
