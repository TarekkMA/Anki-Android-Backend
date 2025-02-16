# AnkiDroid-Backend

Adapter allowing AnkiDroid interop with `anki/rslib` allowing AnkiDroid to leverage Anki Desktop's business logic layer, maintaining 100% compatibility with upstream without the need to port code to Java.

## Installation

```gradle
    implementation "io.github.david-allison-1:anki-android-backend:0.1.0"
    testImplementation "io.github.david-allison-1:anki-android-backend-testing:0.1.0"
```

## Folders

`/anki/` - git submodule containing the Anki Codebase, used both for building into `.so` files, and to obtain the current `.proto` files for use in Java codegen

`/tools/` Tools to generate efficient protobuf-based RPC calls using JNI

`/rsdroid/` - Java library to be consumed by `Anki-Android`.

`rsdroid-testing` - Builds a testing library which exposes a function to load `rsdroid` in a non-Android context for testing via Robolectric

`rsdroid-instrumented` - Android Instrumented Test 

This is defined as an application to allow instrumented tests to be run against a library - there may be a better method

`/rslib-bridge/` (Rust) Android-specific library to communicate with `anki/rslib`

## Implementation

* Points to a fixed commit of `anki` 
  * Currently as a fork in `david-allison-1/anki` 
  * Modified to use a submodule for translations so we have a reproducible build
  * Modifications to the library so we do not need to update to database schema 15 for version 1
* References `backend.proto` and `fluent.proto` which define RPC service calls to the anki backend
* Python script to auto-generate the Java interface/backend to the RPC mechanism. Invoked via gradle.
* Android Library which contains the rust based `.so` under (x86, x86-64, arm, arm64)
   * Implements `android.database.sqlite`, redirecting SQL to the rust library
   * Exposes RPC calls to Rust via a clean Java interface (`net.ankiweb.rsdroid.RustBackend`)
* Testing library to allow the above to be usable under Robolectric


## Additional Information

See `/docs` for more in-depth information.

## License

[GPL-3.0 License](https://github.com/ankidroid/Anki-Android/blob/master/COPYING)  
[AGPL-3.0 Licence](https://github.com/david-allison-1/anki/blob/master/LICENSE) (anki submodule)
