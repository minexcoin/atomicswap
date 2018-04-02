package com.minexcoin.atomic_swap.gui;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.InetSocketAddress;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import com.minexcoin.fsm.FSM;
import com.minexcoin.atomic_swap.AtomicSwapFSM;
import com.minexcoin.atomic_swap.fsm.data.Data;
import com.minexcoin.atomic_swap.fsm.data.SimpleData;
import com.minexcoin.atomic_swap.fsm.states.TxState;
import com.minexcoin.atomic_swap.fsm.states.TxStatus;
import com.minexcoin.atomic_swap.workers.BitcoinWorker;
import com.minexcoin.atomic_swap.workers.MinexCoinWorker;

import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.core.ECKey;

public class HomePageController implements Initializable {
    
    @FXML
    private Pane homePagePane;
    @FXML
    private ChoiceBox typeChoiceBox;
    @FXML
    private TextArea statusTextArea;
    @FXML
    private TextField myInetHostTextField;
    @FXML
    private TextField myInetPortTextField;
    @FXML
    private TextField partnerInetHostTextField;
    @FXML
    private TextField partnerInetPortTextField;
    @FXML
    
    private TextField minexLoginTextField;
    @FXML
    private TextField minexPasswordTextField;
    @FXML
    private TextField minexZMQPortTextField;
    @FXML
    private TextField minexMyPrivKeyTextField;
    @FXML
    private TextField minexPartnerPubKeyTextField;
    @FXML
    private TextField minexAmountTextField;
    @FXML
    private TextField minexConfirmationsTextField;
    @FXML
    private TextField minexExpireTextField;
    
    @FXML
    private TextField bitcoinLoginTextField;
    @FXML
    private TextField bitcoinPasswordTextField;
    @FXML
    private TextField bitcoinZMQPortTextField;
    @FXML
    private TextField bitcoinMyPrivKeyTextField;
    @FXML
    private TextField bitcoinPartnerPubKeyTextField;
    @FXML
    private TextField bitcoinAmountTextField;
    @FXML
    private TextField bitcoinConfirmationsTextField;
    @FXML
    private TextField bitcoinExpireTextField;
    @FXML
    private Button exchangeButton;
    
