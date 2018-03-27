#[macro_use]
extern crate log;
#[macro_use]
extern crate lazy_static;
extern crate rust_keylock;
extern crate serde;
extern crate serde_json;
#[macro_use]
extern crate serde_derive;
extern crate j4rs;

use j4rs::ClasspathEntry;
use std::sync::Mutex;
use std::sync::mpsc::{self, Sender, Receiver};
use rust_keylock::UserSelection;

mod ui_editor;
mod logger;
mod callbacks;
mod errors;

lazy_static! {
    static ref TX: Mutex<Option<Sender<UserSelection>>> = Mutex::new(None);
}

fn main() {
    let mut default_classpath_entry = std::env::current_exe().unwrap();
    default_classpath_entry.pop();
    default_classpath_entry.push("scalaassets");
    default_classpath_entry.push("desktop-ui-0.4.0.jar");

    let jvm_res = j4rs::new_jvm(vec![
        ClasspathEntry::new(default_classpath_entry
            .to_str()
            .unwrap_or("./scalaassets/desktop-ui-0.4.0.jar"))],
                                Vec::new());

    let jvm = jvm_res.unwrap();

    let (tx, rx): (Sender<UserSelection>, Receiver<UserSelection>) = mpsc::channel();
    // Release the lock before calling the execute.
    // Execute will not return for the whole lifetime of the app, so the lock would be for ever acquired if was not explicitly released using the brackets.
    {
        let mut tx_opt = TX.lock().unwrap();
        *tx_opt = Some(tx);
    }

    logger::init();
    let editor = ui_editor::new(jvm, rx);
    println!("TX Mutex initialized. Executing native rust_keylock!");
    rust_keylock::execute(&editor)
}
