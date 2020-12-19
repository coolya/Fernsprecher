package ws.logv.fernsprecher

import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.Urls
import org.jetbrains.builtInWebServer.BuiltInServerOptions

class OpenLogsInBrowserAction: AnAction("Open Logs in Browser") {
    override fun actionPerformed(e: AnActionEvent) {
        val serverPort = BuiltInServerOptions.getInstance().effectiveBuiltInServerPort
        val authority = "localhost:$serverPort"
        val url = Urls.newHttpUrl(authority, "/fernsprecher/")
        BrowserLauncher.instance.browse(url.toExternalForm(), project = e.project)
    }
}