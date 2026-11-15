import AuthenticationServices
import CryptoKit
import Foundation

/// RF-119 on iOS: Google's page in the system's web sheet, OAuth with PKCE for the iOS client, no Google SDK (ADR-028).
/// The ID token goes to the personal Worker, which keeps only its own session.
@MainActor
final class GoogleSignIn: NSObject, ASWebAuthenticationPresentationContextProviding {
    /// The Worker's address from the build settings (Secrets.xcconfig); empty when the build has no account.
    static var hubURL: String {
        let host = (Bundle.main.object(forInfoDictionaryKey: "HQRHubHost") as? String ?? "").trimmingCharacters(in: .whitespaces)
        return host.isEmpty ? "" : "https://" + host
    }

    static var clientID: String {
        (Bundle.main.object(forInfoDictionaryKey: "HQRGoogleClientID") as? String ?? "").trimmingCharacters(in: .whitespaces)
    }

    static var available: Bool { !hubURL.isEmpty && !clientID.isEmpty }

    private var session: ASWebAuthenticationSession?

    /// The ID token, or nil when the person closed the sheet or Google refused.
    func idToken() async -> String? {
        let clientID = Self.clientID
        guard !clientID.isEmpty else { return nil }
        // The redirect is the reversed client id, the scheme Google gives every iOS client.
        let scheme = clientID.components(separatedBy: ".").reversed().joined(separator: ".")
        let redirect = scheme + ":/oauth2redirect"
        let verifier = Self.randomString()
        let challenge = Data(SHA256.hash(data: Data(verifier.utf8))).base64URL
        var auth = URLComponents(string: "https://accounts.google.com/o/oauth2/v2/auth")!
        auth.queryItems = [
            URLQueryItem(name: "client_id", value: clientID),
            URLQueryItem(name: "redirect_uri", value: redirect),
            URLQueryItem(name: "response_type", value: "code"),
            URLQueryItem(name: "scope", value: "openid email"),
            URLQueryItem(name: "code_challenge", value: challenge),
            URLQueryItem(name: "code_challenge_method", value: "S256"),
            URLQueryItem(name: "prompt", value: "select_account"),
        ]
        guard let url = auth.url, let callback = await present(url, scheme: scheme),
              let code = URLComponents(url: callback, resolvingAgainstBaseURL: false)?.queryItems?.first(where: { $0.name == "code" })?.value
        else { return nil }
        return await exchange(code: code, clientID: clientID, redirect: redirect, verifier: verifier)
    }

    private func present(_ url: URL, scheme: String) async -> URL? {
        await withCheckedContinuation { continuation in
            let session = ASWebAuthenticationSession(url: url, callbackURLScheme: scheme) { callback, _ in
                continuation.resume(returning: callback)
            }
            session.presentationContextProvider = self
            session.prefersEphemeralWebBrowserSession = false
            self.session = session
            if !session.start() { continuation.resume(returning: nil) }
        }
    }

    private func exchange(code: String, clientID: String, redirect: String, verifier: String) async -> String? {
        var request = URLRequest(url: URL(string: "https://oauth2.googleapis.com/token")!)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        var form = URLComponents()
        form.queryItems = [
            URLQueryItem(name: "code", value: code),
            URLQueryItem(name: "client_id", value: clientID),
            URLQueryItem(name: "redirect_uri", value: redirect),
            URLQueryItem(name: "code_verifier", value: verifier),
            URLQueryItem(name: "grant_type", value: "authorization_code"),
        ]
        request.httpBody = form.percentEncodedQuery?.data(using: .utf8)
        guard let (data, response) = try? await URLSession.shared.data(for: request),
              (response as? HTTPURLResponse)?.statusCode == 200,
              let body = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
        else { return nil }
        return body["id_token"] as? String
    }

    nonisolated func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        MainActor.assumeIsolated {
            UIApplication.shared.connectedScenes
                .compactMap { ($0 as? UIWindowScene)?.keyWindow }
                .first ?? ASPresentationAnchor()
        }
    }

    private static func randomString() -> String {
        var bytes = [UInt8](repeating: 0, count: 32)
        _ = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)
        return Data(bytes).base64URL
    }
}

private extension Data {
    var base64URL: String {
        base64EncodedString().replacingOccurrences(of: "+", with: "-").replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }
}
