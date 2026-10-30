package com.tiagohs.hqr.ui.sources

/** RF-111: the address the web app runs at (for the server's CORS setting); null outside the browser. */
expect fun browserOrigin(): String?
