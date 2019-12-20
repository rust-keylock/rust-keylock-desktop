// Copyright 2019 astonbitecode
// This file is part of rust-keylock password manager.
//
// rust-keylock is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// rust-keylock is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with rust-keylock.  If not, see <http://www.gnu.org/licenses/>.
package org.rustkeylock.controllers;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextArea;
import com.jfoenix.controls.JFXTextField;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.HBox;
import org.rustkeylock.japi.JavaEntry;
import org.rustkeylock.japi.stubs.GuiResponse;
import org.rustkeylock.japi.stubs.JavaMenu;
import org.rustkeylock.ui.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ShowEntryController extends BaseController implements RklController, Initializable {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private Consumer<Object> callback;
    @FXML
    private JFXTextField titleTextField = new JFXTextField();
    @FXML
    private StringProperty titleMessage = new SimpleStringProperty("");
    @FXML
    private JFXTextField urlTextField = new JFXTextField();
    @FXML
    private StringProperty urlMessage = new SimpleStringProperty("");
    @FXML
    private JFXTextField usernameTextField = new JFXTextField();
    @FXML
    private StringProperty usernameMessage = new SimpleStringProperty("");
    @FXML
    private JFXTextField passwordTextField = new JFXTextField();
    @FXML
    private StringProperty passwordMessage = new SimpleStringProperty("");
    @FXML
    private JFXTextArea descriptionTextArea = new JFXTextArea();
    @FXML
    private StringProperty descriptionMessage = new SimpleStringProperty("");
    @FXML
    private StringProperty leftButtonTooltip = new SimpleStringProperty("");
    @FXML
    private StringProperty rightButtonTooltip = new SimpleStringProperty("");
    @FXML
    private HBox righButtonsHBox = new HBox();
    @FXML
    private final StringProperty deleteButtonId = new SimpleStringProperty(DELETE_BUTTON_ID);
    @FXML
    private final StringProperty okButtonId = new SimpleStringProperty(OK_BUTTON_ID);
    @FXML
    private final StringProperty cautionButtonId = new SimpleStringProperty(CAUTION_BUTTON_ID);
    @FXML
    private HBox leftButtonsHBox = new HBox();
    @FXML
    private final StringProperty editButtonId = new SimpleStringProperty(EDIT_BUTTON_ID);
    @FXML
    private final StringProperty cancelButtonId = new SimpleStringProperty(CANCEL_BUTTON_ID);
    @FXML
    private HBox showHidePasswordHBox = new HBox();
    @FXML
    private final StringProperty showPasswordButtonId = new SimpleStringProperty(SHOW_PASSWORD_BUTTON_ID);
    @FXML
    private final StringProperty hidePasswordButtonId = new SimpleStringProperty(HIDE_PASSWORD_BUTTON_ID);
    @FXML
    private JFXButton showPasswordButton;
    @FXML
    private JFXButton hidePasswordButton;

    private static final String DELETE_BUTTON_ID = "deleteButton";
    private static final String OK_BUTTON_ID = "okButton";
    private static final String CAUTION_BUTTON_ID = "cautionButton";
    private static final String EDIT_BUTTON_ID = "editButton";
    private static final String CANCEL_BUTTON_ID = "cancelButton";
    private static final String SHOW_PASSWORD_BUTTON_ID = "showPassword";
    private static final String HIDE_PASSWORD_BUTTON_ID = "hidePassword";
    private static final String MASK_UTF_CHAR = "\u2022";
    private static final String MASK_STRING = Stream.iterate(0, n -> n + 1)
            .limit(9)
            .map(i -> MASK_UTF_CHAR)
            .collect(Collectors.joining(""));

    private final JavaEntry anEntry;
    private final Integer entryIndex;
    private final Boolean edit;
    private final Boolean delete;

    public ShowEntryController(JavaEntry anEntry, Integer entryIndex, Boolean edit, Boolean delete) {
        this.anEntry = anEntry;
        this.entryIndex = entryIndex;
        this.edit = edit;
        this.delete = delete;
    }

    @Override
    public void setCallback(Consumer<Object> consumer) {
        this.callback = consumer;
    }

    @Override
    Consumer<Object> getCallback() {
        return this.callback;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        titleTextField.setEditable(edit);
        titleTextField.setDisable(!edit);
        titleTextField.setText(anEntry.getName());

        urlTextField.setEditable(edit);
        urlTextField.setDisable(!edit);
        urlTextField.setText(anEntry.getUrl());

        usernameTextField.setEditable(edit);
        usernameTextField.setDisable(!edit);
        usernameTextField.setText(anEntry.getUser());

        passwordTextField.setEditable(edit);
        passwordTextField.setDisable(!edit);
        if (!anEntry.getPass().isEmpty()) {
            passwordTextField.setText(MASK_STRING);
        }
        Utils.removeChildNodeById(showHidePasswordHBox, HIDE_PASSWORD_BUTTON_ID);

        descriptionTextArea.setEditable(edit);
        descriptionTextArea.setDisable(!edit);
        descriptionTextArea.setText(anEntry.getDesc());

        if (!edit && !delete) {
            enableMenuButtons();
            Utils.removeChildNodeById(leftButtonsHBox, CANCEL_BUTTON_ID);
            setLeftButtonTooltip("Edit");
            setRightButtonTooltip("Delete");
            Utils.removeChildNodeById(righButtonsHBox, OK_BUTTON_ID);
            Utils.removeChildNodeById(righButtonsHBox, CAUTION_BUTTON_ID);
        } else if (edit && !delete) {
            disableMenuButtons();
            Utils.removeChildNodeById(leftButtonsHBox, EDIT_BUTTON_ID);
            setLeftButtonTooltip("Cancel");
            Utils.removeChildNodeById(righButtonsHBox, DELETE_BUTTON_ID);
            Utils.removeChildNodeById(righButtonsHBox, CAUTION_BUTTON_ID);
            setRightButtonTooltip("OK");
        } else if (!edit && delete) {
            disableMenuButtons();
            Utils.removeChildNodeById(leftButtonsHBox, EDIT_BUTTON_ID);
            setLeftButtonTooltip("Cancel");
            Utils.removeChildNodeById(righButtonsHBox, DELETE_BUTTON_ID);
            Utils.removeChildNodeById(righButtonsHBox, OK_BUTTON_ID);
            setRightButtonTooltip("Yes I am sure, delete it!");
        } else {
            enableMenuButtons();
            logger.error("Undefined state reached: edit = " + edit + ", delete = " + delete);
            Utils.removeChildNodeById(leftButtonsHBox, EDIT_BUTTON_ID);
            Utils.removeChildNodeById(leftButtonsHBox, CANCEL_BUTTON_ID);
            Utils.removeChildNodeById(righButtonsHBox, DELETE_BUTTON_ID);
            Utils.removeChildNodeById(righButtonsHBox, OK_BUTTON_ID);
            Utils.removeChildNodeById(righButtonsHBox, CAUTION_BUTTON_ID);
        }
    }

    @FXML
    private void toggleRevealPassword(ActionEvent event) {
        event.consume();
        if (passwordTextField.getText().startsWith(MASK_UTF_CHAR)) {
            passwordTextField.setText(anEntry.getPass());
            Utils.removeChildNodeById(showHidePasswordHBox, SHOW_PASSWORD_BUTTON_ID);
            Utils.addNode(showHidePasswordHBox, hidePasswordButton);
        } else {
            passwordTextField.setText(MASK_STRING);
            Utils.removeChildNodeById(showHidePasswordHBox, HIDE_PASSWORD_BUTTON_ID);
            Utils.addNode(showHidePasswordHBox, showPasswordButton);
        }
    }

    @FXML
    private void copyUsernameAction(ActionEvent event) {
        event.consume();
        getCallback().accept(GuiResponse.Copy(anEntry.user));
    }

    @FXML
    private void copyUrlAction(ActionEvent event) {
        event.consume();
        getCallback().accept(GuiResponse.Copy(anEntry.url));
    }

    @FXML
    private void copyPasswordAction(ActionEvent event) {
        event.consume();
        getCallback().accept(GuiResponse.Copy(anEntry.pass));
    }

    @FXML
    private void leftButtonAction(ActionEvent event) {
        event.consume();
        if (!edit && !delete) {
            editEntryAction();
        } else {
            getCallback().accept(GuiResponse.GoToMenu(JavaMenu.EntriesList("")));
        }
    }

    @FXML
    private void rightButtonAction(ActionEvent event) {
        event.consume();
        if (!edit && !delete) {
            deleteButtonAction();
        } else if (edit && !delete) {
            entryOkAction();
        } else if (!edit && delete) {
            areYouSureAction();
        }
    }

    private void editEntryAction() {
        getCallback().accept(GuiResponse.GoToMenu(JavaMenu.EditEntry(entryIndex)));
    }

    private void deleteButtonAction() {
        getCallback().accept(GuiResponse.GoToMenu(JavaMenu.DeleteEntry(entryIndex)));
    }

    private void entryOkAction() {
        setTitleMessage("");
        setUsernameMessage("");
        setPasswordMessage("");

        JavaEntry entry = new JavaEntry();
        entry.name = titleTextField.getText();
        entry.url = urlTextField.getText();
        entry.user = usernameTextField.getText();
        entry.pass = passwordTextField.getText();
        entry.desc = descriptionTextArea.getText();

        logger.info("Saving entry " + entry.name);

        boolean errorsExist = false;
        if (entry.name.isEmpty()) {
            setTitleMessage("Required Field");
            errorsExist = true;
        }
        if (entry.user.isEmpty()) {
            setUsernameMessage("Required Field");
            errorsExist = true;
        }
        if (entry.pass.isEmpty()) {
            setPasswordMessage("Required Field");
            errorsExist = true;
        }
        if (!entry.url.isBlank()) {
            try {
                new URL(entry.url);
            } catch (MalformedURLException error) {
                setUrlMessage("Wrong URL. Eg: https://my.com");
                errorsExist = true;
            }
        }
        if (!errorsExist) {
            if (entryIndex >= 0) {
                callback.accept(GuiResponse.ReplaceEntry(entry, entryIndex));
            } else {
                callback.accept(GuiResponse.AddEntry(entry));
            }
        }
    }

    private void areYouSureAction() {
        callback.accept(GuiResponse.DeleteEntry(entryIndex));
    }

    public String getTitleMessage() {
        return titleMessage.get();
    }

    public StringProperty titleMessageProperty() {
        return titleMessage;
    }

    public void setTitleMessage(String titleMessage) {
        this.titleMessage.set(titleMessage);
    }

    public String getUrlMessage() {
        return urlMessage.get();
    }

    public StringProperty urlMessageProperty() {
        return urlMessage;
    }

    public void setUrlMessage(String urlMessage) {
        this.urlMessage.set(urlMessage);
    }

    public String getUsernameMessage() {
        return usernameMessage.get();
    }

    public StringProperty usernameMessageProperty() {
        return usernameMessage;
    }

    public void setUsernameMessage(String usernameMessage) {
        this.usernameMessage.set(usernameMessage);
    }

    public String getPasswordMessage() {
        return passwordMessage.get();
    }

    public StringProperty passwordMessageProperty() {
        return passwordMessage;
    }

    public void setPasswordMessage(String passwordMessage) {
        this.passwordMessage.set(passwordMessage);
    }

    public String getDescriptionMessage() {
        return descriptionMessage.get();
    }

    public StringProperty descriptionMessageProperty() {
        return descriptionMessage;
    }

    public void setDescriptionMessage(String descriptionMessage) {
        this.descriptionMessage.set(descriptionMessage);
    }

    public JFXTextField getTitleTextField() {
        return titleTextField;
    }

    public void setTitleTextField(JFXTextField titleTextField) {
        this.titleTextField = titleTextField;
    }

    public JFXTextField getUrlTextField() {
        return urlTextField;
    }

    public void setUrlTextField(JFXTextField urlTextField) {
        this.urlTextField = urlTextField;
    }

    public JFXTextField getUsernameTextField() {
        return usernameTextField;
    }

    public void setUsernameTextField(JFXTextField usernameTextField) {
        this.usernameTextField = usernameTextField;
    }

    public JFXTextField getPasswordTextField() {
        return passwordTextField;
    }

    public void setPasswordTextField(JFXTextField passwordTextField) {
        this.passwordTextField = passwordTextField;
    }

    public JFXTextArea getDescriptionTextArea() {
        return descriptionTextArea;
    }

    public void setDescriptionTextArea(JFXTextArea descriptionTextArea) {
        this.descriptionTextArea = descriptionTextArea;
    }

    public String getLeftButtonTooltip() {
        return leftButtonTooltip.get();
    }

    public StringProperty leftButtonTooltipProperty() {
        return leftButtonTooltip;
    }

    public void setLeftButtonTooltip(String leftButtonTooltip) {
        this.leftButtonTooltip.set(leftButtonTooltip);
    }

    public String getRightButtonTooltip() {
        return rightButtonTooltip.get();
    }

    public StringProperty rightButtonTooltipProperty() {
        return rightButtonTooltip;
    }

    public void setRightButtonTooltip(String rightButtonTooltip) {
        this.rightButtonTooltip.set(rightButtonTooltip);
    }

    public JavaEntry getAnEntry() {
        return anEntry;
    }

    public Integer getEntryIndex() {
        return entryIndex;
    }

    public Boolean getEdit() {
        return edit;
    }

    public Boolean getDelete() {
        return delete;
    }

    public HBox getRighButtonsHBox() {
        return righButtonsHBox;
    }

    public void setRighButtonsHBox(HBox righButtonsHBox) {
        this.righButtonsHBox = righButtonsHBox;
    }

    public String getDeleteButtonId() {
        return deleteButtonId.get();
    }

    public StringProperty deleteButtonIdProperty() {
        return deleteButtonId;
    }

    public void setDeleteButtonId(String deleteButtonId) {
        this.deleteButtonId.set(deleteButtonId);
    }

    public String getOkButtonId() {
        return okButtonId.get();
    }

    public StringProperty okButtonIdProperty() {
        return okButtonId;
    }

    public void setOkButtonId(String okButtonId) {
        this.okButtonId.set(okButtonId);
    }

    public String getCautionButtonId() {
        return cautionButtonId.get();
    }

    public StringProperty cautionButtonIdProperty() {
        return cautionButtonId;
    }

    public void setCautionButtonId(String cautionButtonId) {
        this.cautionButtonId.set(cautionButtonId);
    }

    public HBox getLeftButtonsHBox() {
        return leftButtonsHBox;
    }

    public void setLeftButtonsHBox(HBox leftButtonsHBox) {
        this.leftButtonsHBox = leftButtonsHBox;
    }

    public String getEditButtonId() {
        return editButtonId.get();
    }

    public StringProperty editButtonIdProperty() {
        return editButtonId;
    }

    public void setEditButtonId(String editButtonId) {
        this.editButtonId.set(editButtonId);
    }

    public String getCancelButtonId() {
        return cancelButtonId.get();
    }

    public StringProperty cancelButtonIdProperty() {
        return cancelButtonId;
    }

    public void setCancelButtonId(String cancelButtonId) {
        this.cancelButtonId.set(cancelButtonId);
    }

    public HBox getShowHidePasswordHBox() {
        return showHidePasswordHBox;
    }

    public void setShowHidePasswordHBox(HBox showHidePasswordHBox) {
        this.showHidePasswordHBox = showHidePasswordHBox;
    }

    public String getShowPasswordButtonId() {
        return showPasswordButtonId.get();
    }

    public StringProperty showPasswordButtonIdProperty() {
        return showPasswordButtonId;
    }

    public void setShowPasswordButtonId(String showPasswordButtonId) {
        this.showPasswordButtonId.set(showPasswordButtonId);
    }

    public String getHidePasswordButtonId() {
        return hidePasswordButtonId.get();
    }

    public StringProperty hidePasswordButtonIdProperty() {
        return hidePasswordButtonId;
    }

    public void setHidePasswordButtonId(String hidePasswordButtonId) {
        this.hidePasswordButtonId.set(hidePasswordButtonId);
    }
}