    private final Data selfData = new SimpleData();
    private final Data partnerData = new SimpleData();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        typeChoiceBox.setItems(FXCollections.observableArrayList("Buy Minexcoin", "Sell Minexcoin"));
        typeChoiceBox.getSelectionModel().selectFirst();
    }
    
    @SuppressWarnings("restriction")
	@FXML
    private void exchange(ActionEvent event) {
        statusTextArea.setText("");
        
        disableForm();
        try {
            setUpParams();
        } catch (Throwable e) {
            statusTextArea.appendText("Error: " + e.getMessage() + "\n");
            enableForm();
            return;
        }

        statusTextArea.appendText("---- Starting process ----" + "\n");
        
        final FSM<TxState<TxStatus>> fsm = AtomicSwapFSM.create(
            selfData, partnerData,
            true,
            5, TimeUnit.SECONDS,
            5, TimeUnit.SECONDS,
            1000, "AtomicSwapFSM Event", "AtomicSwapFSM-Task-%d"
        );
        
        fsm.states().subscribe(
            state -> {
                Platform.runLater(() -> {
                    statusTextArea.appendText(state.status() + "\n");
                });

                switch(state.status()) {
                    case SellerTx: {
                    	Platform.runLater(() -> {
                    		statusTextArea.appendText("- Generated secret: " + state.selfData().secretHash() + "\n");
                    		statusTextArea.appendText("- Funding TX out point: " + state.selfData().txOutPoint() + "\n");
                    	});
                        break;
                    }
                    case BuyerTx: {
                    	Platform.runLater(() -> {
                    		statusTextArea.appendText("- Funding TX out point: " + state.selfData().txOutPoint() + "\n");
                    	});
                        break;
                    }
                    case Finish: {
                        if (state.selfData().closeTx() != null) {
                        	Platform.runLater(() -> {
                        		statusTextArea.appendText("- Close TX: " + state.selfData().closeTx() + "\n");
                        	});
                        } else {
                        	Platform.runLater(() -> {
                        		statusTextArea.appendText("- Close TX: " + state.partnerData().closeTx() + "\n");
                        	});
                        }
                        Platform.runLater(() -> {
                        	statusTextArea.appendText("---- Atomic swap is done ----");
                        });
                        enableForm();
                        break;
                    }
                default:
                    break;
                }
            },
            e -> {
                Platform.runLater(() -> {
                	StringWriter errors = new StringWriter();
                	e.printStackTrace(new PrintWriter(errors));
                    statusTextArea.appendText(errors.toString());
                    enableForm();
                });
            }
        );

        fsm.start();
    }
    
    /**
     * Set up all properties from according form fields.
     */
    private void setUpParams() throws Throwable {
    	// Self
        selfData.inetAddress(FormValidator.getSocketAddress(
        	myInetHostTextField.getText(),
            myInetPortTextField.getText()
        ));
        
        // Partner
        partnerData.inetAddress(FormValidator.getSocketAddress(
            partnerInetHostTextField.getText(),
            partnerInetPortTextField.getText()
        ));
        
        if (typeChoiceBox.getSelectionModel().getSelectedIndex() == 0) {
            // Buy MNX
            
            selfData.worker(BitcoinWorker.instance());
            partnerData.worker(MinexCoinWorker.instance());
            
            // Self
            selfData.nodeAddress(new InetSocketAddress("127.0.0.1", 18332));
            
            selfData.nodeLogin(FormValidator.getCredential(
                "Bitcoin RPC login", bitcoinLoginTextField.getText()
            ));
            
            selfData.nodePassword(FormValidator.getCredential(
                "Bitcoin RPC password", bitcoinPasswordTextField.getText()
            ));
            
            selfData.notificationPort(FormValidator.getPortNumber(
                "Bitcoin notification port", bitcoinZMQPortTextField.getText()
            ));
            
            selfData.myKey(ECKey.fromPrivate(
                FormValidator.getPrivateKey(
                    "My Bitcoin priv key", bitcoinMyPrivKeyTextField.getText()
                ),
                false
            ));
            
            selfData.otherKey(ECKey.fromPublicOnly(
                FormValidator.getPublicKey(
                    "Partner Bitcoin pub key", bitcoinPartnerPubKeyTextField.getText()
                )
            ));

            selfData.amount(FormValidator.getAmount(
                "Bitcoin amount", bitcoinAmountTextField.getText()
            ));
            
            selfData.confirmations(FormValidator.getConfirmations(
                "Bitcoin amount of confirmations", bitcoinConfirmationsTextField.getText()
            ));
            
            selfData.csv(FormValidator.getConfirmations(
                "Bitcoin expire", bitcoinExpireTextField.getText()
            ));
            
            // Partner
            partnerData.nodeAddress(new InetSocketAddress("127.0.0.1", 17788));
            
            partnerData.nodeLogin(FormValidator.getCredential(
                "Minexcoin RPC login", minexLoginTextField.getText()
            ));
            
            partnerData.nodePassword(FormValidator.getCredential(
                "Minexcoin RPC password", minexPasswordTextField.getText()
            ));
            
            partnerData.notificationPort(FormValidator.getPortNumber(
                "Minexcoin notification port", minexZMQPortTextField.getText()
            ));
            
            partnerData.myKey(ECKey.fromPrivate(
                FormValidator.getPrivateKey(
                    "My Minexcoin priv key", minexMyPrivKeyTextField.getText()
                ),
                false
            ));
            
            partnerData.otherKey(ECKey.fromPublicOnly(
                FormValidator.getPublicKey(
                    "Partner Minxcoin pub key", minexPartnerPubKeyTextField.getText()
                )
            ));
            
            partnerData.amount(FormValidator.getAmount(
                "Minexcoin amount", minexAmountTextField.getText()
            ));
            
            partnerData.confirmations(FormValidator.getConfirmations(
                "Minexcoin amount of confirmations", minexConfirmationsTextField.getText()
            ));
            
            partnerData.csv(FormValidator.getConfirmations(
                "Minexcoin expire", minexExpireTextField.getText()
            ));
        } else {
            // Sell MNX
            
            selfData.worker(MinexCoinWorker.instance());
            partnerData.worker(BitcoinWorker.instance());
            
            // Self
            selfData.nodeAddress(new InetSocketAddress("127.0.0.1", 17788));
            
            selfData.nodeLogin(FormValidator.getCredential(
                "Minexcoin RPC login", minexLoginTextField.getText()
            ));
            
            selfData.nodePassword(FormValidator.getCredential(
                "Minexcoin RPC password", minexPasswordTextField.getText()
            ));
            
            selfData.notificationPort(FormValidator.getPortNumber(
                "Minexcoin notification port", minexZMQPortTextField.getText()
            ));
            
            selfData.myKey(ECKey.fromPrivate(
                FormValidator.getPrivateKey(
                    "My Minexcoin priv key", minexMyPrivKeyTextField.getText()
                ),
                false
            ));
            
            selfData.otherKey(ECKey.fromPublicOnly(
                FormValidator.getPublicKey(
                    "Partner Minxcoin pub key", minexPartnerPubKeyTextField.getText()
                )
            ));
            
            selfData.amount(FormValidator.getAmount(
                "Minexcoin amount", minexAmountTextField.getText()
            ));
            
            selfData.confirmations(FormValidator.getConfirmations(
                "Minexcoin amount of confirmations", minexConfirmationsTextField.getText()
            ));
            
            selfData.csv(FormValidator.getConfirmations(
                "Minexcoin expire", minexExpireTextField.getText()
            ));
            
            // Partner
            partnerData.nodeAddress(new InetSocketAddress("127.0.0.1", 18332));
            
            partnerData.nodeLogin(FormValidator.getCredential(
                "Bitcoin RPC login", bitcoinLoginTextField.getText()
            ));
            
            partnerData.nodePassword(FormValidator.getCredential(
                "Bitcoin RPC password", bitcoinPasswordTextField.getText()
            ));
            
            partnerData.notificationPort(FormValidator.getPortNumber(
                "Bitcoin notification port", bitcoinZMQPortTextField.getText()
            ));
            
            partnerData.myKey(ECKey.fromPrivate(
                FormValidator.getPrivateKey(
                    "My Bitcoin priv key", bitcoinMyPrivKeyTextField.getText()
                ),
                false
            ));
            
            partnerData.otherKey(ECKey.fromPublicOnly(
                FormValidator.getPublicKey(
                    "Partner Bitcoin pub key", bitcoinPartnerPubKeyTextField.getText()
                )
            ));
            
            partnerData.amount(FormValidator.getAmount(
                "Bitcoin amount", bitcoinAmountTextField.getText()
            ));
            
            partnerData.confirmations(FormValidator.getConfirmations(
                "Bitcoin amount of confirmations", bitcoinConfirmationsTextField.getText()
            ));
            
            partnerData.csv(FormValidator.getConfirmations(
                "Bitcoin expire", bitcoinExpireTextField.getText()
            ));
        }
        
        selfData.netParams(TestNet3Params.get());
        partnerData.netParams(TestNet3Params.get());
    }
    
    /**
     * Make all fields of form disabled.
     */
    private void disableForm() {
        myInetHostTextField.setDisable(true);
        myInetPortTextField.setDisable(true);
        partnerInetHostTextField.setDisable(true);
        partnerInetPortTextField.setDisable(true);
        
        minexLoginTextField.setDisable(true);
        minexPasswordTextField.setDisable(true);
        minexZMQPortTextField.setDisable(true);
        minexMyPrivKeyTextField.setDisable(true);
        minexPartnerPubKeyTextField.setDisable(true);
        minexAmountTextField.setDisable(true);
        minexConfirmationsTextField.setDisable(true);
        minexExpireTextField.setDisable(true);
        
        bitcoinLoginTextField.setDisable(true);
        bitcoinPasswordTextField.setDisable(true);
        bitcoinZMQPortTextField.setDisable(true);
        bitcoinMyPrivKeyTextField.setDisable(true);
        bitcoinPartnerPubKeyTextField.setDisable(true);
        bitcoinAmountTextField.setDisable(true);
        bitcoinConfirmationsTextField.setDisable(true);
        bitcoinExpireTextField.setDisable(true);

        typeChoiceBox.setDisable(true);
        exchangeButton.setDisable(true);
    }
    
    /**
     * Make all fields of form enabled again.
     */
    private void enableForm() {
        myInetHostTextField.setDisable(false);
        myInetPortTextField.setDisable(false);
        partnerInetHostTextField.setDisable(false);
        partnerInetPortTextField.setDisable(false);
        
        minexLoginTextField.setDisable(false);
        minexPasswordTextField.setDisable(false);
        minexZMQPortTextField.setDisable(false);
        minexMyPrivKeyTextField.setDisable(false);
        minexPartnerPubKeyTextField.setDisable(false);
        minexAmountTextField.setDisable(false);
        minexConfirmationsTextField.setDisable(false);
        minexExpireTextField.setDisable(false);
        
        bitcoinLoginTextField.setDisable(false);
        bitcoinPasswordTextField.setDisable(false);
        bitcoinZMQPortTextField.setDisable(false);
        bitcoinMyPrivKeyTextField.setDisable(false);
        bitcoinPartnerPubKeyTextField.setDisable(false);
        bitcoinAmountTextField.setDisable(false);
        bitcoinConfirmationsTextField.setDisable(false);
        bitcoinExpireTextField.setDisable(false);
        
        typeChoiceBox.setDisable(false);
        exchangeButton.setDisable(false);
    }
}
