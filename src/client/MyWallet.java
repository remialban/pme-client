package client;


import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import javax.management.Notification;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import java.awt.*;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;

public class MyWallet extends Application {

    WalletService walletService;

    private void buildUI(Stage stage) {

        Font font = Font.loadFont(getClass().getResourceAsStream("/fonts/OpenSans-Bold.ttf"), 24);
        Font.loadFont(getClass().getResourceAsStream("/fonts/OpenSans-Regular.ttf"), 24);

        System.out.println(font.getFamily());
        System.out.println(font.getName());
        stage.setTitle("Wallet");


        // Header
        HBox header = new HBox();

        HBox leftHeader = new HBox();
        HBox rightHeader = new HBox();

        HBox.setHgrow(leftHeader, Priority.ALWAYS);
        HBox.setHgrow(rightHeader, Priority.ALWAYS);

        Label titleApplicationLabel = new Label("Wallet");
        titleApplicationLabel.setStyle(
                "-fx-font-family: 'Open Sans';" +
                        "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;"
        );
        leftHeader.getChildren().addAll(titleApplicationLabel);

        rightHeader.setAlignment(Pos.CENTER_RIGHT);

        Label conectedCardLabel = new Label("Carte :");
        conectedCardLabel.setStyle(

                "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;"
        );


        Label cardStatus = new Label("");
        cardStatus.setStyle(

                "-fx-font-size: 14px;" +
                        "-fx-font-weight: normal;"
        );


        BooleanProperty cardConnected = new SimpleBooleanProperty(false);
        cardStatus.textProperty().bind(Bindings.when(cardConnected).then("Connectée").otherwise("Non connectée"));
        cardStatus.textFillProperty().bind(Bindings.when(cardConnected).then(Color.GREEN).otherwise(Color.RED));
        rightHeader.getChildren().addAll(conectedCardLabel, cardStatus);
        rightHeader.setSpacing(5);

        header.getChildren().addAll(leftHeader, rightHeader);

        // Main content
        VBox mainContent = new VBox();
        mainContent.setAlignment(Pos.CENTER);
        mainContent.setMinHeight(200);
        Label balanceLabel = new Label("Solde :");

        Label balanceValue = new Label("tttt");
        balanceLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold");

        balanceValue.setStyle("-fx-font-size: 24px; -fx-font-weight: normal");
        balanceValue.textProperty().bind(
                Bindings.createStringBinding(
                        () -> cardConnected.get()
                                ? walletService.getAmmount().toString() + " €"
                                : "- €",
                        cardConnected
                )        );
        HBox actionsLayout = new HBox();
        actionsLayout.setSpacing(10);
        actionsLayout.setAlignment(Pos.CENTER);
        Button creditButton = new Button("Créditer");
        creditButton.setPrefWidth(200);
        creditButton.setPrefHeight(100);

        creditButton.setOnAction(actionEvent -> {
                TextInputDialog amountDialog = new TextInputDialog();
                amountDialog.setTitle("Créditer le portefeuille");
                amountDialog.setHeaderText("Entrez le montant à créditer");
                amountDialog.setContentText("Montant :");
                amountDialog.showAndWait().ifPresent(amountStr -> {
                    try {
                        int amount = Integer.parseInt(amountStr);
                        walletService.credit(amount);

                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Crédit réussi");
                        alert.setHeaderText("Le crédit a été effectué avec succès");
                        alert.setContentText("Le montant de " + amount + " € a été crédité sur le portefeuille.");
                        alert.showAndWait();
                    } catch (NumberFormatException e) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Montant invalide");
                        alert.setHeaderText("Le montant saisi est invalide");
                        alert.setContentText("Veuillez entrer un nombre entier.");
                        alert.showAndWait();
                    } catch (CardException | ConnectException | IllegalArgumentException e) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Erreur de communication");
                        alert.setHeaderText("Une erreur est survenue lors de la communication avec la carte");
                        alert.setContentText(e.getMessage());
                        alert.showAndWait();
                    }
                });






        });
        creditButton.disableProperty().bind(cardConnected.not());
        Button debitButton = new Button("Débiter");
        debitButton.setPrefWidth(200);
        debitButton.setPrefHeight(100);
        debitButton.disableProperty().bind(cardConnected.not());

        debitButton.setOnAction(actionEvent -> {
            TextInputDialog amountDialog = new TextInputDialog();
            amountDialog.setTitle("Débiter le portefeuille");
            amountDialog.setHeaderText("Entrez le montant à débiter");
            amountDialog.setContentText("Montant :");
            amountDialog.showAndWait().ifPresent(amountStr -> {
                try {
                    int amount = Integer.parseInt(amountStr);
                    walletService.debit(amount);

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Débit réussi");
                    alert.setHeaderText("Le débit a été effectué avec succès");
                    alert.setContentText("Le montant de " + amount + " € a été débit du portefeuille.");
                    alert.showAndWait();
                } catch (NumberFormatException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Montant invalide");
                    alert.setHeaderText("Le montant saisi est invalide");
                    alert.setContentText("Veuillez entrer un nombre entier.");
                    alert.showAndWait();
                } catch (CardException | ConnectException | IllegalArgumentException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erreur de communication");
                    alert.setHeaderText("Une erreur est survenue lors de la communication avec la carte");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                }
            });






        });

        Button verifyPinButton = new Button("Vérifier code PIN");
        verifyPinButton.setPrefWidth(200);
        verifyPinButton.setPrefHeight(100);
        verifyPinButton.disableProperty().bind(cardConnected.not());

        verifyPinButton.onActionProperty().set(event -> {
            // Handle the verify PIN button action
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Vérification du code PIN");
            dialog.setHeaderText("Entrez votre code PIN");
            dialog.setContentText("Code :");
            dialog.showAndWait().ifPresent(pin -> {
                byte [] pinBytes = new byte[4];
                for (int i = 0; i < 4; i++) {
                    pinBytes[i] = (byte) (pin.charAt(i) - '0');
                }

                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() throws Exception {
                        return walletService.verify(pinBytes);
                    }
                };
// Création et affichage de la fenêtre de chargement
                task.setOnSucceeded(e -> {
                    boolean answer = task.getValue();
                    if (answer) {
                        System.out.println("PIN correct");
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Pin correct");
                        alert.setHeaderText("Le code PIN est correct");
                        alert.setContentText("Vous pouvez maintenant effectuer des opérations.");
                        alert.showAndWait();
                    } else {
                        System.out.println("PIN incorrect");
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Pin incorrect");
                        alert.setHeaderText("Le code PIN est incorrect");
                        alert.setContentText("Veuillez réessayer.");
                        alert.showAndWait();
                    }
                });

                task.setOnFailed(e -> {
                    Throwable exception = task.getException();
                    System.out.println("Erreur lors de la vérification du PIN");
                    exception.printStackTrace();
                });

                Thread thread = new Thread(task);
                thread.setDaemon(true);
                thread.start();
            });




        });

        Button changePinButton = new Button("Changer le code PIN");
        changePinButton.setPrefWidth(200);
        changePinButton.setPrefHeight(100);
        changePinButton.disableProperty().bind(cardConnected.not());

        changePinButton.onActionProperty().set(event -> {
                    // Ask for the old PIN and the new PIN afterwards
                    TextInputDialog oldPinDialog = new TextInputDialog();
                    oldPinDialog.setTitle("Changement du code PIN");
                    oldPinDialog.setHeaderText("Entrez votre ancien code PIN");
                    oldPinDialog.setContentText("Ancien code :");
                    oldPinDialog.showAndWait().ifPresent(oldPin -> {
                        TextInputDialog newPinDialog = new TextInputDialog();
                        newPinDialog.setTitle("Changement du code PIN");
                        newPinDialog.setHeaderText("Entrez votre nouveau code PIN");
                        newPinDialog.setContentText("Nouveau code :");
                        newPinDialog.showAndWait().ifPresent(newPin -> {
                            byte[] oldPinBytes = new byte[4];
                            byte[] newPinBytes = new byte[4];
                            for (int i = 0; i < 4; i++) {
                                oldPinBytes[i] = (byte) (oldPin.charAt(i) - '0');
                                newPinBytes[i] = (byte) (newPin.charAt(i) - '0');
                            }

                            Task<Boolean> task = new Task<>() {
                                @Override
                                protected Boolean call() throws Exception {
                                    walletService.changePin(oldPinBytes, newPinBytes);
                                    return true;
                                }
                            };

                            task.setOnSucceeded(e -> {
                                Boolean result = task.getValue();

                                if (result) {
                                    System.out.println("PIN changed successfully");
                                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                    alert.setTitle("Changement du PIN réussi");
                                    alert.setHeaderText("Le code PIN a été changé avec succès");
                                    alert.setContentText("Vous pouvez maintenant utiliser votre nouveau code PIN.");
                                    alert.showAndWait();
                                } else {
                                    System.out.println("Failed to change PIN");
                                }
                            });

                            task.setOnFailed(e -> {
                                Throwable exception = task.getException();

                                Alert alert = new Alert(Alert.AlertType.ERROR);
                                alert.setTitle("Erreur lors du changement du PIN");
                                alert.setHeaderText("Une erreur est survenue lors du changement du PIN");
                                alert.setContentText(exception.getMessage());
                                alert.showAndWait();
                            });

                            Thread thread = new Thread(task);
                            thread.setDaemon(true);
                            thread.start();
                        });


                    });
                });

        actionsLayout.getChildren().addAll(creditButton, debitButton, verifyPinButton, changePinButton);
        mainContent.getChildren().addAll(balanceLabel, balanceValue, actionsLayout);

        HBox footer = new HBox();


        walletService.setOnTerminalDetected(new Runnable() {
            @Override
            public void run() {

                Platform.runLater(() -> {
                    cardConnected.setValue(walletService.card != null);

                    Integer balance = walletService.getAmmount();


                });
            }
        });




        VBox root = new VBox();
        root.setStyle("-fx-font-family: 'Open Sans'; -fx-font-weight: normal;");
        root.setPadding(new Insets(50));
        root.getChildren().addAl    l(header, mainContent, footer);

        Scene scene = new Scene(root, 700, 800);

        stage.setScene(scene);
        stage.show();

    }

    private void runLoop() {
        Thread thread = new Thread(() -> {
            try {
                walletService.loop();
            } catch (CardException e) {
                e.printStackTrace();
            } catch (ConnectException e) {
                throw new RuntimeException(e);
            }
        });

        thread.setDaemon(true);
        thread.start();

    }
    @Override
    public void start(Stage stage) {
        this.walletService = new WalletService();

        this.buildUI(stage);
        this.runLoop();
    }

    public static void main(String[] args) {
        launch();
    }
}