#[macro_use]
extern crate log;
#[macro_use]
extern crate lazy_static;
extern crate rust_keylock;
extern crate libc;

use libc::c_char;
use std::ffi::{CStr, CString};
use std::mem;
use std::str;
use std::sync::Mutex;
use std::sync::mpsc::{self, Sender, Receiver};
use rust_keylock::{UserSelection, Menu, Entry, UserOption, RklConfiguration};
use rust_keylock::nextcloud::NextcloudConfiguration;

mod ui_editor;
mod logger;

type LogCallback = extern "C" fn(*const c_char, *const c_char, *const c_char, i32, *const c_char);
type StringCallback = extern "C" fn(*const c_char);
type StringListCallback = extern "C" fn(Box<StringList>);
type ShowEntryCallback = extern "C" fn(Box<ScalaEntry>, i32, bool, bool);
type ShowEntriesSetCallback = extern "C" fn(Box<ScalaEntriesSet>, *const c_char);
type ShowMessageCallback = extern "C" fn(Box<ScalaUserOptionsSet>, *const c_char, *const c_char);

lazy_static! {
    static ref TX: Mutex<Option<Sender<UserSelection>>> = Mutex::new(None);
}

#[repr(C)]
pub struct ScalaEntry {
    name: *const c_char,
    user: *const c_char,
    pass: *const c_char,
    desc: *const c_char,
}

impl ScalaEntry {
    fn new(entry: &Entry) -> ScalaEntry {
        ScalaEntry {
            name: to_java_string(entry.name.clone()),
            user: to_java_string(entry.user.clone()),
            pass: to_java_string(entry.pass.clone()),
            desc: to_java_string(entry.desc.clone()),
        }
    }

    fn empty() -> ScalaEntry {
        ScalaEntry {
            name: to_java_string("".to_string()),
            user: to_java_string("".to_string()),
            pass: to_java_string("".to_string()),
            desc: to_java_string("".to_string()),
        }
    }

    fn with_nulls() -> ScalaEntry {
        ScalaEntry {
            name: to_java_string("null".to_string()),
            user: to_java_string("null".to_string()),
            pass: to_java_string("null".to_string()),
            desc: to_java_string("null".to_string()),
        }
    }
}

#[repr(C)]
pub struct ScalaUserOption {
    label: *const c_char,
    value: *const c_char,
    short_label: *const c_char,
}

impl ScalaUserOption {
    fn new(user_option: &UserOption) -> ScalaUserOption {
        ScalaUserOption {
            label: to_java_string(user_option.label.clone()),
            value: to_java_string(user_option.value.to_string()),
            short_label: to_java_string(user_option.short_label.clone()),
        }
    }
}

impl ScalaUserOption {
    fn with_nulls() -> ScalaUserOption {
        ScalaUserOption {
            label: to_java_string("null".to_string()),
            value: to_java_string("null".to_string()),
            short_label: to_java_string("null".to_string()),
        }
    }
}

#[repr(C)]
pub struct ScalaEntriesSet {
    entries: Box<[ScalaEntry]>,
    number_of_entries: i32,
}

impl<'a> From<&'a [Entry]> for ScalaEntriesSet {
    fn from(entries: &[Entry]) -> ScalaEntriesSet {
        // Create JavaEntries from Entries
        let scala_entries: Vec<ScalaEntry> = entries.iter()
            .clone()
            .map(|entry| ScalaEntry::new(entry))
            .collect();
        // Get the length of the entries
        let num_entries = scala_entries.len();

        let scala_entries_set = ScalaEntriesSet {
            entries: scala_entries.into_boxed_slice(),
            number_of_entries: num_entries as i32,
        };
        scala_entries_set
    }
}

impl ScalaEntriesSet {
    fn with_nulls() -> ScalaEntriesSet {
        let empty_entry = ScalaEntry::with_nulls();
        let dummy = vec![empty_entry];
        let scala_entries_set = ScalaEntriesSet {
            entries: dummy.into_boxed_slice(),
            number_of_entries: 1,
        };
        scala_entries_set
    }
}

#[repr(C)]
pub struct ScalaUserOptionsSet {
    options: Box<[ScalaUserOption]>,
    number_of_options: i32,
}

impl<'a> From<&'a [UserOption]> for ScalaUserOptionsSet {
    fn from(user_options: &[UserOption]) -> ScalaUserOptionsSet {
        // Create JavaEntries from Entries
        let scala_entries: Vec<ScalaUserOption> = user_options.iter()
            .clone()
            .map(|user_option| ScalaUserOption::new(user_option))
            .collect();
        // Get the length of the entries
        let num_entries = scala_entries.len();

        let scala_options_set = ScalaUserOptionsSet {
            options: scala_entries.into_boxed_slice(),
            number_of_options: num_entries as i32,
        };
        scala_options_set
    }
}

