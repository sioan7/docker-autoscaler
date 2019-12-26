package pages

import kotlinx.html.*

val index: HTML.() -> Unit = {
    head {
        title { +"Monitor" }
        link(href = "static/favicon.ico", rel = "shortcut icon", type = "image/x-icon") {}
        script(src = "static/script.js") {}
    }
    body {
        input(type = InputType.file) { onChange = "openFile(this)" }
        button {
            onClick = "uploadFile()"
            +"UPLOAD FILE"
        }
        p { +"Current status: ${"none"}" }
        p { +"Result: ${1234}" }
        button {
            onClick = "downloadFile()"
            +"DOWNLOAD"
        }
    }
}
