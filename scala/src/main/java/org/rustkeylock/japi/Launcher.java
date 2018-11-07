// Copyright 2017 astonbitecode
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
package org.rustkeylock.japi;

import javafx.event.EventHandler;
import javafx.stage.WindowEvent;
import org.astonbitecode.j4rs.api.invocation.NativeCallbackToRustChannelSupport;
import org.rustkeylock.Ui;
import org.rustkeylock.components.RklStage;
import org.rustkeylock.japi.stubs.GuiResponse;
import org.rustkeylock.utils.Defs;
import scalafx.stage.Stage;

public class Launcher extends NativeCallbackToRustChannelSupport implements EventHandler<WindowEvent> {
    private boolean initialized = false;

    public static Launcher start() {
        new Thread(() -> Ui.main(new String[]{})).start();
        return new Launcher();
    }

    public static RklStage getStage() {
        Stage s = Ui.stage();
        if (s == null) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return getStage();
        }
        return new RklStage(s);
    }

    // This is called from the native in order to activate asynchronous callback for the exit event
    public void initHandler() {
        if (!initialized) {
            // Call the getStage in order to implicitly wait for the initialization and set the OnCloseListener
            Stage stage = getStage().fxStage();
            stage.onCloseRequest_$eq(this);
            initialized = true;
        }
    }

    @Override
    public void handle(WindowEvent ev) {
        doCallback(GuiResponse.GoToMenu(Defs.MENU_EXIT()));
        ev.consume();
    }
}