impl ScalaUserOptionsSet {
    fn with_nulls() -> ScalaUserOptionsSet {
        let empty_entry = ScalaUserOption::with_nulls();
        let dummy = vec![empty_entry];
        let scala_entries_set = ScalaUserOptionsSet {
            options: dummy.into_boxed_slice(),
            number_of_options: 1,
        };
        scala_entries_set
    }
}

#[repr(C)]
pub struct StringList {
    strings: Box<[*const c_char]>,
    number_of_strings: i32,
}

impl From<Vec<String>> for StringList {
    fn from(strings: Vec<String>) -> StringList {
        // Get the length of the entries
        let num_entries = strings.len();
        // Create JavaEntries from Entries
        let java_strings: Vec<_> = strings.into_iter()
            .map(|string| to_java_string(string.clone()))
            .collect();

        let string_list = StringList {
            strings: java_strings.into_boxed_slice(),
            number_of_strings: num_entries as i32,
        };
        string_list
    }
}

#[no_mangle]
pub extern "C" fn execute(show_menu_cb: StringCallback,
                          show_entry_cb: ShowEntryCallback,
                          show_entries_set_cb: ShowEntriesSetCallback,
                          show_message_cb: ShowMessageCallback,
                          edit_configuration_cb: StringListCallback,
                          log_cb: LogCallback) {
    let (tx, rx): (Sender<UserSelection>, Receiver<UserSelection>) = mpsc::channel();
    // Release the lock before calling the execute.
    // Execute will not return for the whole lifetime of the app, so the lock would be for ever acquired if was not explicitly released using the brackets.
    {
        let mut tx_opt = TX.lock().unwrap();
        *tx_opt = Some(tx);
    }
    let editor = ui_editor::new(show_menu_cb, show_entry_cb, show_entries_set_cb, show_message_cb, edit_configuration_cb, log_cb, rx);
    debug!("TX Mutex initialized. Executing native rust_keylock!");
    rust_keylock::execute(&editor)
}

#[no_mangle]
pub extern "C" fn set_password(password: *const c_char, number: i32) {
    debug!("set_password called");

    match TX.try_lock() {
        Ok(tx_opt) => {
            debug!("Lock acquired");
            let tx = tx_opt.as_ref().unwrap();
            let user_selection = UserSelection::ProvidedPassword(to_rust_string(password), number as usize);
            tx.send(user_selection).unwrap();
            debug!("set_password sent to the TX");
        }
        Err(error) => {
            error!("Could not acquire lock for tx: {:?}", error);
        }
    };
}

#[no_mangle]
pub extern "C" fn go_to_menu(menu_name: *const c_char) {
    debug!("go_to_menu called");
    let tx = {
        TX.lock().unwrap().as_ref().unwrap().clone()
    };
    let rust_string_menu_name = to_rust_string(menu_name);
    debug!("go_to_menu '{}'", rust_string_menu_name);
    let menu = Menu::from(rust_string_menu_name, None, None);
    let user_selection = UserSelection::GoTo(menu);
    tx.send(user_selection).unwrap();
    debug!("go_to_menu sent UserSelection to the TX");
}

#[no_mangle]
pub extern "C" fn go_to_menu_plus_arg(menu_name: *const c_char, arg_num: *const c_char, arg_str: *const c_char) {
    debug!("go_to_menu_plus_arg called");
    let tx = {
        TX.lock().unwrap().as_ref().unwrap().clone()
    };
    let rust_string_menu_name = to_rust_string(menu_name);
    debug!("go_to_menu_plus_arg '{}'", rust_string_menu_name);
    let rust_string_arg_num = to_rust_string(arg_num);
    let rust_string_arg_str = to_rust_string(arg_str);
    debug!("Arguments: num = '{}' and str = '{}'", rust_string_arg_num, rust_string_arg_str);

    let num_opt = if rust_string_arg_num == "null" {
        None
    } else {
        let num = rust_string_arg_num.parse::<usize>().unwrap();
        Some(num)
    };

    let str_opt = if rust_string_arg_str == "null" {
        None
    } else {
        Some(rust_string_arg_str)
    };

    let menu = Menu::from(rust_string_menu_name, num_opt, str_opt);
    let user_selection = UserSelection::GoTo(menu);
    tx.send(user_selection).unwrap();
    debug!("go_to_menu_plus_arg sent UserSelection to the TX");
}

