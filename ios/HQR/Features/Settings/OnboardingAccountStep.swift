import Shared
import SwiftUI

/// SCR-ONB-001, optional account step (RF-119): right after the first opening, only where the build knows the Worker.
struct OnboardingAccountStep: View {
    let onDone: () -> Void
    @StateObject private var model = ScreenModel(Core.graph.accountViewModel()) {
        Screen<AccountViewModel.State>(viewModel: $0, flow: $0.state)
    }
    @State private var google = GoogleSignIn()
    @State private var signingIn = false

    var body: some View {
        VStack(spacing: 16) {
            Text(L("account_pitch_title"))
                .font(ComicFont.bangers(40))
                .multilineTextAlignment(.center)
                .foregroundStyle(Palette.onSurface)
                .padding(.top, 48)
                .accessibilityAddTraits(.isHeader)
            Text(L("onboarding_account_hint")).font(.subheadline).multilineTextAlignment(.center)
            VStack(spacing: 14) {
                HStack(spacing: 10) {
                    Image(systemName: "iphone").font(.system(size: 36))
                    Image(systemName: "arrow.triangle.2.circlepath").font(.system(size: 28)).foregroundStyle(Palette.primary)
                    Image(systemName: "books.vertical").font(.system(size: 36))
                }
                .accessibilityHidden(true)
                Text(L("onboarding_account_body")).font(.subheadline).multilineTextAlignment(.center)
            }
            .padding(20)
            .background(Palette.surface, in: RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Palette.onSurface, lineWidth: 2))
            .rotationEffect(.degrees(-1.5))
            .padding(.top, 12)
            Spacer()
            GoogleSignInButton(title: L("account_sign_in"), busy: signingIn || model.state.signingIn) {
                signingIn = true
                Task {
                    let token = await google.idToken()
                    signingIn = false
                    if let token { model.viewModel.signInWith(idToken: token) }
                }
            }
            Button(L("onboarding_not_now"), action: onDone)
                .frame(maxWidth: .infinity, minHeight: 44)
        }
        .padding(.horizontal, 24)
        .padding(.bottom, 24)
        .frame(maxWidth: 420)
        .onChange(of: model.state.account is AccountStateSignedIn) { _, signedIn in
            if signedIn { onDone() }
        }
    }
}

#Preview("OnboardingAccountStep · Light") { OnboardingAccountStep {}.previewScreen().preferredColorScheme(.light) }
#Preview("OnboardingAccountStep · Dark") { OnboardingAccountStep {}.previewScreen().preferredColorScheme(.dark) }
