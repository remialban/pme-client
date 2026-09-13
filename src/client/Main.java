package client;

import javax.smartcardio.*;
import java.io.IOException;
import java.util.*;

public class Main {
    /* Constantes */

    public static final byte CLA_MONAPP = (byte) 0x00;
    /* constants declaration */
    private static final byte GET_VAL_INS = 0x24;// lire un octet du fichier
    // séléctionné
    private static final byte VALIDATE_PIN_INS = 0x26;
    public static byte[] FILE_ID = new byte[] { (byte) 0xA0,
            (byte) 0x00,
            (byte) 0x00,
            (byte) 0x00,
            (byte) 0x62,
            (byte) 0x03,
            (byte) 0x01,
            (byte) 0x0C,
            (byte) 0x06,
            (byte) 0x01};

    public static void main(String[] args) {
        ResponseAPDU r;

        /* Connexion au lecteur */
        TerminalFactory factory = TerminalFactory.getDefault();
        List<CardTerminal> terminals;
        try {
            terminals = factory.terminals().list();
            System.out.println("Terminaux : " + terminals);
            CardTerminal terminal = terminals.get(0);
            /* Connexion à la carte */
            Card card = terminal.connect("T=1");
            System.out.println("Carte : " + card);
            CardChannel channel = card.getBasicChannel();
            // 1. Select File
            CommandAPDU a = new CommandAPDU(0x00, 0xA4, 0x04, 0x00,
                    FILE_ID);
            r = channel.transmit(a);
            if (r.getSW() != 0x9000)
                throw new Exception("File selection failed " + r.getSW());

            /* Menu principal */
            boolean fin = false;
            while (!fin) {
                System.out.println();
                System.out.println("Application cliente Java");
                System.out.println("----------------------------");
                System.out.println();
                System.out.println("0 - Get Name");//not implemented
                System.out.println("1 - Get Value");
                System.out.println("2 - Crediter");//not implemented
                System.out.println("3 - Sign");//not implemented
                System.out.println("4 - Verify Pin");//not implemented
                System.out.println("5 - Quitter");
                System.out.println();
                System.out.println("Votre choix ?");

                int choix = System.in.read();
                while (!(choix >= '1' && choix <= '5')) {
                    choix = System.in.read();
                }
                switch (choix) {
                    case '1':

                        getValue(channel);
                        break;

                    case '4':
                        verifyPin(channel);
                        break;

                    case '5':
                        fin = true;
                        break;
                }
            }

            /* Mise hors tension de la carte */
            card.disconnect(false);

        } catch (CardException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    static void verifyPin(CardChannel channel) throws CardException, IOException {
        /*Scanner scanner = new Scanner(System.in);

        System.out.print("Entrez votre PIN (4 chiffres) : ");
        String input = scanner.nextLine();

        if (input.length() != 4 || !input.matches("\\d{4}")) {
            System.out.println("PIN invalide");
            return;
        }
        byte[] code = new byte[4];

        for (int i = 0; i < 4; i++) {
            code[i] = (byte) (input.charAt(i) - '0');
        }
        System.out.printf("%02X%n", code[0] & 0xFF);
        System.out.println(code[1]);
        System.out.println(code[2]);
        System.out.println(code[3]);
*/
        byte[] pin = new byte[4];

        pin = new byte[] {
                (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04
        };

        CommandAPDU command = new CommandAPDU((byte) 0x80, (byte) 0x20, (byte) 0x00, (byte) 0x00, pin);

        ResponseAPDU r = channel.transmit(command);

        if (r.getSW() != 0x9000) {
            if (r.getSW() == 0x6300) {
                System.out.println("Pin incorrect");
            } else {
                System.out.println("Erreur inconu, code :" + r.getSW());
            }
        } else {
            System.out.println("Code valide");
        }
    }
    static void getValue(CardChannel channel) throws CardException {
        ResponseAPDU r;
        r = channel.transmit(new CommandAPDU((byte) 0x80, (byte) 0x50,
                (byte) 0x00, (byte) 0x00, (byte) 0x02));

        if (r.getSW() != 0x9000) {
            System.out.println("Erreur : status word different de 0x9000 "
                    + r.getSW());
        } else {

            byte[] name = r.getData();
            for (int i = 0; i < name.length; i++) {
                System.out.printf("%02X ", name[i] & 0xFF);
            }
            System.out.println("premier octet: " + name[0]);

        }
    }


}