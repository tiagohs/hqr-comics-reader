import Shared
import SwiftUI

/// SCR-SET-005 Conta e sincronização on iOS: a grouped list, as the iPhone board of the canvas (RF-119 to RF-125).
struct AccountScreen: View {
    @StateObject private var model = ScreenModel(Core.graph.accountViewModel()) {
        Screen<AccountViewModel.State>(viewModel: $0, flow: $0.state)
    }
    @State private var google = GoogleSignIn()
    @State private var signingIn = false
    @State private var arrived: Int?

    var body: some View {
        let state = model.state
        List {
            if let signedIn = state.account as? AccountStateSignedIn {
                Section {
                    HStack(spacing: 12) {
                        Text(String(signedIn.email.prefix(1)).uppercased())
                            .font(ComicFont.bangers(24))
                            .foregroundStyle(Palette.onPrimary)
                            .frame(width: 44, height: 44)
                            .background(Palette.primary, in: Circle())
                            .accessibilityHidden(true)
                        VStack(alignment: .leading) {
                            Text(signedIn.email).font(.body.weight(.semibold))
                            Text(L("account_google")).font(.footnote).foregroundStyle(Palette.onSurfaceVariant)
                        }
                    }
                    .padding(.vertical, 4)
                    SyncStatusRow(status: state.status)
                    Button(state.status.error == nil ? L("sync_now") : L("sync_try_again")) { model.viewModel.syncNow() }
                        .disabled(state.status.running || state.status.error == .session)
                } footer: {
                    Text(L("account_pitch"))
                }
            } else {
                Section {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(L("account_pitch_title")).font(ComicFont.bangers(26))
                        Text(L("account_pitch")).font(.subheadline)
                        if let problem = state.problem {
                            Label(problemText(problem), systemImage: problem == .notAllowed ? "lock" : "exclamationmark.triangle")
                                .foregroundStyle(Palette.error)
                                .font(.subheadline.weight(.semibold))
                        }
                        GoogleSignInButton(
                            title: state.problem == .notAllowed ? L("account_use_other") : L("account_sign_in"),
                            busy: signingIn || state.signingIn
                        ) { signIn() }
                        .disabled(!GoogleSignIn.available)
                    }
                    .padding(.vertical, 8)
                } footer: {
                    Text(L("account_offline_note"))
                }
            }
            Section {
                Label(L("account_goes_library"), systemImage: "checkmark")
                Label(L("account_goes_progress"), systemImage: "checkmark")
                Label(L("account_goes_feed"), systemImage: "checkmark")
            } header: {
                Text(L("account_goes"))
            } footer: {
                Text(L("account_stays_downloads") + " · " + L("account_stays_device"))
            }
            if state.account is AccountStateSignedIn {
                Section {
                    Button(L("account_sign_out"), role: .destructive) { model.viewModel.signOut() }
                } footer: {
                    Text(L("account_sign_out_note"))
                }
            }
        }
        .navigationTitle(L("account_title"))
        .onChange(of: state.arrived?.intValue) { _, value in
            if let value { arrived = value }
            model.viewModel.arrivedShown()
        }
        .alert(arrived.map { P("sync_arrived", $0) } ?? "", isPresented: Binding(get: { arrived != nil }, set: { if !$0 { arrived = nil } })) {
            Button(L("action_ok")) { arrived = nil }
        }
    }

    private func signIn() {
        signingIn = true
        Task {
            let token = await google.idToken()
            signingIn = false
            if let token { model.viewModel.signInWith(idToken: token) }
        }
    }

    private func problemText(_ problem: SignInProblem) -> String {
        switch problem {
        case .notAllowed: L("account_not_allowed")
        case .offline: L("account_sign_in_offline")
        default: L("account_sign_in_failed")
        }
    }
}

/// "Sincronizado há 2 min", "Sincronizando…" or the failure with its reason (RF-125).
private struct SyncStatusRow: View {
    let status: SyncStatus

    var body: some View {
        if status.running {
            HStack {
                ProgressView()
                Text(L("sync_running"))
            }
        } else if let error = status.error {
            VStack(alignment: .leading, spacing: 2) {
                Label(L("sync_failed"), systemImage: "exclamationmark.triangle").foregroundStyle(Palette.error)
                Text(reason(error)).font(.footnote).foregroundStyle(Palette.onSurfaceVariant)
            }
        } else {
            Label(synced(status.lastSuccessAt?.int64Value), systemImage: "checkmark.circle")
                .foregroundStyle(Palette.onSurface)
        }
    }

