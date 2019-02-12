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

extern crate glob;

use glob::glob;
use std::env;
use std::process::Command;

fn main() {
    let jvm_dyn_lib_file_name = if cfg!(windows) {
        "jvm.dll"
    } else {
        "libjvm.*"
    };

    // Retrieve the LD_LIBRARY_PATH
    let ld_library_path = get_ld_library_path(jvm_dyn_lib_file_name);
    let mut exec_dir = env::current_exe().expect("Could not get the directory of the executable...");
    exec_dir.pop();
    let base_dir = exec_dir.to_str().unwrap_or_else(|| panic!("Could not create str from exec_dir {:?}", exec_dir));

    let mut command: Command;

    // Prepare the command depending on the host
    if cfg!(windows) {

        command = Command::new(format!("{}\\rust-keylock-ui.exe", base_dir));
        let path_var = env::var("PATH").unwrap_or("".to_string());
        command.env("PATH", format!("{};{}", ld_library_path, path_var));
    } else if cfg!(macos) {
        command = Command::new(format!("{}/rust-keylock-ui", base_dir));
        let ld = env::var("DYLD_LIBRARY_PATH").unwrap_or("".to_string());
        command.env("DYLD_LIBRARY_PATH", format!("{}:{}", ld_library_path, ld));
    } else {
        command = Command::new(format!("{}/rust-keylock-ui", base_dir));
        let ld = env::var("LD_LIBRARY_PATH").unwrap_or("".to_string());
        command.env("LD_LIBRARY_PATH", format!("{}:{}", ld_library_path, ld));
    };

    // Execute the command and wait
    let mut child = command.spawn().expect("Failed to execute rust-keylock-ui");
    let status = child.wait().expect("Failed to wait on ust-keylock-ui");

    match status.code() {
        Some(code) => println!("rust-keylock-ui exited with status code: {}", code),
        None => println!("rust-keylock-ui terminated by signal")
    }
}

fn get_ld_library_path(lib_file_name: &str) -> String {
    // Find the JAVA_HOME
    let java_home = env::var("JAVA_HOME").unwrap_or("".to_owned());
    if java_home.is_empty() {
        panic!("JAVA_HOME is not set. \
        Please make sure that Java is installed (version 1.8 at least) and the JAVA_HOME environment variable is set.");
    }

    let query = format!("{}/**/{}", java_home, lib_file_name);

    let paths_vec: Vec<String> = glob(&query).unwrap()
        .filter_map(Result::ok)
        .map(|path_buf| {
            let mut pb = path_buf.clone();
            pb.pop();
            pb.to_str().unwrap().to_string()
        })
        .collect();

    if paths_vec.is_empty() {
        let name = if cfg!(windows) {
            "jvm.lib"
        } else {
            "libjvm"
        };
        panic!("Could not find the {} in any subdirectory of {}", name, java_home);
    }

    paths_vec[0].clone()
}