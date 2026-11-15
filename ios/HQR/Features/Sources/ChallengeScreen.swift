import Shared
import SwiftUI
import WebKit

/// SCR-STA-001 (verification): the source's page in a WKWebView; when a new `cf_clearance` appears, its cookies and the
/// browser's User-Agent go to the core's HTTP client and the screen closes (RF-106, ADR-018).
struct ChallengeScreen: View {
    let url: String
    @EnvironmentObject private var navigator: Navigator

    var body: some View {
        VStack(spacing: 0) {
            Text(L("challenge_hint"))
                .font(.footnote)
                .foregroundStyle(Palette.onSurfaceVariant)
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Palette.surfaceHigh)
            if let address = URL(string: url) {
                ChallengeWebView(url: address) { navigator.back() }
            } else {
                EmptyState(title: L("challenge_title"), message: L("challenge_unavailable"), systemImage: "exclamationmark.shield")
            }
        }
        .navigationTitle(L("challenge_title"))
        .navigationBarTitleDisplayMode(.inline)
    }
}

/// The page, watched until the clearance changes.
struct ChallengeWebView: UIViewRepresentable {
    let url: URL
    let passed: () -> Void

    func makeCoordinator() -> Coordinator { Coordinator(url: url, passed: passed) }

    func makeUIView(context: Context) -> WKWebView {
        let view = WKWebView(frame: .zero, configuration: WKWebViewConfiguration())
        view.navigationDelegate = context.coordinator
        context.coordinator.start(view)
        return view
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {}

    @MainActor
    final class Coordinator: NSObject, WKNavigationDelegate {
        private let url: URL
        private let passed: () -> Void
        private var before: String?
        private var done = false
        private weak var view: WKWebView?

        init(url: URL, passed: @escaping () -> Void) {
            self.url = url
            self.passed = passed
        }

        func start(_ view: WKWebView) {
            self.view = view
            // The clearance kept from an earlier visit does not count: only a new value means the check passed.
            view.configuration.websiteDataStore.httpCookieStore.getAllCookies { cookies in
                MainActor.assumeIsolated {
                    self.before = Self.clearanceCookie(in: cookies, host: self.url.host ?? "")?.value
                    view.load(URLRequest(url: self.url))
                    self.poll()
                }
            }
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            check()
        }

        /// Looks once now and then every second until the view goes away (some checks pass without a page load).
        private func poll() {
            DispatchQueue.main.asyncAfter(deadline: .now() + 1) { [weak self] in
                guard let self, !self.done, self.view != nil else { return }
                self.check()
                self.poll()
            }
        }

        private func check() {
            guard !done, let view, let host = url.host else { return }
            view.configuration.websiteDataStore.httpCookieStore.getAllCookies { cookies in
                MainActor.assumeIsolated {
                    guard !self.done, let cookie = Self.clearanceCookie(in: cookies, host: host), cookie.value != self.before else { return }
                    self.done = true
                    // Saved under the cookie's domain, so api. or www. of the same site use it too.
                    let domain = cookie.domain.trimmingCharacters(in: CharacterSet(charactersIn: "."))
                    let header = cookies
                        .filter { host.hasSuffix($0.domain.trimmingCharacters(in: CharacterSet(charactersIn: "."))) }
                        .map { "\($0.name)=\($0.value)" }
                        .joined(separator: "; ")
                    view.evaluateJavaScript("navigator.userAgent") { agent, _ in
                        MainActor.assumeIsolated {
                            Core.graph.challengeCookies().save(host: domain, header: header, browserUserAgent: agent as? String ?? "")
                            self.passed()
                        }
                    }
                }
            }
        }

        private static func clearanceCookie(in cookies: [HTTPCookie], host: String) -> HTTPCookie? {
            cookies.first { $0.name == "cf_clearance" && host.hasSuffix($0.domain.trimmingCharacters(in: CharacterSet(charactersIn: "."))) }
        }
    }
}

#Preview("ChallengeScreen · Light") { ChallengeScreen(url: "https://example.com").previewScreen().preferredColorScheme(.light) }
#Preview("ChallengeScreen · Dark") { ChallengeScreen(url: "https://example.com").previewScreen().preferredColorScheme(.dark) }
#Preview("ChallengeWebView · Light") { ChallengeWebView(url: URL(string: "https://example.com")!, passed: {}).preferredColorScheme(.light) }
#Preview("ChallengeWebView · Dark") { ChallengeWebView(url: URL(string: "https://example.com")!, passed: {}).preferredColorScheme(.dark) }