#[no_mangle]
pub extern "C" fn add_entry(java_entry: &ScalaEntry) {
    debug!("add_entry");
    let tx = {
        TX.lock().unwrap().as_ref().unwrap().clone()
    };
    let entry = Entry::new(to_rust_string(java_entry.name),
                           to_rust_string(java_entry.user),
                           to_rust_string(java_entry.pass),
                           to_rust_string(java_entry.desc));

    let user_selection = UserSelection::NewEntry(entry);
    tx.send(user_selection).unwrap();
    debug!("add_entry sent UserSelection to the TX");
}

#[no_mangle]
pub extern "C" fn replace_entry(java_entry: &ScalaEntry, index: i32) {
    debug!("replace_entry");
    let tx = {
        TX.lock().unwrap().as_ref().unwrap().clone()
    };
    let entry = Entry::new(to_rust_string(java_entry.name),
                           to_rust_string(java_entry.user),
                           to_rust_string(java_entry.pass),
                           to_rust_string(java_entry.desc));

    let user_selection = UserSelection::ReplaceEntry(index as usize, entry);
    tx.send(user_selection).unwrap();
    debug!("replace_entry sent UserSelection to the TX");
}

#[no_mangle]
pub extern "C" fn delete_entry(index: i32) {
    debug!("delete_entry");
    let tx = {
        TX.lock().unwrap().as_ref().unwrap().clone()
    };

    let user_selection = UserSelection::DeleteEntry(index as usize);
    tx.send(user_selection).unwrap();
    debug!("delete_entry sent UserSelection to the TX");
}

#[no_mangle]
pub extern "C" fn export_import(path: *const c_char, export: u32, password: *const c_char, number: u32) {
    debug!("export_import");
    let tx = {
        TX.lock().unwrap().as_ref().unwrap().clone()
    };

    let user_selection = if export > 0 {
        debug!("Followed exporting path");
        UserSelection::ExportTo(to_rust_string(path))
    } else {
        debug!("Followed importing path");
        UserSelection::ImportFrom(to_rust_string(path), to_rust_string(password), number as usize)
    };
    tx.send(user_selection).unwrap();
    debug!("export_import sent UserSelection to the TX");
}

#[no_mangle]
pub extern "C" fn user_option_selected(label: *const c_char, value: *const c_char, short_label: *const c_char) {
    debug!("user_option_selected");
    let tx = {
        TX.lock().unwrap().as_ref().unwrap().clone()
    };

    tx.send(UserSelection::UserOption(UserOption::from((to_rust_string(label), to_rust_string(value), to_rust_string(short_label)))))
        .unwrap();
    debug!("user_option_selected sent UserSelection to the TX");
}

#[no_mangle]
pub extern "C" fn set_configuration(string_list: &StringList) {
    debug!("set_configuration with {} elements", string_list.strings.len());
    let tx = {
        TX.lock().unwrap().as_ref().unwrap().clone()
    };

    let ncc = if string_list.strings.len() == 4 {
        NextcloudConfiguration::new(to_rust_string(string_list.strings[0]),
                                    to_rust_string(string_list.strings[1]),
                                    to_rust_string(string_list.strings[2]),
                                    to_rust_string(string_list.strings[3]))
    } else {
        NextcloudConfiguration::new("Wrong Java Data".to_string().to_string(), "Wrong Java Data".to_string(), "Wrong Java Data".to_string(), "Wrong Java Data".to_string())
    };

    let conf = UserSelection::UpdateConfiguration(RklConfiguration::from(ncc.unwrap()));
    tx.send(conf).unwrap();
    debug!("set_configuration sent UserSelection to the TX");
}

#[no_mangle]
pub extern "C" fn drop_java_entry(_: Box<ScalaEntry>) {
    // Do nothing here. Because we own the JavaEntry here (we're using a Box) and we're not
    // returning it, Rust will assume we don't want it anymore and clean it up.
}

#[no_mangle]
pub extern "C" fn drop_java_entries_set(_: Box<ScalaEntriesSet>) {
    // Do nothing here. Because we own the JavaEntriesSet here (we're using a Box) and we're not
    // returning it, Rust will assume we don't want it anymore and clean it up.
}

fn to_rust_string(pointer: *const c_char) -> String {
    let slice = unsafe { CStr::from_ptr(pointer).to_bytes() };
    str::from_utf8(slice).unwrap().to_string()
}

fn to_java_string(string: String) -> *const c_char {
    let cs = CString::new(string.as_bytes()).unwrap();
    let ptr = cs.as_ptr();
    // Tell Rust not to clean up the string while we still have a pointer to it.
    // Otherwise, we'll get a segfault.
    mem::forget(cs);
    ptr
}