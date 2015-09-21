# LifeLine

## Goal
Seizure detection app using Microsoft Band sensors to detect when a user might experience one
Home for AppFoundry Android project template


## Development Android Signing Key

The Android system requires that all installed applications be digitally signed with a certificate whose private key is held by the application's developer. The Android system uses the certificate as a means of identifying the author of an application and establishing trust relationships between applications. The certificate is not used to control which applications the user can install. The certificate does not need to be signed by a certificate authority: it is perfectly allowable, and typical, for Android applications to use self-signed certificates.


### Signing process

The Android build process signs the application differently depending on which build mode you use to build the application. There are two build modes: debug mode and release mode. You use debug mode when you are developing and testing the application. You use release mode when you want to build a release version of the application that you can distribute directly to users or publish on an application marketplace such as Google Play.

When you build in debug mode the Android SDK build tools use the Keytool utility (included in the JDK) to create a debug key. Because the SDK build tools created the debug key, they know the debug key's alias and password. Each time you compile your application in debug mode, the build tools use the debug key along with the Jarsigner utility (also included in the JDK) to sign the application's .apk file. Because the alias and password are known to the SDK build tools, the tools don't need to prompt you for the debug key's alias and password each time you compile.

When you build in release mode you use your own private key to sign the application. If you don't have a private key, you can use the Keytool utility to create one for you. When you compile the application in release mode, the build tools use your private key along with the Jarsigner utility to sign your application's .apk file. Because the certificate and private key you use are your own, you must provide the password for the keystore and key alias.
