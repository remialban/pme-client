package client;

import javax.management.InstanceNotFoundException;
import javax.smartcardio.*;
import java.net.ConnectException;
import java.util.List;

public class WalletService {
    private Integer ammount;
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
    private List<CardTerminal> terminals;

    private Runnable onTerminalDetected;

    Card card;
    CardChannel cardChannel;

    public void setOnTerminalDetected(Runnable callback) {
        this.onTerminalDetected = callback;
    }

    private CardTerminal terminal;

    public WalletService() {
        try {
            loadTerminals();
        } catch (CardException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadTerminals() throws CardException {
        TerminalFactory factory = TerminalFactory.getDefault();
        this.terminals = factory.terminals().list();
    }

    public void setTerminal(String name) throws InstanceNotFoundException {
        for (CardTerminal cardTerminal: terminals) {
            if (cardTerminal.equals(name)) {
                this.terminal = cardTerminal;
                return;
            }
        }

        throw new InstanceNotFoundException("Le terminal n'a pas été trouvé");

    }

    public void loop() throws CardException, ConnectException {
        TerminalFactory factory = TerminalFactory.getDefault();

        javax.smartcardio.CardTerminals terminals = factory.terminals();

        while (true) {
            this.terminals = terminals.list();

            System.out.println("==============================");
            boolean changeDetected = terminals.waitForChange(1000);

            if (!changeDetected) {
                continue;
            }

            if (onTerminalDetected != null) {
                onTerminalDetected.run();
            }
            System.out.println("nouveau");
            terminals = factory.terminals();

            this.card = null;
            this.ammount = null;
            for (CardTerminal terminal: terminals.list()) {
                Card mycard = this.isMyWallet(terminal);

                if(mycard != null) {
                    this.card = mycard;
                    this.cardChannel = mycard.getBasicChannel();
                    this.terminal = terminal;
                    this.selectWallet();
                    this.updateAmmount();
                    break;
                }
            }
            System.out.println("ici");
            this.onTerminalDetected.run();

            if (onTerminalDetected != null) {
                System.out.println("las bas");
            }


        }
    }

    public List<CardTerminal> getTerminals() {
        return terminals;
    }

    private Card isMyWallet(CardTerminal terminal) throws CardException {

        if (!terminal.isCardPresent()) {
            System.out.println("CARTE NON PRESENTE");
            return null;
        }
        System.out.println("CARTE PRESSENTE");
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
            System.out.println("premier ismywillet");
            System.out.println(card);

            System.out.println("deuxieme ismywillet");

            if (walletDetected) {
                return card;
            } else {
                return null;
            }
        } catch (CardException e) {
            return null;
        }
    }

    public void updateAmmount() throws CardException, ConnectException {
        CardChannel channel = card.getBasicChannel();


        CommandAPDU select = new CommandAPDU(
                0x80,
                0x50,
                0x00,
                0x00,
                0x02
        );

        ResponseAPDU response = channel.transmit(select);

        if (response.getSW() != 0x9000) {
            System.out.println(response.getSW());

            throw new ConnectException("Error on getting balance");
        }

        byte[] data = response.getData();

        int amount = ((data[0] & 0xFF) << 8)
                | (data[1] & 0xFF);

        this.ammount = amount;

        System.out.println(amount);

        if (this.onTerminalDetected != null) {
            this.onTerminalDetected.run();
        }
    }

    public boolean verify(byte[] code) throws CardException, ConnectException {
        System.out.println("Carte :");
        System.out.println(card);
        System.out.println("test avant channel");

        // display code in format 0xXX ...
        System.out.print("Code: ");
        for (byte b : code) {
            System.out.printf("0x%02X ", b);
        }
        System.out.println("test apres channel");
        CommandAPDU select = new CommandAPDU(
                0x00,
                0xA4,
                0x04,
                0x00,
                WALLET_AID
        );
        System.out.println("apres creation object command");
        ResponseAPDU response = cardChannel.transmit(select);
        System.out.println("select fait !");
        if (response.getSW() != 0x9000) {
            throw new ConnectException("Error");
        }


        select = new CommandAPDU(
                0x80,
                0x20,
                0x00,
                0x00,
            code
        );

        System.out.println("T");
        response = cardChannel.transmit(select);

        System.out.println("select2 fait");
        if (response.getSW() == 0x6300) {
            return false;
        }
        if (response.getSW() != 0x9000) {
            System.out.println(response.getSW());

            throw new ConnectException("Problem with APDU COMMANDE");
        }

        return true;
    }

    private void selectWallet() throws CardException, ConnectException {
        CommandAPDU select = new CommandAPDU(
                0x00,
                0xA4,
                0x04,
                0x00,
                WALLET_AID
        );

        ResponseAPDU response = cardChannel.transmit(select);

        if (response.getSW() != 0x9000) {
            throw new ConnectException("Error");
        }
    }



    public void credit(int amount) throws CardException, ConnectException {
        // this.selectWallet();

        if (amount < 0 || amount > 127) {
            throw new IllegalArgumentException("Amount must be between 0 and 127");
        }

        CommandAPDU credit = new CommandAPDU(
                0x80,
                0x30,
                0x00,
                0x00,
                new byte[]{(byte) amount},
                0
        );

        ResponseAPDU response = cardChannel.transmit(credit);

        if (response.getSW() == 0x6301) {
            throw new CardSWException(response.getSW(), "PIN required");
        }

        switch (response.getSW()) {
            case 0x6301:
                throw new CardSWException(response.getSW(), "PIN required");
            case 0x6A83:
                throw new CardSWException(response.getSW(), "Invalid amount");
            case 0x6A84:
                throw new CardSWException(response.getSW(), "Exceeding maximum balance");
            case 0x9000:
                this.updateAmmount();
                break;
            default:
                throw new CardSWException(response.getSW(), "Unknown error");
        }

    }

    public void debit(int amount) throws CardException, ConnectException {
        // this.selectWallet();

        if (amount < 0 || amount > 127) {
            throw new IllegalArgumentException("Amount must be between 0 and 127");
        }

        CommandAPDU debit = new CommandAPDU(
                0x80,
                0x40,
                0x00,
                0x00,
                new byte[]{(byte) amount},
                0
        );

        ResponseAPDU response = cardChannel.transmit(debit);

        if (response.getSW() == 0x6301) {
            throw new CardSWException(response.getSW(), "PIN required");
        }

        switch (response.getSW()) {
            case 0x6301:
                throw new CardSWException(response.getSW(), "PIN required");
            case 0x6A83:
                throw new CardSWException(response.getSW(), "Invalid amount");
            case 0x6A85:
                throw new CardSWException(response.getSW(), "Negative balance not allowed");
            case 0x9000:
                this.updateAmmount();
                break;
            default:
                throw new CardSWException(response.getSW(), "Unknown error");
        }

    }

    public void changePin(byte[] oldPin, byte[] newPin) throws CardException {
        if (oldPin.length != 4 || newPin.length != 4) {
            throw new IllegalArgumentException("PIN must be 4 bytes long");
        }

        byte[] data = new byte[8];

        System.arraycopy(oldPin, 0, data, 0, 4);
        System.arraycopy(newPin, 0, data, 4, 4);

        CommandAPDU changePin = new CommandAPDU(
                0x80,
                0x24,
                0x00,
                0x00,
                data,
                0
        );

        ResponseAPDU response = cardChannel.transmit(changePin);

        switch (response.getSW()) {
            case 0x6300:
                throw new CardSWException(response.getSW(), "Incorrect old PIN");
            case 0x9000:
                break;
            default:
                throw new CardSWException(response.getSW());
        }

    }

    public Integer getAmmount() {
        return ammount;
    }
}
