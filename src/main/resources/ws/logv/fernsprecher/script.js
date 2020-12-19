let socket;

const statusId = "status-text"

function maxMsgs() {
    return 1000
}

function copyStacktrace(e) {
    let trace = e.parentElement.parentElement.getElementsByClassName("stacktrace-data")[0].getElementsByTagName("p")[0].innerHTML
    navigator.clipboard.writeText(trace).then(r => console.log("copied"));
}

function connect() {
    socket = new WebSocket("ws://" + window.location.host + "/fernsprecher/logs")
    let element = document.getElementById(statusId);

    if (element) {
        element.setAttribute("data-state", "connecting")
    }

    socket.onmessage = function (event) {
        let msg = JSON.parse(event.data)

        let classes = ""
        if (msg.level === "ERROR") {
            classes = "error"
        } else if (msg.level === "WARN") {
            classes = "warn"
        }
        let row = document.createElement("tr")
        row.className = classes

        let time = document.createElement("td")
        time.innerHTML = msg.time
        row.append(time)

        let level = document.createElement("td")
        level.innerHTML = msg.level
        row.append(level)

        let logger = document.createElement("td")
        logger.innerHTML = msg.logger
        row.append(logger)

        let message = document.createElement("td")
        message.innerHTML = msg.message

        row.append(message)

        let stacktrace = document.createElement("td")

        stacktrace.innerHTML = ""
        if (msg.stacktrace) {
            stacktrace.innerHTML = `
                <div class="stacktrace">
                <div class="copy-area">
                    <button onclick="copyStacktrace(this)" class="copy tooltip" about="" data-tooltip="Copy Stacktrace"></button>               
                </div>

                <div class="stacktrace-data"><p>${msg.stacktrace.join("\r\n")}</p></div>
                </div>
`
        }



        row.append(stacktrace)

        // don't scroll for new message if the user scrolled up
        let atBottom = (window.innerHeight + window.scrollY) >= document.body.offsetHeight

        let logs = document.getElementById("logs-content");
        logs.appendChild(row)

        if (atBottom) {
            console.log("scrolling")
            row.scrollIntoView(false)
        }

        while (logs.childNodes.length >= maxMsgs()) {
            logs.removeChild(logs.firstChild)
        }
    }
    socket.onopen = function (e) {
        console.debug("connected")
        let element = document.getElementById(statusId);
        element.setAttribute("data-state", "connected")
    };
    socket.onclose = function (err) {
        if (err.wasClean) {
            console.error(`[close] Connection closed cleanly, code=${err.code} reason=${err.reason}`)
        } else {
            console.error('[close] Connection died')
        }
        let element = document.getElementById(statusId);
        element.setAttribute("data-state", "closed")
        setTimeout(function () {
            connect()
        }, 1000)
    }
    socket.onerror = function (err) {
        console.error('Socket encountered error: ', err.message, 'Closing socket');
        socket.close();
    }
}

setTimeout(function () {
    connect()
}, 1000)