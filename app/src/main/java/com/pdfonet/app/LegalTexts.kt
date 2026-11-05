package com.pdfonet.app

object LegalTexts {
    const val CONTACT = "Telegram: @blvcknetwork"

    val privacy = """
Privacy Policy
Last updated: 5 November 2026

PDio works fully offline. The app does not request internet permission and does not send any data anywhere.

What stays on your device
- The text you type, paste or import is used only to create your PDF.
- Photos you scan or pick are used only to create your PDF.
- Created PDFs are saved in Downloads/PDio, or in the folder you choose.
- A small history list (file name, size, date, favorite mark), your draft and your settings are stored privately inside the app.

What we do not do
- No accounts, no ads, no analytics, no trackers.
- No data is collected, shared or sold.

Your control
You can remove items from History or delete the PDF files at any time. Uninstalling the app removes its private data. Your PDFs in the save folder stay until you delete them.

Contact
$CONTACT
""".trimIndent()

    val terms = """
Terms of Use
Last updated: 5 November 2026

By using PDio you agree to these terms.

1. Use of the app
PDio is provided free of charge to create and edit PDF files for personal or professional use.

2. Your content
You are responsible for the content you convert and for having the right to use it. We do not see or store your content.

3. No warranty
The app is provided "as is" without warranties of any kind. We do not guarantee that it will be error-free or that PDFs will meet every requirement.

4. Limitation of liability
To the maximum extent permitted by law, the developer is not liable for any loss or damage arising from the use of the app, including lost files.

5. Changes
These terms may be updated with new versions of the app.

Contact
$CONTACT
""".trimIndent()

    val licenses = """
Open-source licenses

PDio is built with these open-source components, all under the Apache License 2.0:
- Kotlin and kotlinx.coroutines
- AndroidX (Core, Activity, Lifecycle)
- Jetpack Compose and Material 3

Full license text: https://www.apache.org/licenses/LICENSE-2.0
""".trimIndent()

    val about = """
PDio
Version 1.3

Fast, private, offline PDF tools.

Contact
$CONTACT
""".trimIndent()
}