    private func reason(_ error: SyncError) -> String {
        switch error {
        case .offline: L("sync_failed_offline")
        case .session: L("sync_failed_session")
        default: L("sync_failed_server")
        }
    }

    private func synced(_ at: Int64?) -> String {
        guard let at else { return L("sync_never") }
        let ago = Date().timeIntervalSince1970 * 1000 - Double(at)
        if ago < 60_000 { return L("sync_done_now") }
        if ago < 3_600_000 { return L("sync_done_minutes", Int(ago / 60_000)) }
        return L("sync_done_hours", Int(ago / 3_600_000))
    }
}

/// Google's sign-in button look: white, thin grey border, the colored "G" (Google's own guidelines).
struct GoogleSignInButton: View {
    let title: String
    let busy: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                if busy { ProgressView() } else { GoogleMark().frame(width: 20, height: 20) }
                Text(title).font(.body.weight(.semibold))
            }
            .frame(maxWidth: .infinity, minHeight: 44)
            .foregroundStyle(Color(red: 0.12, green: 0.12, blue: 0.12))
            .background(Color.white, in: Capsule())
            .overlay(Capsule().stroke(Color(red: 0.45, green: 0.47, blue: 0.46), lineWidth: 1))
        }
        .buttonStyle(.plain)
        .disabled(busy)
    }
}

/// The four-color "G", drawn as four arcs.
private struct GoogleMark: View {
    var body: some View {
        Canvas { context, size in
            let lineWidth = size.width * 0.2
            let rect = CGRect(origin: .zero, size: size).insetBy(dx: lineWidth / 2, dy: lineWidth / 2)
            let center = CGPoint(x: rect.midX, y: rect.midY)
            let radius = rect.width / 2
            let arcs: [(Color, Double, Double)] = [
                (Color(red: 0.92, green: 0.26, blue: 0.21), 200, 315),
                (Color(red: 0.98, green: 0.74, blue: 0.02), 145, 200),
                (Color(red: 0.20, green: 0.66, blue: 0.33), 45, 145),
                (Color(red: 0.26, green: 0.52, blue: 0.96), 0, 45),
            ]
            for (color, start, end) in arcs {
                var arc = Path()
                arc.addArc(center: center, radius: radius, startAngle: .degrees(start), endAngle: .degrees(end), clockwise: false)
                context.stroke(arc, with: .color(color), lineWidth: lineWidth)
            }
            var bar = Path()
            bar.move(to: CGPoint(x: center.x, y: center.y))
            bar.addLine(to: CGPoint(x: rect.maxX + lineWidth / 2, y: center.y))
            context.stroke(bar, with: .color(Color(red: 0.26, green: 0.52, blue: 0.96)), lineWidth: lineWidth)
        }
        .accessibilityHidden(true)
    }
}

/// RF-125 on iOS: "Não sincroniza há 2 dias" over the library, opening the account.
struct SyncStaleNotice: View {
    let days: Int

    var body: some View {
        NavigationLink(value: Route.account) {
            HStack(spacing: 10) {
                Image(systemName: "icloud.slash")
                Text(P("sync_stale_days", days)).font(.subheadline.weight(.semibold))
                Spacer()
                Text(L("account_see")).font(.subheadline.weight(.bold))
            }
            .foregroundStyle(Color(red: 0.086, green: 0.086, blue: 0.1))
            .padding(.horizontal, 12)
            .frame(minHeight: 44)
            .background(Color(red: 1, green: 0.945, blue: 0.659), in: RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
    }
}

#Preview("AccountScreen · Light") { AccountScreen().previewScreen().preferredColorScheme(.light) }
#Preview("AccountScreen · Dark") { AccountScreen().previewScreen().preferredColorScheme(.dark) }
#Preview("GoogleSignInButton · Light") { GoogleSignInButton(title: "Entrar com o Google", busy: false) {}.padding().preferredColorScheme(.light) }
#Preview("GoogleSignInButton · Dark") { GoogleSignInButton(title: "Entrar com o Google", busy: true) {}.padding().preferredColorScheme(.dark) }
#Preview("SyncStaleNotice · Light") { SyncStaleNotice(days: 2).padding().preferredColorScheme(.light) }
#Preview("SyncStaleNotice · Dark") { SyncStaleNotice(days: 2).padding().preferredColorScheme(.dark) }
