package nl.leidenuniv.pages

import kotlinx.html.*

val index: HTML.() -> Unit = {
    head {
        title { +"Monitor" }
        link(href = "static/favicon.ico", rel = "shortcut icon", type = "image/x-icon") {}
        link(href = "static/styles.css", rel = "stylesheet") {}
        script(src = "static/script.js") {}
    }
    body {
        input(type = InputType.file) {
            onChange = "openFile(this)"
        }
        button {
            onClick = "uploadFile()"
            +"UPLOAD FILE"
        }
        p {
            id = "refreshInformation"
            +"Do not reload the page! The information is being refreshed every second..."
        }
        p {
            id = "currentStatus"
            +"Current status: ${"none"}"
        }
        p {
            id = "numberResult"
            +"Number of numbers: unknown"
        }
        a {
            id = "downloadLink"
            +"DOWNLOAD"
        }
    }
}
